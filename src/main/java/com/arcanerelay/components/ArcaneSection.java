package com.arcanerelay.components;

import java.time.Instant;
import java.util.BitSet;
import java.util.Comparator;
import java.util.concurrent.locks.StampedLock;

import javax.annotation.Nonnull;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.BitSetUtil;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.function.predicate.ObjectPositionBlockFunction;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.io.ByteBufUtil;
import com.hypixel.hytale.sneakythrow.SneakyThrow;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

/**
 * Tracks ticking blocks and their sources. Source map: blockIdx -> list of source positions (as int[3] world coords).
 * Uses ConcurrentHashMap for thread-safe updates from parallel tick threads.
 */
public class ArcaneSection implements Component<ChunkStore> {
    public static final int VERSION = 1;
    public static final BuilderCodec<ArcaneSection> CODEC = BuilderCodec.builder(ArcaneSection.class, ArcaneSection::new)
        .versioned()
        .codecVersion(VERSION)
        .append(new KeyedCodec<>("Data", Codec.BYTE_ARRAY), ArcaneSection::deserialize, ArcaneSection::serialize)
        .add()
        .build();

   private final StampedLock arcaneSectionLock;
   private final ObjectHeapPriorityQueue<ArcaneSection.TickRequest> tickRequests;
   private final static Comparator<ArcaneSection.TickRequest> TICK_REQUEST_COMPARATOR = Comparator.comparing(t -> t.requestedGameTime);
   BitSet tickingBlocks;
   BitSet lastTickingBlocks;

//    /** blockIdx -> list of source positions (int[]{x,y,z}). Concurrent for parallel addSource from multiple sections. */
//    private final Map<Integer, List<int[]>> pendingSources;
//    /** Snapshot of pendingSources at preTick; read-only during forEachTicking. */
//    private Map<Integer, List<int[]>> lastSources;

    public ArcaneSection() {
        this.arcaneSectionLock = new StampedLock();
        this.tickingBlocks = new BitSet();
        this.lastTickingBlocks = new BitSet();
        this.tickRequests = new ObjectHeapPriorityQueue<>(TICK_REQUEST_COMPARATOR);
        // this.pendingSources = new ConcurrentHashMap<>();
    }
    

    public static ComponentType<ChunkStore, ArcaneSection> getComponentType() {
        return ArcaneRelayPlugin.get().getArcaneSectionComponentType();
    }

    public void scheduleTick(int index, @Nonnull Instant gameTime) {
        if (gameTime != null) {
            this.tickRequests.enqueue(new TickRequest(index, gameTime));
        }
    }

    public void preTick(@Nonnull Instant gameTime) {
        ArcaneSection.TickRequest request;
        while (!this.tickRequests.isEmpty() && (request = this.tickRequests.first()).requestedGameTime.isBefore(gameTime)) {
            this.tickRequests.dequeue();
            this.setTicking(request.index, true);
        }

        long writeStamp = this.arcaneSectionLock.writeLock();
        try {
            if (this.tickingBlocks.isEmpty() && this.lastTickingBlocks.isEmpty()) {
                return;
            }
            BitSetUtil.copyValues(this.tickingBlocks, this.lastTickingBlocks);
            this.tickingBlocks.clear();
            // this.lastSources = Map.copyOf(this.pendingSources);
            // this.pendingSources.clear();
        } finally {
            this.arcaneSectionLock.unlockWrite(writeStamp);
        }
    }

    public <T, V> int forEachTicking(T t, V v, BlockSection section, int sectionIndex, @Nonnull ObjectPositionBlockFunction<T, V, BlockTickStrategy> acceptor) {
        int sectionStartYBlock = sectionIndex << 5;
        int ticked = 0;

        for(int index = this.lastTickingBlocks.nextSetBit(0); index >= 0; index = this.lastTickingBlocks.nextSetBit(index + 1)) {
            int x = ChunkUtil.xFromIndex(index);
            int y = ChunkUtil.yFromIndex(index);
            int z = ChunkUtil.zFromIndex(index);

            BlockTickStrategy strategy = acceptor.accept(t, v, x, y | sectionStartYBlock, z, section.get(index));
            switch (strategy) {
                case PROCESSED:
                    ticked++;
                    continue;
                case WAIT_FOR_ADJACENT_CHUNK_LOAD:
                case CONTINUE:
                        this.setTicking(index, true);
                        continue;
                default:
                    continue;
            }
        }
        return ticked;
    }

    public void setTicking(int x, int y, int z, boolean ticking) {
         this.setTicking(ChunkUtil.indexBlock(x, y, z), ticking);
    }

    /** Sets a block to ticking and records the source position for when it is processed. */
    public void setTicking(int x, int y, int z, boolean ticking, int sourceX, int sourceY, int sourceZ) {
        int index = ChunkUtil.indexBlock(x, y, z);
      
        if (setTicking(index, ticking)) {
            ArcaneRelayPlugin.LOGGER.atInfo().log("Adding source for block " + index + " to " + sourceX + ", " + sourceY + ", " + sourceZ);
            // addSource(index, sourceX, sourceY, sourceZ);
        }
    }

    public boolean setTicking(int blockIndex, boolean ticking) {
        long readStamp = this.arcaneSectionLock.readLock();

      try {
         if (this.tickingBlocks.get(blockIndex) == ticking) {
            return false;
         }
      } finally {
         this.arcaneSectionLock.unlockRead(readStamp);
      }
      
      boolean result = false;

      long writeStamp = this.arcaneSectionLock.writeLock();
      try {
        this.tickingBlocks.set(blockIndex, ticking);
        result = true;
      } finally {
        this.arcaneSectionLock.unlockWrite(writeStamp);
      }
      return result;
    }

    /** Serializes ticking state to the buffer (under read lock). */
    private void serialize(@Nonnull ByteBuf buf) {
        long lock = this.arcaneSectionLock.readLock();
        try {
            BitSet combined = (BitSet) this.tickingBlocks.clone();
            combined.or(this.lastTickingBlocks);
            buf.writeShort(combined.cardinality());
            long[] data = combined.toLongArray();
            buf.writeShort(data.length);
            for (long l : data) {
                buf.writeLong(l);
            }
        } finally {
            this.arcaneSectionLock.unlockRead(lock);
        }
    }

    /** Returns serialized payload for the codec. */
    public byte[] serialize(@Nonnull ExtraInfo extraInfo) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            serialize(buf);
            return ByteBufUtil.getBytesRelease(buf);
        } catch (Throwable t) {
            buf.release();
            throw SneakyThrow.sneakyThrow(t);
        }
    }

    /** Deserializes ticking state from the buffer (sets tickingBlocks and lastTickingBlocks under write lock). */
    private void deserialize(@Nonnull ByteBuf buf, int version) {
        buf.readUnsignedShort(); // cardinality (format compatibility with BlockSection)
        int len = buf.readUnsignedShort();
        long[] data = new long[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = buf.readLong();
        }
        BitSet restored = BitSet.valueOf(data);
        long writeStamp = this.arcaneSectionLock.writeLock();
        try {
            this.tickingBlocks = restored;
            this.lastTickingBlocks = (BitSet) restored.clone();
        } finally {
            this.arcaneSectionLock.unlockWrite(writeStamp);
        }
    }

    /** Entry point for codec: restore state from saved bytes. */
    public void deserialize(@Nonnull byte[] bytes, @Nonnull ExtraInfo extraInfo) {
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        deserialize(buf, extraInfo.getVersion());
    }

    @Override
    public Component<ChunkStore> clone() {
        return new ArcaneSection();
    }

    public static enum BlockTickStrategy {
        PROCESSED,
        WAIT_FOR_ADJACENT_CHUNK_LOAD,
        CONTINUE;
     
        private BlockTickStrategy() {
        }
     }

    private record TickRequest(int index, @Nonnull Instant requestedGameTime) {
    }
}

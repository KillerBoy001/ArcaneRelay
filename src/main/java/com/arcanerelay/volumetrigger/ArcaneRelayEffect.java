package com.arcanerelay.volumetrigger;

import com.arcanerelay.ArcaneRelayPlugin;

import com.arcanerelay.core.activation.ActivationExecutor;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import javax.annotation.Nonnull;

import org.joml.Vector3d;
import org.joml.Vector3i;

public class ArcaneRelayEffect extends TriggerEffect {

    @Nonnull
    public static final BuilderCodec<ArcaneRelayEffect> CODEC;
    @Nonnull
    private TriggerType action;
    private Vector3i pos;
    private PlayerRef PRef;

    public ArcaneRelayEffect() {
        this.action = TriggerType.TRIGGER_ALL;
    }

    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();
        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (world != null) {
            TransformComponent triggerTransform = (TransformComponent) store.getComponent(context.getEntityRef(), TransformComponent.getComponentType());
            Vector3d triggerPos = triggerTransform != null ? triggerTransform.getPosition() : new Vector3d(context.getVolume().getPosition());
            Vector3d min = new Vector3d();
            Vector3d max = new Vector3d();
            LongOpenHashSet processedBlocks = new LongOpenHashSet();

            for(VolumeEntry volume : context.getSpatialVolumes()) {
                TriggerVolumeShape shape = volume.getShape();
                Vector3d origin = volume.getPosition();
                shape.getWorldAABB(origin, min, max);
                int minX = MathUtil.floor(min.x());
                int minY = MathUtil.floor(min.y());
                int minZ = MathUtil.floor(min.z());
                int maxX = MathUtil.floor(max.x());
                int maxY = MathUtil.floor(max.y());
                int maxZ = MathUtil.floor(max.z());
                for(int x = minX; x <= maxX; ++x) {
                    for(int y = minY; y <= maxY; ++y) {
                        for(int z = minZ; z <= maxZ; ++z) {
                            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
                            if (chunk != null) {
                                BlockType blockType = chunk.getBlockType(x, y, z);
                                if (blockType != null) {
                                    Vector3i anchor = AnchorForCell(world, x, y, z);
                                    if (processedBlocks.add(BlockUtil.pack(anchor.x, anchor.y, anchor.z))) {
                                        WorldChunk chunkAtAnchor = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(anchor.x, anchor.z));
                                        if (chunkAtAnchor != null) {
                                            BlockType typeAtAnchor = chunkAtAnchor.getBlockType(anchor.x, anchor.y, anchor.z);
                                            if (typeAtAnchor != null && typeAtAnchor.getId().contains("Pseudo")) {
                                                this.SendTrigger(context,world, chunkAtAnchor, typeAtAnchor, anchor.x, anchor.y, anchor.z);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // End of execute

    @Nonnull
    private static Vector3i AnchorForCell(@Nonnull World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return new Vector3i(x, y, z);
        } else {
            int filler = chunk.getFiller(x, y, z);
            return filler == 0 ? new Vector3i(x, y, z) : new Vector3i(x - FillerBlockUtil.unpackX(filler), y - FillerBlockUtil.unpackY(filler), z - FillerBlockUtil.unpackZ(filler));
        }
    }

    static {CODEC = BuilderCodec.builder(
                    ArcaneRelayEffect.class,
                    ArcaneRelayEffect::new, BASE_CODEC)
                .append(new KeyedCodec<>("TriggerType", new EnumCodec<>(TriggerType.class)),
                        (e, v) -> e.action = v, (e) -> e.action)
                .add()
                .build();
    }

    public enum TriggerType {
        TRIGGER_ALL,
        TRIGGER_CONNECTIONS;
    }

    private void SendTrigger(@Nonnull TriggerContext context, @Nonnull World world, @Nonnull WorldChunk chunk, @Nonnull BlockType blockType, int x, int y, int z) {
        switch(this.action) {
            //------------------------------//
            case TRIGGER_ALL:
                PRef = context.getStore().getComponent(context.getEntityRef(), PlayerRef.getComponentType());
                PRef.sendMessage(Message.raw("VolumeTrigger: WIP Trigger All on block: "+blockType.getId()+" at: "+x+", "+y+", "+z));
                ArcaneRelayPlugin.LOGGER.atInfo().log("VolumeTrigger: WIP Trigger All on block: %s at: %d, %d, %d ", blockType.getId(), x, y, z);


                // Grabbing some data for usage, just remove what's not needed at the end
                Ref<ChunkStore> ChunkRef = chunk.getReference();
                Store<ChunkStore> ChunkStore = ChunkRef.getStore();
                Vector3i WorldBlockPos = new Vector3i(x, y, z);
                Vector3i ChunkPos = WorldPosToChunkPos(x, y, z);
                Vector3i BlockLocalPos = WorldPosToBlockChunkPos(x, y, z);
                int index = ChunkUtil.indexBlockInColumn(BlockLocalPos.x, BlockLocalPos.y, BlockLocalPos.z);
                Ref<ChunkStore> blockRef = chunk.getReference();


                ActivationExecutor.sendSignals(ChunkStore, blockRef, x, y, z);
                // Do Actual Triggering for TRIGGER ALL
                break;
            //------------------------------//
            case TRIGGER_CONNECTIONS:
                PRef = context.getStore().getComponent(context.getEntityRef(), PlayerRef.getComponentType());
                PRef.sendMessage(Message.raw("VolumeTrigger: WIP Trigger Connections on block: "+blockType.getId()+" at: "+x+", "+y+", "+z));
                ArcaneRelayPlugin.LOGGER.atInfo().log("VolumeTrigger: WIP Trigger Connections on block: %s at: %d, %d, %d ", blockType.getId(), x, y, z);
                // Do Actual Triggering for TRIGGER CONNECTIONS
                break;
        }
    }


    private Vector3i WorldPosToChunkPos(int x, int y, int z){
        int cX = x >> 5;
        int cZ = y >> 5;
        return new Vector3i(cX,0,cZ);
    }

    private Vector3i WorldPosToBlockChunkPos(int x, int y, int z){
        int X = x & 31;
        int Z = z & 31;
        int Y = y;
        return new Vector3i(X,Y,Z);
    }

}



package com.arcanerelay.volumetrigger;

import com.arcanerelay.ArcaneRelayPlugin;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class ArcaneRelayEffect extends TriggerEffect {

    @Nonnull
    public static final BuilderCodec<ArcaneRelayEffect> CODEC;
    @Nonnull
    private ArcaneRelayEffect.Actions action;

    public ArcaneRelayEffect() {
        this.action = ArcaneRelayEffect.Actions.TRIGGER;
    }

    public void execute(@Nonnull TriggerContext context) {
        Store<EntityStore> store = context.getStore();
        World world = ((EntityStore)store.getExternalData()).getWorld();
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
                                    Vector3i anchor = doorAnchorForCell(world, x, y, z);
                                    if (processedBlocks.add(BlockUtil.pack(anchor.x, anchor.y, anchor.z))) {
                                        WorldChunk chunkAtAnchor = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(anchor.x, anchor.z));
                                        if (chunkAtAnchor != null) {
                                            BlockType typeAtAnchor = chunkAtAnchor.getBlockType(anchor.x, anchor.y, anchor.z);
                                            if (typeAtAnchor != null && typeAtAnchor.getId().contains("Pseudo")) {
                                                this.SendTrigger(anchor.x, anchor.y, anchor.z);
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
    private static Vector3i doorAnchorForCell(@Nonnull World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return new Vector3i(x, y, z);
        } else {
            int filler = chunk.getFiller(x, y, z);
            return filler == 0 ? new Vector3i(x, y, z) : new Vector3i(x - FillerBlockUtil.unpackX(filler), y - FillerBlockUtil.unpackY(filler), z - FillerBlockUtil.unpackZ(filler));
        }
    }

    static {
        CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(ArcaneRelayEffect.class, ArcaneRelayEffect::new, BASE_CODEC).append(new KeyedCodec("Action", new EnumCodec(ArcaneRelayEffect.Actions.class)), (e, v) -> e.action = v, (e) -> e.action).add()).build();
    }

    public static enum Actions {
        TRIGGER;
    }

    private void SendTrigger(int BlockX,int BlockY,int BlockZ){
            ArcaneRelayPlugin.LOGGER.atInfo().log("VolumeTrigger: WIP Trigger on block: %d, %d, %d ",BlockX,BlockY,BlockZ);
    }

}

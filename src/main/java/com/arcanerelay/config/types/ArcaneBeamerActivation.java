package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcanePullerBlock;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ActivationExecutor;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.arcanerelay.resources.ArcaneMoveState;
import com.arcanerelay.util.ArcaneConnectedBlocksUtil;
import com.arcanerelay.util.ArcaneUtil;
import com.arcanerelay.util.BlockVectorUtil;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ArcaneBeamerActivation extends Activation {
    private int range = 15;
    private String LaserKey ="Pseudo_Arcane_Beamer_Extension";
    public static final BuilderCodec<ArcaneBeamerActivation> CODEC = BuilderCodec.builder(
                    ArcaneBeamerActivation.class,
                    ArcaneBeamerActivation::new,
                    Activation.ABSTRACT_CODEC)
            .documentation("Builds the laser beam needed for presence detection")
            .appendInherited(
                    new KeyedCodec<>("Range", Codec.INTEGER),
                    (a, r) -> a.range = r,
                    a -> a.range,
                    (a, p) -> a.range = p.range)
            .documentation("Maximum extension range (default: 15).")
            .add()
            .build();

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    @Override
    public ArcaneSection.BlockTickStrategy execute(
            @Nonnull ArcaneCachedAccessor accessor,
            @Nullable Ref<ChunkStore> sectionRef,
            @Nullable Ref<ChunkStore> blockRef,
            int worldX, int worldY, int worldZ,
            @Nonnull List<int[]> sources
    ) {
        ChunkStoreCommandBufferLike commandBuffer = accessor.getCommandBuffer();

        if (blockRef == null || !blockRef.isValid()) {
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        World world = commandBuffer.getExternalData().getWorld();
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (chunk == null) return ArcaneSection.BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;

        BlockType BeamerBlockType = chunk.getBlockType(worldX, worldY, worldZ);
        if (BeamerBlockType == null) return ArcaneSection.BlockTickStrategy.PROCESSED;

        Vector3i BeamerPos = new Vector3i(worldX, worldY, worldZ);
        Vector3i globalUp = BlockVectorUtil.getUpVector(chunk, BeamerPos);
        String state = BeamerBlockType.getStateForBlock(BeamerBlockType);
        if (state == null || state.isEmpty() || "null".equals(state)) {
            state = "Disabled";
        }

        if (globalUp.length() == 0) return ArcaneSection.BlockTickStrategy.PROCESSED;
        int maxRange = getRange();

        if (state.contains("Disabled")){ // PriorState since this triggers before actual statechange.
            ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Enabling");
            BuildLaserBeam(commandBuffer, BeamerPos, chunk, maxRange);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        } else if (state.contains("Enabled")){ // PriorState since this triggers before actual statechange.
            ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Disabling");
            BlockVectorUtil.setTickingAround(chunk, BeamerPos, 1);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    private ArcaneSection.BlockTickStrategy BuildLaserBeam(
            ChunkStoreCommandBufferLike commandBuffer,
            Vector3i BeamerPos,
            WorldChunk beamerChunk,
            int maxRange
    ) {
        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();

        for (int i = 0; i <= maxRange; i++) {
            Vector3i Localforward = BlockVectorUtil.getForwardVector(beamerChunk,BeamerPos,i+1 ); // +1 cause we wanna start ahead.
            Vector3i NextPos = new Vector3i (BeamerPos.x + Localforward.x, BeamerPos.y + Localforward.y, BeamerPos.z + Localforward.z);
            BlockType Block = beamerChunk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            int rotind = beamerChunk.getRotationIndex(BeamerPos.x,BeamerPos.y,BeamerPos.z);

            int LaserID = assetMap.getIndex(LaserKey);
            BlockType LaserType = assetMap.getAsset(LaserKey);

            if (Block==LaserType) {
                // Just log and move on ,when rebuilding partial existing laser
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Existing laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
            } else if (BlockVectorUtil.isEmpty(Block)) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Creating laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
                beamerChunk.setBlock(NextPos.x, NextPos.y, NextPos.z, LaserID, LaserType, rotind, 0, 4);
                BlockVectorUtil.setTickingAround(beamerChunk, NextPos, 1);
            }else if(i == maxRange || !BlockVectorUtil.isEmpty(Block)){ //Not empty
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Created laser with range: %d", i);
                break;
            }
        }
        return ArcaneSection.BlockTickStrategy.CONTINUE;
    }
}
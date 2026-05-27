package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.Activation;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
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
import java.util.concurrent.*;


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
        World world = commandBuffer.getExternalData().getWorld();

        //if (blockRef == null || !blockRef.isValid()) {  //Makes the beam trigger not continue
        //    return ArcaneSection.BlockTickStrategy.PROCESSED;
        //}

        WorldChunk Triggerchunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (Triggerchunk == null) return ArcaneSection.BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;

        world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        BlockType TriggerBlockType = Triggerchunk.getBlockType(worldX, worldY, worldZ);
        if (TriggerBlockType == null) return ArcaneSection.BlockTickStrategy.PROCESSED;
        Vector3i TriggerPos = new Vector3i(worldX, worldY, worldZ);

        if (TriggerBlockType.getId().equals(LaserKey)){                             // Handle laser trigger
            int maxRange = getRange();
            Vector3i BeamerPos = GetBeamerPosFromLaser(commandBuffer,TriggerPos,Triggerchunk,maxRange);
            WorldChunk BeamerChunk = commandBuffer.getExternalData().getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(BeamerPos.x, BeamerPos.z));
            SendTriggerFromSourceBeamer(commandBuffer, BeamerPos, BeamerChunk);
        } else {                                                                    // Handle beamer trigger
            String state = TriggerBlockType.getStateForBlock(TriggerBlockType);
            if (state == null || state.isEmpty() || "null".equals(state)) {
                state = "Off";
            }

            int maxRange = getRange();

            if (state.contains("On")) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Enabling");
                BuildLaserBeam(commandBuffer, TriggerPos, Triggerchunk, maxRange);
                return ArcaneSection.BlockTickStrategy.PROCESSED;
            } else if (state.contains("Off")) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Disabling");
                BlockVectorUtil.setTickingAround(Triggerchunk, TriggerPos, 1);
                return ArcaneSection.BlockTickStrategy.PROCESSED;
            }
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
            Vector3i Localforward = BlockVectorUtil.getForwardVector(beamerChunk,BeamerPos,i+1 );
            Vector3i NextPos = new Vector3i (BeamerPos.x + Localforward.x, BeamerPos.y + Localforward.y, BeamerPos.z + Localforward.z);

            WorldChunk chnk = commandBuffer.getExternalData().getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(NextPos.x, NextPos.z));
            BlockType Block = chnk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            int rotind = beamerChunk.getRotationIndex(BeamerPos.x,BeamerPos.y,BeamerPos.z);

            int LaserID = assetMap.getIndex(LaserKey);
            BlockType LaserType = assetMap.getAsset(LaserKey);

            if (Block.getId().contains("Extension")) {
                // Just log and move on ,when rebuilding partial existing laser
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Existing laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
            } else if (BlockVectorUtil.isEmpty(Block)) {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Creating laser at: %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
                chnk.setBlock(NextPos.x, NextPos.y, NextPos.z, LaserID, LaserType, rotind, 0, 4);
                BlockVectorUtil.setTickingAround(chnk, NextPos, 1);
            }else if(!BlockVectorUtil.isEmpty(Block)){ //Not empty
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Created laser with range: %d", i);
                break;
            }
            if(i == maxRange) ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Created laser with maxrange: %d", i);
        }
        return ArcaneSection.BlockTickStrategy.CONTINUE;
    }

    private void SendTriggerFromSourceBeamer (
            ChunkStoreCommandBufferLike commandBuffer,
            Vector3i BeamerPos,
            WorldChunk beamerChunk
    ) {
        ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Send signal now - WIP");
    }

    Vector3i GetBeamerPosFromLaser(ChunkStoreCommandBufferLike commandBuffer,Vector3i TriggerPos,WorldChunk Chunk,int maxRange){
        for (int i = 1; i <= maxRange+1; i++){
            Vector3i LocalBackward = BlockVectorUtil.getForwardVector(Chunk,TriggerPos,i-i*2 );
            Vector3i NextPos = new Vector3i (TriggerPos.x + LocalBackward.x, TriggerPos.y + LocalBackward.y, TriggerPos.z + LocalBackward.z);

            WorldChunk chnk = commandBuffer.getExternalData().getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(NextPos.x, NextPos.z));
            BlockType Block = chnk.getBlockType(NextPos.x,NextPos.y,NextPos.z);
            String BlockId = Block.getId();
            if (!BlockId.contains("Extension")&&BlockId.contains("On")){
                ArcaneRelayPlugin.LOGGER.atInfo().log("Beamer: Found laser source at : %d,%d,%d", NextPos.x, NextPos.y, NextPos.z);
                return NextPos;
            }
        }
        return null;
    }
}
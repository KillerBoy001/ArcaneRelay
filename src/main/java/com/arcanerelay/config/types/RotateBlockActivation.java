package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.util.BlockVectorUtil;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RotateBlockActivation extends Activation {
    private String[] RotTypeID = new String[0];
    public static final BuilderCodec<RotateBlockActivation> CODEC = BuilderCodec.builder(
                    RotateBlockActivation.class,
                    RotateBlockActivation::new,
                    Activation.ABSTRACT_CODEC)
            .documentation("Rotates the block On-top of rotator")
            .appendInherited(
                    new KeyedCodec<>("Activations", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (a, ids) -> a.RotTypeID = ids,
                    a -> a.RotTypeID,
                    (a, p) -> a.RotTypeID = p.RotTypeID
            )
            .documentation("Type of rotation either Clockwise or Counter-Clockwise")
            .add()
            .build();


    private boolean isClockWise(BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId();
        return id != null && id.toLowerCase().contains("rotatorl");
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
        commandBuffer.run((@Nonnull Store<ChunkStore> store) -> {

            World world = store.getExternalData().getWorld();
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
            BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
            // Rotator info
            BlockType RotatorBlockType = chunk.getBlockType(worldX, worldY, worldZ);
            Vector3i RotatorPos = new Vector3i(worldX, worldY, worldZ);
            boolean IsClockWise = isClockWise(RotatorBlockType); // If RotatorL

            // Target Info
            Vector3i TempUp = BlockVectorUtil.getUpVector(chunk, RotatorPos);
            Vector3i TargetPos = new Vector3i (RotatorPos.x+TempUp.x,RotatorPos.y+TempUp.y,RotatorPos.z+TempUp.z);
            BlockType TargetBlockType = chunk.getBlockType(TargetPos.x, TargetPos.y, TargetPos.z);
            //String TargetID = TargetBlockType.getId();
            String TargetID = com.arcanerelay.util.ArcaneUtil.getOriginalBlockTypeId(TargetBlockType); // Makes sure it doesnt do initial animation

            RotationTuple currenRotation = RotationTuple.get(chunk.getRotationIndex(TargetPos.x, TargetPos.y, TargetPos.z));
            Vector3i rotatorUp = BlockVectorUtil.getUpVector(chunk, RotatorPos);

            RotationTuple newRotation = BlockVectorUtil.rotateOverAxis90Degrees(currenRotation, rotatorUp, IsClockWise);

            if (currenRotation.index() == newRotation.index()) {
                return;
            }

            if(BlockVectorUtil.isRotatable(TargetBlockType)) {
                chunk.setBlock(TargetPos.x, TargetPos.y, TargetPos.z, assetMap.getIndex(TargetID), TargetBlockType, newRotation.index(), 0, 4);
                BlockVectorUtil.setTickingAround(chunk,TargetPos,1);
            } else {
                ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Block of type: '%s', is not allowed to be rotated", TargetBlockType.getId());
            }
        });

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }
}


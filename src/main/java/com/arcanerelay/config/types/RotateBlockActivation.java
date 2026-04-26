package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import static com.arcanerelay.config.types.MoveBlockActivation.GetUpFromBlock;
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
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RotateBlockActivation extends Activation {
    private String[] RotTypeID = new String[0]
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

    public static int GetNewTarRotIndex(int OwnRotIndex,int TargetRotIndex, boolean Clockwise){
        if(Clockwise && OwnRotIndex ==0) { //Inversed
            return switch (TargetRotIndex) {
                case 0 -> 3;
                case 3 -> 2;
                case 2 -> 1;
                case 1 -> 0;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==0){ //Normal
            return switch (TargetRotIndex) {
                case 0 -> 1;
                case 3 -> 0;
                case 2 -> 3;
                case 1 -> 2;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==4) { //Inversed
            return switch (TargetRotIndex) {
                case 10 -> 26;
                case 26 -> 0;
                case 0 -> 16;
                case 16 -> 10;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==4){ //Normal
            return switch (TargetRotIndex) {
                case 11 -> 27;
                case 27 -> 1;
                case 1 -> 17;
                case 17 -> 11;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==5) { //Inversed
            return switch (TargetRotIndex) {
                case 11 -> 17;
                case 17 -> 1;
                case 1 -> 27;
                case 27 -> 11;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==5){ //Normal
            return switch (TargetRotIndex) {
                case 11 -> 27;
                case 27 -> 1;
                case 1 -> 17;
                case 17 -> 11;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==6) { //Inversed
            return switch (TargetRotIndex) {
                case 9 -> 19;
                case 19 -> 3;
                case 3 -> 25;
                case 25 -> 9;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==6){ //Normal
            return switch (TargetRotIndex) {
                case 8 -> 24;
                case 24 -> 2;
                case 2 -> 18;
                case 18 -> 8;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==7) { //Inversed
            return switch (TargetRotIndex) {
                case 8 -> 18;
                case 18 -> 2;
                case 2 -> 24;
                case 24 -> 8;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==7){ //Normal
            return switch (TargetRotIndex) {
                case 9 -> 25;
                case 25 -> 3;
                case 3 -> 19;
                case 19 -> 9;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==8) { //Inversed
            return switch (TargetRotIndex) {
                case 9 -> 10;
                case 10 -> 11;
                case 11 -> 8;
                case 8 -> 9;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==8){ //Normal
            return switch (TargetRotIndex) {
                case 9 -> 8;
                case 8 -> 11;
                case 11 -> 10;
                case 10 -> 9;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        else return TargetRotIndex; // If Rotation is not supported return source
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
            Vector3i TempUp = GetUpFromBlock(chunk, RotatorPos, false,1);
            Vector3i globalUp = new Vector3i (RotatorPos.x+TempUp.x,RotatorPos.y+TempUp.y,RotatorPos.z+TempUp.z);
            BlockType TargetBlockType = chunk.getBlockType(globalUp.x, globalUp.y,globalUp.z);
            String TargetID = TargetBlockType.getId();

            int OwnRotIndex = chunk.getRotationIndex(worldX, worldY, worldZ);
            int TargetRotIndex = chunk.getRotationIndex(globalUp.x, globalUp.y, globalUp.z);

            int NewRotInd = GetNewTarRotIndex(OwnRotIndex,TargetRotIndex,IsClockWise);
            if (TargetRotIndex==NewRotInd){
                ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Unknown/Unsupported rotation NewRotIndex sourceRotInd: %d, TargetRotIndex %d, No change",OwnRotIndex,TargetRotIndex);
            }else{
                chunk.setBlock(globalUp.x, globalUp.y, globalUp.z, assetMap.getIndex(TargetID), TargetBlockType, NewRotInd, 0, 0);
            }
        });

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

}

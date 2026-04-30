package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import static com.arcanerelay.util.BlockVectorUtil.*;
import com.arcanerelay.util.ArcaneUtil;
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

    public static int GetNewTarRotIndex(int OwnRotIndex,int TargetRotIndex, boolean Clockwise){
        if(Clockwise && OwnRotIndex ==0) { //Inversed
            return switch (TargetRotIndex) {
                case 0 -> 3;
                case 3 -> 2;
                case 2 -> 1;
                case 1 -> 0;

                case 4 -> 7;
                case 7 -> 6;
                case 6 -> 5;
                case 5 -> 4;

                case 9 -> 8;
                case 8 -> 11;
                case 11 -> 10;
                case 10 -> 9;

                case 13 -> 12;
                case 12 -> 15;
                case 15 -> 14;
                case 14 -> 13;

                case 16 -> 19;
                case 17 -> 16;
                case 18 -> 17;
                case 19 -> 18;

                case 24 -> 27;
                case 25 -> 24;
                case 26 -> 25;
                case 27 -> 26;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==0){ //Normal
            return switch (TargetRotIndex) {
                case 0 -> 1;
                case 3 -> 0;
                case 2 -> 3;
                case 1 -> 2;

                case 4 -> 5;
                case 5 -> 6;
                case 6 -> 7;
                case 7 -> 4;

                case 8 -> 9;
                case 9 -> 10;
                case 10 -> 11;
                case 11 -> 8;

                case 12 -> 13;
                case 13 -> 14;
                case 14 -> 15;
                case 15 -> 12;

                case 16 -> 17;
                case 17 -> 18;
                case 18 -> 19;
                case 19 -> 16;

                case 24 -> 25;
                case 25 -> 26;
                case 26 -> 27;
                case 27 -> 24;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==4) { //Inversed
            return switch (TargetRotIndex) {
                case 10 -> 16;
                case 16 -> 0;
                case 0 -> 26;
                case 26 -> 10;

                case 9 -> 13;
                case 13 -> 1;
                case 1 -> 5;
                case 5 -> 9;

                case 8 -> 24;
                case 24 -> 2;
                case 2 -> 18;
                case 18 -> 8;

                case 11 -> 7;
                case 7 -> 3;
                case 3 -> 15;
                case 15 -> 11;

                case 25 -> 14;
                case 14 -> 17;
                case 17 -> 4;
                case 4 -> 25;

                case 19 -> 12;
                case 12 -> 27;
                case 27 -> 6;
                case 6 -> 19;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==4){ //Normal
            return switch (TargetRotIndex) {
                case 10 -> 26;
                case 26 -> 0;
                case 0 -> 16;
                case 16 -> 10;

                case 9 -> 5;
                case 5 -> 1;
                case 1 -> 13;
                case 13 -> 9;

                case 24 -> 8;
                case 2 -> 24;
                case 18 -> 2;
                case 8 -> 18;

                case 7 -> 11;
                case 3 -> 7;
                case 15 -> 3;
                case 11 -> 15;

                case 14 -> 25;
                case 17 -> 14;
                case 4 -> 17;
                case 25 -> 4;

                case 12 -> 19;
                case 27 -> 12;
                case 6 -> 27;
                case 19 -> 6;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==5) { //Inversed
            return switch (TargetRotIndex) {
                case 11 -> 17;
                case 17 -> 1;
                case 1 -> 27;
                case 27 -> 11;

                case 0 -> 12;
                case 12 -> 8;
                case 8 -> 4;
                case 4 -> 0;

                case 7 -> 16;
                case 16 -> 13;
                case 13 -> 24;
                case 24 -> 7;

                case 9 -> 25;
                case 19 -> 9;
                case 3 -> 19;
                case 25 -> 3;

                case 5 -> 26;
                case 18 -> 5;
                case 15 -> 18;
                case 26 -> 15;

                case 14 -> 2;
                case 10 -> 14;
                case 6 -> 10;
                case 2 -> 6;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==5){ //Normal
            return switch (TargetRotIndex) {
                case 11 -> 27;
                case 27 -> 1;
                case 1 -> 17;
                case 17 -> 11;

                case 0 -> 4;
                case 4 -> 8;
                case 8 -> 12;
                case 12 -> 0;

                case 7 -> 24;
                case 24 -> 13;
                case 13 -> 16;
                case 16 -> 7;

                case 25 -> 9;
                case 9 -> 19;
                case 19 -> 3;
                case 3 -> 25;

                case 26 -> 5;
                case 5 -> 18;
                case 18 -> 15;
                case 15 -> 26;

                case 2 -> 14;
                case 14 -> 10;
                case 10 -> 6;
                case 6 -> 2;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==6) { //Inversed
            return switch (TargetRotIndex) {
                case 10 -> 26;
                case 26 -> 0;
                case 0 -> 16;
                case 16 -> 10;

                case 9 -> 5;
                case 5 -> 1;
                case 1 -> 13;
                case 13 -> 9;

                case 24 -> 8;
                case 2 -> 24;
                case 18 -> 2;
                case 8 -> 18;

                case 7 -> 11;
                case 3 -> 7;
                case 15 -> 3;
                case 11 -> 15;

                case 14 -> 25;
                case 17 -> 14;
                case 4 -> 17;
                case 25 -> 4;

                case 12 -> 19;
                case 27 -> 12;
                case 6 -> 27;
                case 19 -> 6;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==6){ //Normal
            return switch (TargetRotIndex) {
                case 10 -> 16;
                case 16 -> 0;
                case 0 -> 26;
                case 26 -> 10;

                case 9 -> 13;
                case 13 -> 1;
                case 1 -> 5;
                case 5 -> 9;

                case 8 -> 24;
                case 24 -> 2;
                case 2 -> 18;
                case 18 -> 8;

                case 11 -> 7;
                case 7 -> 3;
                case 3 -> 15;
                case 15 -> 11;

                case 25 -> 14;
                case 14 -> 17;
                case 17 -> 4;
                case 4 -> 25;

                case 19 -> 12;
                case 12 -> 27;
                case 27 -> 6;
                case 6 -> 19;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==7) { //Inversed
            return switch (TargetRotIndex) {
                case 11 -> 27;
                case 27 -> 1;
                case 1 -> 17;
                case 17 -> 11;

                case 0 -> 4;
                case 4 -> 8;
                case 8 -> 12;
                case 12 -> 0;

                case 7 -> 24;
                case 24 -> 13;
                case 13 -> 16;
                case 16 -> 7;

                case 25 -> 9;
                case 9 -> 19;
                case 19 -> 3;
                case 3 -> 25;

                case 26 -> 5;
                case 5 -> 18;
                case 18 -> 15;
                case 15 -> 26;

                case 2 -> 14;
                case 14 -> 10;
                case 10 -> 6;
                case 6 -> 2;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==7){ //Normal
            return switch (TargetRotIndex) {
                case 11 -> 17;
                case 17 -> 1;
                case 1 -> 27;
                case 27 -> 11;

                case 0 -> 12;
                case 12 -> 8;
                case 8 -> 4;
                case 4 -> 0;

                case 7 -> 16;
                case 16 -> 13;
                case 13 -> 24;
                case 24 -> 7;

                case 9 -> 25;
                case 19 -> 9;
                case 3 -> 19;
                case 25 -> 3;

                case 5 -> 26;
                case 18 -> 5;
                case 15 -> 18;
                case 26 -> 15;

                case 14 -> 2;
                case 10 -> 14;
                case 6 -> 10;
                case 2 -> 6;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(Clockwise && OwnRotIndex ==8) { //Inversed
            return switch (TargetRotIndex) {
                case 0 -> 1;
                case 3 -> 0;
                case 2 -> 3;
                case 1 -> 2;

                case 4 -> 5;
                case 5 -> 6;
                case 6 -> 7;
                case 7 -> 4;

                case 8 -> 9;
                case 9 -> 10;
                case 10 -> 11;
                case 11 -> 8;

                case 12 -> 13;
                case 13 -> 14;
                case 14 -> 15;
                case 15 -> 12;

                case 16 -> 17;
                case 17 -> 18;
                case 18 -> 19;
                case 19 -> 16;

                case 24 -> 25;
                case 25 -> 26;
                case 26 -> 27;
                case 27 -> 24;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if(!Clockwise && OwnRotIndex ==8){ //Normal
            return switch (TargetRotIndex) {
                case 0 -> 3;
                case 3 -> 2;
                case 2 -> 1;
                case 1 -> 0;

                case 4 -> 7;
                case 7 -> 6;
                case 6 -> 5;
                case 5 -> 4;

                case 9 -> 8;
                case 8 -> 11;
                case 11 -> 10;
                case 10 -> 9;

                case 13 -> 12;
                case 12 -> 15;
                case 15 -> 14;
                case 14 -> 13;

                case 16 -> 19;
                case 17 -> 16;
                case 18 -> 17;
                case 19 -> 18;

                case 24 -> 27;
                case 25 -> 24;
                case 26 -> 25;
                case 27 -> 26;
                default -> TargetRotIndex; // If Rotation is not supported return source
            };
        }
        if (OwnRotIndex >=9){
            ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Error, None supported Rotator rotation, Was a rotator turned with another rotator ?");
            return TargetRotIndex;
        }
        else {
            ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Unknown/Unsupported target rotation Rotator-RotIndex: %d, Target-RotIndex %d, No change's on target",OwnRotIndex,TargetRotIndex);
            return TargetRotIndex; // If Rotation is not supported return source
        }
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
            Vector3i TempUp = GetUpVector(chunk, RotatorPos);
            Vector3i TargetPos = new Vector3i (RotatorPos.x+TempUp.x,RotatorPos.y+TempUp.y,RotatorPos.z+TempUp.z);
            BlockType TargetBlockType = chunk.getBlockType(TargetPos.x, TargetPos.y, TargetPos.z);
            String TargetID = TargetBlockType.getId();

            int OwnRotIndex = chunk.getRotationIndex(worldX, worldY, worldZ);
            int TargetRotIndex = chunk.getRotationIndex(TargetPos.x, TargetPos.y, TargetPos.z);

            int NewRotInd = GetNewTarRotIndex(OwnRotIndex,TargetRotIndex,IsClockWise);

            if (TargetRotIndex !=NewRotInd){
                Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                if(isRotatable(TargetBlockType)) {
                    chunk.setBlock(TargetPos.x, TargetPos.y, TargetPos.z, assetMap.getIndex(TargetID), TargetBlockType, NewRotInd, 0, 0);
                    ArcaneUtil.setTicking(chunkStore, TargetPos.x, TargetPos.y, TargetPos.z);
                } else {
                    ArcaneRelayPlugin.LOGGER.atInfo().log("Rotator: Block of type: '%s', is not allowed to be rotated",TargetBlockType.getId());
                }

            }
        });

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

}

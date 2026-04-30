package com.arcanerelay.util;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Arrays;

public class BlockVectorUtil {

    static List<String> NoneMoveableIDs = Arrays.asList(
            "Void_Suspender"
    );

    static List<String> NoneRotatableIDs = Arrays.asList(
            "Void_Suspender", "Soil_Grass", "Bench", "Bed"
    );

    private static boolean isExtensionBlock(@Nullable BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId();
        if (id == null) return false;
        String lower = id.toLowerCase();
        return lower.contains("puller") && lower.contains("extension");
    }

    public static boolean isEmpty(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0 ) return false;
        return isEmpty(blockType);
    }
    public static boolean isEmpty(@Nullable BlockType blockType) {
        if (blockType == null) return true;
        return blockType.getMaterial() == BlockMaterial.Empty;
    }

    public static boolean isPullable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0 ) return false;
        return isPullable(blockType);
    }
    public static boolean isPullable(@Nullable BlockType blockType) {
        String id = blockType.getId();
        if (blockType == null) return false;
        if (isExtensionBlock(blockType)) return false;
        for (String keyword : NoneMoveableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }
        return blockType.getMaterial() == BlockMaterial.Solid;
    }

    public static boolean isMoveable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0 ) return false;
        return isMoveable(blockType);
    }
    public static boolean isMoveable(@Nullable BlockType blockType) {
        String id = blockType.getId();
        if (blockType == null) return false;
        for (String keyword : NoneMoveableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }
        return blockType.getMaterial() != BlockMaterial.Empty;
    }

    public static boolean isRotatable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0 ) return false;
        return isRotatable(blockType);
    }
    public static boolean isRotatable(@Nullable BlockType blockType) {
        String id = blockType.getId();
        if (blockType == null)
            return false;
        for (String keyword : NoneRotatableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }
        return blockType.getMaterial() != BlockMaterial.Empty;
    }

    /** Gets the local upward Vector from a block based on its RotationIndex. */
    public static Vector3i GetUpVector(@Nonnull WorldChunk chnk, Vector3i SourcePos) {
        return GetUpVector(chnk,SourcePos,false,1);
    }
    public static Vector3i GetUpVector(@Nonnull WorldChunk chnk, Vector3i SourcePos,int Distance) {
        return GetUpVector(chnk,SourcePos,false,Distance);
    }
    public static  Vector3i GetUpVector(@Nonnull WorldChunk chnk, Vector3i SourcePos, boolean IsWallPusher) {
        return GetUpVector(chnk,SourcePos,IsWallPusher,1);
    }
    public static Vector3i GetUpVector(@Nonnull WorldChunk chnk, Vector3i SourcePos,boolean IsWallPusher,int Distance){
        int RotIndex = chnk.getRotationIndex(SourcePos.x,SourcePos.y,SourcePos.z);
        if(IsWallPusher) {
            return switch (RotIndex) {
                case 11 -> new Vector3i(Distance, 0, 0); //EastWall Facing Up
                case 17 -> new Vector3i(Distance, 0, 0); //EastWall Facing Right
                case 27 -> new Vector3i(Distance, 0, 0); //EastWall Facing Left
                case 1 -> new Vector3i(Distance, 0, 0); //EastWall Facing Down

                case 8 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Up
                case 18 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Right
                case 24 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Left
                case 2 -> new Vector3i(0, 0, -Distance); //NorthWall Facing Down

                case 9 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Up
                case 19 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Right
                case 25 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Left
                case 3 -> new Vector3i(-Distance, 0, 0); //WestWall Facing Down

                case 10 -> new Vector3i(0, 0, Distance); //SouthWall Facing Up
                case 16 -> new Vector3i(0, 0, Distance); //SouthWall Facing Right
                case 26 -> new Vector3i(0, 0, Distance); //SouthWall Facing Left
                case 0 -> new Vector3i(0, 0, Distance); //SouthWall Facing Down

                case 15 -> new Vector3i(0, Distance, 0); //Facing West Upright
                case 12 -> new Vector3i(0, Distance, 0); //Facing South Upright
                case 13 -> new Vector3i(0, Distance, 0); //Facing East Upright
                case 14 -> new Vector3i(0, Distance, 0); //Facing North Upright

                case 4 -> new Vector3i(0, -Distance, 0); //Facing North UpsideDown
                case 5 -> new Vector3i(0, -Distance, 0); //Facing West UpsideDown
                case 6 -> new Vector3i(0, -Distance, 0); //Facing South UpsideDown
                case 7 -> new Vector3i(0, -Distance, 0); //Facing East UpsideDown

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        } else{
            return switch (RotIndex) {
                case 0 -> new Vector3i(0, Distance, 0); //Facing North Upright
                case 1 -> new Vector3i(0, Distance, 0); //Facing West Upright
                case 2 -> new Vector3i(0, Distance, 0); //Facing South Upright
                case 3 -> new Vector3i(0, Distance, 0); //Facing East Upright

                case 4 -> new Vector3i(0, 0, Distance); //DoublePipe SouthWall extra for pullers and rotators
                case 5 -> new Vector3i(Distance, 0, 0); //DoublePipe EastWall extra for pullers and rotators
                case 6 -> new Vector3i(0, 0, -Distance); //DoublePipe NorthWall extra for pullers and rotators
                case 7 -> new Vector3i(-Distance, 0, 0); //DoublePipe WestWall extra for pullers and rotators

                case 8 -> new Vector3i(0, -Distance, 0); //Facing South UpsideDown
                case 9 -> new Vector3i(0, -Distance, 0); //Facing East UpsideDown
                case 10 -> new Vector3i(0, -Distance, 0); //Facing North UpsideDown
                case 11 -> new Vector3i(0, -Distance, 0); //Facing West UpsideDown

                case 12 -> new Vector3i(0, 0, -Distance);
                case 13 -> new Vector3i(-Distance, 0, 0);
                case 14 -> new Vector3i(0, 0, Distance);
                case 15 -> new Vector3i(Distance, 0, 0);

                case 24 -> new Vector3i(-Distance, 0, 0);
                case 25 -> new Vector3i(0, 0, Distance);
                case 27 -> new Vector3i(0, 0, -Distance);

                case 49 -> new Vector3i(0, 0, -Distance); //Facing West LayingOnRightSide
                case 50 -> new Vector3i(-Distance, 0, 0); //Facing South LayingOnRightSide
                case 26 -> new Vector3i(Distance, 0, 0); //Facing North LayingOnRightSide
                case 51 -> new Vector3i(0, 0, Distance); //Facing East LayingOnRightSide

                case 16 -> new Vector3i(-Distance, 0, 0); //Facing North LayingOnLeftSide
                case 17 -> new Vector3i(0, 0, Distance); //Facing West LayingOnLeftSide
                case 18 -> new Vector3i(Distance, 0, 0); //Facing South LayingOnRightSide
                case 19 -> new Vector3i(0, 0, -Distance); //Facing East LayingOnLeftSide

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        }
    }

    /** Gets the local forward Vector from a block based on its RotationIndex. */
    public static Vector3i GetForwardVector(@Nonnull WorldChunk chnk, Vector3i SourcePos) {
        return GetForwardVector(chnk,SourcePos,false,1);
    }
    public static Vector3i GetForwardVector(@Nonnull WorldChunk chnk, Vector3i SourcePos,int Distance) {
        return GetForwardVector(chnk,SourcePos,false,Distance);
    }
    public static Vector3i GetForwardVector(@Nonnull WorldChunk chnk, Vector3i SourcePos, boolean IsWallPusher) {
        return GetForwardVector(chnk,SourcePos,IsWallPusher,1);
    }
    public static Vector3i GetForwardVector(@Nonnull WorldChunk chnk,Vector3i SourcePos,boolean IsWallPusher,int Distance){
        int RotIndex = chnk.getRotationIndex(SourcePos.x,SourcePos.y,SourcePos.z);
        if(IsWallPusher) {
            return switch (RotIndex) {
                case 11 -> new Vector3i(0, Distance, 0); //EastWall Facing Up
                case 17 -> new Vector3i(0, 0, -Distance); //EastWall Facing Right
                case 27 -> new Vector3i(0, 0, Distance); //EastWall Facing Left
                case 1 -> new Vector3i(0, -Distance, 0); //EastWall Facing Down

                case 8 -> new Vector3i(0, Distance, 0); //NorthWall Facing Up
                case 18 -> new Vector3i(-Distance, 0, 0); //NorthWall Facing Right
                case 24 -> new Vector3i(Distance, 0, 0); //NorthWall Facing Left
                case 2 -> new Vector3i(0, -Distance, 0); //NorthWall Facing Down

                case 9 -> new Vector3i(0, Distance, 0); //WestWall Facing Up
                case 19 -> new Vector3i(0, 0, Distance); //WestWall Facing Right
                case 25 -> new Vector3i(0, 0, -Distance); //WestWall Facing Left
                case 3 -> new Vector3i(0, -Distance, 0); //WestWall Facing Down

                case 10 -> new Vector3i(0, Distance, 0); //SouthWall Facing Up
                case 16 -> new Vector3i(Distance, 0, 0); //SouthWall Facing Right
                case 26 -> new Vector3i(-Distance, 0, 0); //SouthWall Facing Left
                case 0 -> new Vector3i(0, -Distance, 0); //SouthWall Facing Down

                case 15 -> new Vector3i(-Distance, 0, 0); //Facing West Upright
                case 12 -> new Vector3i(0, 0, Distance); //Facing South Upright
                case 13 -> new Vector3i(Distance, 0, 0); //Facing East Upright
                case 14 -> new Vector3i(0, 0, -Distance); //Facing North Upright

                case 4 -> new Vector3i(0, 0, -Distance); //Facing North UpsideDown
                case 5 -> new Vector3i(-Distance, 0, 0); //Facing West UpsideDow
                case 6 -> new Vector3i(0, 0, Distance); //Facing South UpsideDown
                case 7 -> new Vector3i(Distance, 0, 0); //Facing East UpsideDown

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        } else {
            return switch (RotIndex) {
                case 0 -> new Vector3i(0, 0, -Distance); //Facing North Upright
                case 1 -> new Vector3i(-Distance, 0, 0); //Facing West Upright
                case 2 -> new Vector3i(0, 0, Distance); //Facing South Upright
                case 3 -> new Vector3i(Distance, 0, 0); //Facing East Upright

                case 4 -> new Vector3i(0, Distance, 0); //DoublePipe SouthWall extra for pullers and rotators
                case 5 -> new Vector3i(0, Distance, 0); //DoublePipe EastWall extra for pullers and rotators
                case 6 -> new Vector3i(0, Distance, 0); //DoublePipe NorthWall extra for pullers and rotators
                case 7 -> new Vector3i(0, Distance, 0); //DoublePipe WestWall extra for pullers and rotators

                case 8 -> new Vector3i(0, 0, Distance); //Facing South UpsideDown
                case 9 -> new Vector3i(Distance, 0, 0); //Facing East UpsideDown
                case 10 -> new Vector3i(0, 0, -Distance); //Facing North UpsideDown
                case 11 -> new Vector3i(-Distance, 0, 0); //Facing West UpsideDown

                case 12 -> new Vector3i(0, -Distance, 0);
                case 13 -> new Vector3i(0, -Distance, 0);
                case 14 -> new Vector3i(0, -Distance, 0);
                case 15 -> new Vector3i(0, -Distance, 0);

                case 24 -> new Vector3i(0, 0, Distance);
                case 25 -> new Vector3i(Distance, 0, 0);
                case 27 -> new Vector3i(-Distance, 0, 0);

                case 49 -> new Vector3i(-Distance, 0, 0); //Facing West LayingOnRightSide
                case 50 -> new Vector3i(0, 0, Distance); //Facing South LayingOnRightSide
                case 26 -> new Vector3i(0, 0, -Distance); //Facing North LayingOnRightSide
                case 51 -> new Vector3i(Distance, 0, 0); //Facing East LayingOnRightSide

                case 16 -> new Vector3i(0, 0, -Distance); //Facing North LayingOnLeftSide
                case 17 -> new Vector3i(-Distance, 0, 0); //Facing West LayingOnLeftSide
                case 18 -> new Vector3i(0, 0, Distance); //Facing South LayingOnRightSide
                case 19 -> new Vector3i(Distance, 0, 0); //Facing East LayingOnLeftSide

                default -> new Vector3i(0, 0, 0); // extend if needed
            };
        }
    }
}

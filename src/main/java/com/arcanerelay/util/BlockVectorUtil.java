package com.arcanerelay.util;

import org.joml.Vector3i;
import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.ArcaneRelayConfig;
import com.hypixel.hytale.math.Axis;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Arrays;

public class BlockVectorUtil {
    private static ArcaneRelayConfig config = ArcaneRelayPlugin.get().getConfig();

    private static List<String> NoneMoveableIDs = Arrays.asList(config.getNoneMoveableBlocks());

    private static List<String> NoneRotatableIDs = Arrays.asList(config.getNoneRotatableBlocks());

    public static void setTickingAround(@Nonnull WorldChunk chnk, Vector3i pos, int range) {
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    chnk.setTicking(pos.x + x, pos.y + y, pos.z + z, true);
                }
            }
        }
    }

    private static boolean isExtensionBlock(@Nullable BlockType blockType) {
        if (blockType == null)
            return false;

        String id = blockType.getId();
        if (id == null)
            return false;

        String lower = id.toLowerCase();
        return lower.contains("puller") && lower.contains("extension");
    }

    public static boolean isEmpty(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0)
            return false;
        return isEmpty(blockType);
    }

    public static boolean isEmpty(@Nullable BlockType blockType) {
        if (blockType == null)
            return true;
        return blockType.getMaterial() == BlockMaterial.Empty;
    }

    public static boolean isPullable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0)
            return false;
        return isPullable(blockType);
    }

    public static boolean isPullable(@Nullable BlockType blockType) {
        if (blockType == null)
            return false;
        String id = blockType.getId();

        if (isExtensionBlock(blockType))
            return false;
        for (String keyword : NoneMoveableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }

        return blockType.getMaterial() == BlockMaterial.Solid;
    }

    public static boolean isMoveable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0)
            return false;
        return isMoveable(blockType);
    }

    public static boolean isMoveable(@Nullable BlockType blockType) {
        if (blockType == null)
            return false;
        String id = blockType.getId();

        for (String keyword : NoneMoveableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }

        return blockType.getMaterial() != BlockMaterial.Empty;
    }

    public static boolean isRotatable(@Nullable BlockType blockType, int blockID) {
        if (blockID == 0)
            return false;
        return isRotatable(blockType);
    }

    public static boolean isRotatable(@Nullable BlockType blockType) {
        if (blockType == null)
            return false;
        String id = blockType.getId();

        for (String keyword : NoneRotatableIDs) {
            if (id.contains(keyword)) {
                return false;
            }
        }

        return blockType.getMaterial() != BlockMaterial.Empty;
    }

    public static Vector3i getUpVector(@Nonnull WorldChunk chnk, Vector3i sourcePos) {
        return getUpVector(chnk, sourcePos, false, 1);
    }

    public static Vector3i getUpVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, int distance) {
        return getUpVector(chnk, sourcePos, false, distance);
    }

    public static Vector3i getUpVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, boolean isWallPusher) {
        return getUpVector(chnk, sourcePos, isWallPusher, 1);
    }

    public static Vector3i getUpVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, boolean isWallPusher, int distance) {
        int RotIndex = chnk.getRotationIndex(sourcePos.x, sourcePos.y, sourcePos.z);
        RotationTuple blockRotation = RotationTuple.get(RotIndex);

        Vector3i localUp = isWallPusher ? new Vector3i(0, 0, 1) : new Vector3i(0, 1, 0);
        Vector3i resultVector = applyRotationToVector(localUp, blockRotation);

        return resultVector.mul(distance);
    }

    public static Vector3i getForwardVector(@Nonnull WorldChunk chnk, Vector3i sourcePos) {
        return getForwardVector(chnk, sourcePos, false, 1);
    }

    public static Vector3i getForwardVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, int distance) {
        return getForwardVector(chnk, sourcePos, false, distance);
    }

    public static Vector3i getForwardVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, boolean isWallPusher) {
        return getForwardVector(chnk, sourcePos, isWallPusher, 1);
    }

    public static Vector3i getForwardVector(@Nonnull WorldChunk chnk, Vector3i sourcePos, boolean isWallPusher, int distance) {
        int RotIndex = chnk.getRotationIndex(sourcePos.x, sourcePos.y, sourcePos.z);
        RotationTuple blockRotation = RotationTuple.get(RotIndex);

        Vector3i localForward = isWallPusher ? new Vector3i(0, -1, 0) : new Vector3i(0, 0, -1);
        Vector3i resultVector = applyRotationToVector(localForward, blockRotation);

        return resultVector.mul(distance);
    }

    /**
     * Had to apply in custom multiplication order
     * Using default RotationTuple.rotatedVector produced incorrect results for wall pushers in certain orientations.
     * Likely due to non-commutative rotations and how they are applied in the game engine
     */
    private static Vector3i applyRotationToVector(Vector3i vector, RotationTuple rotation) {
        if (rotation == null) {
            return new Vector3i(vector.x, vector.y, vector.z);
        }

        Rotation roll = rotation.roll();
        Rotation pitch = rotation.pitch();
        Rotation yaw = rotation.yaw();

        roll = (roll == null) ? Rotation.None : roll;
        pitch = (pitch == null) ? Rotation.None : pitch;
        yaw = (yaw == null) ? Rotation.None : yaw;

        Vector3d vec = new Vector3d(vector.x, vector.y, vector.z);

        roll.rotateZ(vec, vec);
        pitch.rotateX(vec, vec);
        yaw.rotateY(vec, vec);

        return new Vector3i((int) Math.round(vec.x), (int) Math.round(vec.y), (int) Math.round(vec.z));
    }

    public static RotationTuple rotateOverAxis90Degrees(RotationTuple currentRotation, Vector3i rotationAxis, boolean clockwise) {
        Axis axis = getAxisFromVector(rotationAxis);
        if (axis == null) return currentRotation;

        boolean isNegativeAxis = (rotationAxis.x < 0 || rotationAxis.y < 0 || rotationAxis.z < 0);
        
        Rotation addedRotation;
        if (clockwise) {
            addedRotation = isNegativeAxis ? Rotation.Ninety : Rotation.TwoSeventy;
        } else {
            addedRotation = isNegativeAxis ? Rotation.TwoSeventy : Rotation.Ninety;
        }
       
        Rotation roll = (currentRotation.roll() == null) ? Rotation.None : currentRotation.roll();
        Rotation pitch = (currentRotation.pitch() == null) ? Rotation.None : currentRotation.pitch();
        Rotation yaw = (currentRotation.yaw() == null) ? Rotation.None : currentRotation.yaw();

        RotationTuple result = RotationTuple.of(Rotation.None, Rotation.None, roll);
        result = result.composeOnAxis(Axis.X, pitch);
        result = result.composeOnAxis(Axis.Y, yaw);

        return result.composeOnAxis(axis, addedRotation);
    }

     /**
     * Determines which global axis (X, Y, or Z) a vector primarily points along.
     * Returns the axis with the largest absolute component.
     */
    @Nullable
    private static Axis getAxisFromVector(@Nonnull Vector3i vector) {
        double x = Math.abs(vector.x);
        double y = Math.abs(vector.y);
        double z = Math.abs(vector.z);
        
        if (x > y && x > z) {
            return Axis.X;
        } else if (y > x && y > z) {
            return Axis.Y;
        } else if (z > x && z > y) {
            return Axis.Z;
        }
        
        return null;
    }
}
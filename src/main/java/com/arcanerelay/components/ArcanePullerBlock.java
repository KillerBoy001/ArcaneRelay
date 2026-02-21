package com.arcanerelay.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ArcanePullerBlock implements Component<ChunkStore> {
    /** Indices along the chain (0 = in front of puller). Used for pull-back ordering. */
    private List<Integer> extensionPositions = new ArrayList<>();
    private String extensionBlockKey = "";

    /** Phase of the puller state machine. */
    public enum Phase {
        IDLE,
        EXTENDING,
        PULLING_BACK
    }

    private Phase phase = Phase.IDLE;
    /** When PULLING_BACK: block we hit and are dragging back. */
    private int pulledBlockId;
    private int pulledBlockRotation;
    private int pulledBlockFiller;
    @Nullable
    private Holder<ChunkStore> pulledBlockHolder;
    /** Hit position (world) when we collided with a block. */
    private int pulledFromX, pulledFromY, pulledFromZ;

    @Nonnull
    public static final BuilderCodec<ArcanePullerBlock> CODEC = BuilderCodec.builder(ArcanePullerBlock.class, ArcanePullerBlock::new)
        .append(new KeyedCodec<>("ExtensionPositions", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (o, v) -> o.extensionPositions = new ArrayList<>(Arrays.asList(v)),
            o -> o.extensionPositions.toArray(Integer[]::new))
        .add()
        .append(new KeyedCodec<>("ExtensionBlockKey", Codec.STRING), (o, v) -> o.extensionBlockKey = v, o -> o.extensionBlockKey)
        .add()
        .build();

    @Override
    public Component<ChunkStore> clone() {
        ArcanePullerBlock clone = new ArcanePullerBlock();
        clone.extensionPositions = new ArrayList<>(extensionPositions);
        clone.extensionBlockKey = extensionBlockKey;
        clone.phase = phase;
        clone.pulledBlockId = pulledBlockId;
        clone.pulledBlockRotation = pulledBlockRotation;
        clone.pulledBlockFiller = pulledBlockFiller;
        clone.pulledBlockHolder = pulledBlockHolder != null ? pulledBlockHolder.clone() : null;
        clone.pulledFromX = pulledFromX;
        clone.pulledFromY = pulledFromY;
        clone.pulledFromZ = pulledFromZ;
        return clone;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        ArcaneRelayPlugin.LOGGER.atInfo().log("Setting phase to %s for block %d", phase, this.getExtensionLength());
        this.phase = phase;
    }

    public int getExtensionLength() {
        return extensionPositions.size();
    }

    public void addExtensionPosition(int index) {
        extensionPositions.add(index);
    }

    public void clearExtensionPositions() {
        extensionPositions.clear();
    }

    /** Remove the last extension index (furthest from puller). */
    public boolean removeLastExtensionPosition() {
        if (extensionPositions.isEmpty()) return false;
        extensionPositions.remove(extensionPositions.size() - 1);
        return true;
    }

    /** Store the block we hit for pull-back. */
    public void setPulledBlock(int blockId, int rotation, int filler, @Nullable Holder<ChunkStore> holder, int worldX, int worldY, int worldZ) {
        this.pulledBlockId = blockId;
        this.pulledBlockRotation = rotation;
        this.pulledBlockFiller = filler;
        this.pulledBlockHolder = holder != null ? holder.clone() : null;
        this.pulledFromX = worldX;
        this.pulledFromY = worldY;
        this.pulledFromZ = worldZ;
    }

    /** Update the stored pulled block position after a step. */
    public void updatePulledFrom(int worldX, int worldY, int worldZ) {
        this.pulledFromX = worldX;
        this.pulledFromY = worldY;
        this.pulledFromZ = worldZ;
    }

    /** Clear stored pulled block info after completion. */
    public void clearPulledBlock() {
        this.pulledBlockId = 0;
        this.pulledBlockRotation = 0;
        this.pulledBlockFiller = 0;
        this.pulledBlockHolder = null;
        this.pulledFromX = 0;
        this.pulledFromY = 0;
        this.pulledFromZ = 0;
    }

    public int getPulledBlockId() { return pulledBlockId; }
    public int getPulledBlockRotation() { return pulledBlockRotation; }
    public int getPulledBlockFiller() { return pulledBlockFiller; }
    @Nullable public Holder<ChunkStore> getPulledBlockHolder() { return pulledBlockHolder; }
    public int getPulledFromX() { return pulledFromX; }
    public int getPulledFromY() { return pulledFromY; }
    public int getPulledFromZ() { return pulledFromZ; }

    public String getExtensionBlockKey() {
        return extensionBlockKey;
    }

    private static boolean isEmpty(@Nullable BlockType blockType, int blockId) {
        if (blockId == 0) return true;
        if (blockType == null) return true;
        return blockType.getMaterial() == BlockMaterial.Empty;
    }

    /**
     * Attempt to place an extension block at the end of the chain.
     * Uses isEmpty as "test place" validation.
     * @return true if placement succeeded.
     */
    public boolean extend(World world, int pullerX, int pullerY, int pullerZ, @Nonnull BlockType pullerBlockType, int rotationIndex) {
        if (extensionBlockKey == null || extensionBlockKey.isEmpty()) return false;

        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
        BlockType extensionBlockType = assetMap.getAsset(extensionBlockKey);
        if (extensionBlockType == null) return false;

        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        Vector3d localForward = getLocalForward(pullerBlockType);
        Vector3d rotatedForward = rotationTuple.rotate(localForward);
        Vector3i forward = new Vector3i(
            (int) Math.round(rotatedForward.getX()),
            (int) Math.round(rotatedForward.getY()),
            (int) Math.round(rotatedForward.getZ())
        );

        int chainLen = this.extensionPositions.size();
        int posX = pullerX + forward.x * (chainLen + 1);
        int posY = pullerY + forward.y * (chainLen + 1);
        int posZ = pullerZ + forward.z * (chainLen + 1);

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(posX, posZ));
        if (chunk == null) return false;

        int existingId = chunk.getBlock(posX, posY, posZ);
        BlockType existingType = assetMap.getAsset(existingId);
        if (!isEmpty(existingType, existingId)) return false;

        int extensionBlockID = extensionBlockType.DEBUG_MODEL_ID;
        // int extensionBlockIndex = BlockType.getAssetMap().getIndex(extensionBlockType.getId());
        chunk.setBlock(posX, posY, posZ, extensionBlockID, extensionBlockType, rotationIndex, 0, 4);
        this.extensionPositions.add(existingId);
        return true;
    }

    private static Vector3d getLocalForward(BlockType blockType) {
        return switch (blockType.getVariantRotation()) {
            case UpDown -> new Vector3d(0, 1, 0);
            case Wall -> new Vector3d(0, -1, 0);
            default -> new Vector3d(0, 0, -1);
        };
    }
}


// Puller -> ArcanePullerBlock, keeps track of placed extensions.
    // we might want to simply use an entity group :D, i think its more efficient (to group multuple blcoks/components)
// Extension -> ArcanePullerExtensionBlock
    // have a system that listens for extension blocks break (from an external system (like a player or a pusher)) as if this happens we want to halt

// Activation -> ArcanePullerActivation, 
// Check phase/state of ArcanePullerBlock (IDLE, EXTENDING, PULLING_BACK)
// Execute pull-back or extension logic based on phase, validate action
    // when extending, we are placing an extension block.
    // when pulling back, we are (removing an extension block) and enqueing a move entry for the pulled block.
// If we have not finished pulling or extending, continue to next tick


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
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.connectedblocks.ConnectedBlocksUtil;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ArcanePullerBlock implements Component<ChunkStore> {
    /** Indices along the chain (0 = in front of puller). Used for pull-back ordering. */
    private List<Integer> extensionPositions = new ArrayList<>();
    private String extensionBlockKey = "";
    private boolean pullingTarget = false;

    /** Phase of the puller state machine. */
    public enum Phase {
        IDLE,
        EXTENDING,
        PULLING_BACK
    }

    private Phase phase = Phase.IDLE;
    @Nonnull
    public static final BuilderCodec<ArcanePullerBlock> CODEC = BuilderCodec.builder(ArcanePullerBlock.class, ArcanePullerBlock::new)
        .append(new KeyedCodec<>("ExtensionPositions", new ArrayCodec<>(Codec.INTEGER, Integer[]::new)),
            (o, v) -> o.extensionPositions = new ArrayList<>(Arrays.asList(v)),
            o -> o.extensionPositions.toArray(Integer[]::new))
        .add()
        .append(new KeyedCodec<>("ExtensionBlockKey", Codec.STRING), (o, v) -> o.extensionBlockKey = v, o -> o.extensionBlockKey)
        .add()
        .append(new KeyedCodec<>("PullingTarget", Codec.BOOLEAN, true), (o, v) -> o.pullingTarget = v, o -> o.pullingTarget)
        .add()
        .build();

    @Override
    public Component<ChunkStore> clone() {
        ArcanePullerBlock clone = new ArcanePullerBlock();
        clone.extensionPositions = new ArrayList<>(extensionPositions);
        clone.extensionBlockKey = extensionBlockKey;
        clone.phase = phase;
        return clone;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        ArcaneRelayPlugin.LOGGER.atInfo().log("Setting phase to %s", phase);
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

    public String getExtensionBlockKey() {
        return extensionBlockKey;
    }

    public boolean isPullingTarget() {
        return pullingTarget;
    }

    public void setPullingTarget(boolean pullingTarget) {
        this.pullingTarget = pullingTarget;
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
    public boolean extend(@Nonnull World world,
        int pullerX, int pullerY, int pullerZ, @Nonnull BlockType pullerBlockType, int rotationIndex) {
        if (extensionBlockKey == null || extensionBlockKey.isEmpty()) return false;

        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
        BlockType extensionBlockType = assetMap.getAsset(extensionBlockKey);
        if (extensionBlockType == null) return false;

        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        Vector3d localUp = getLocalUpVector(pullerBlockType);
        Vector3d rotatedUp = rotationTuple.rotate(localUp);
        Vector3i up = new Vector3i(
            (int) Math.round(rotatedUp.getX()),
            (int) Math.round(rotatedUp.getY()),
            (int) Math.round(rotatedUp.getZ())
        );

        int chainLen = this.extensionPositions.size();
        int posX = pullerX + up.x * (chainLen + 1);
        int posY = pullerY + up.y * (chainLen + 1);
        int posZ = pullerZ + up.z * (chainLen + 1);

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(posX, posZ));
        if (chunk == null) return false;

        int existingId = chunk.getBlock(posX, posY, posZ);
        BlockType existingType = assetMap.getAsset(existingId);
        if (!isEmpty(existingType, existingId)) return false;

        int extensionBlockIndex = BlockType.getAssetMap().getIndex(extensionBlockType.getId());
        Store<ChunkStore> store = world.getChunkStore().getStore();
        BlockChunk blockChunk = store.getComponent(chunk.getReference(), BlockChunk.getComponentType());

        int noParticles = 4;
        int applyPhysics = 32;
        int maybeConnectedComponents = 132;
        chunk.placeBlock(posX, posY, posZ, extensionBlockKey, rotationTuple.yaw(), rotationTuple.pitch(), rotationTuple.roll());
        // chunk.setBlock(posX, posY, posZ, extensionBlockIndex, extensionBlockType, rotationIndex, 0, noParticles | applyPhysics | maybeConnectedComponents | 2048);
        // chunk.setTicking(posX, posY, posZ, true);
        // ConnectedBlocksUtil.setConnectedBlockAndNotifyNeighbors(extensionBlockIndex, rotationTuple, up, new Vector3i(posX, posY, posZ), chunk, blockChunk);
        // world.performBlockUpdate(posX, posY, posZ, false);

        if (store != null) {
            if (blockChunk != null) {
                BlockSection blockSection = blockChunk.getSectionAtBlockY(posY);
                if (blockSection != null) {
                    blockSection.setTicking(posX, posY, posZ, true);
                }
            }
        }
        this.extensionPositions.add(chainLen);
        return true;
    }

    private static Vector3d getLocalUpVector(BlockType blockType) {
        return switch (blockType.getVariantRotation()) {
            case UpDown -> new Vector3d(0, 1, 0);
            case Wall -> new Vector3d(0, 0, 1);
            default -> new Vector3d(0, 1, 0);
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


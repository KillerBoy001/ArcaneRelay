package com.arcanerelay.util;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.connectedblocks.ConnectedBlocksUtil;
import com.hypixel.hytale.server.core.universe.world.connectedblocks.ConnectedBlocksUtil.ConnectedBlockResult;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class ArcaneConnectedBlocksUtil {
    private static final int SETTINGS = 132;

    private ArcaneConnectedBlocksUtil() { }

    /**
     * Updates connected-block state for the current block and up to 2 blocks behind it
     * in the extending direction. Preserves block state (holder) if a block type swap occurs.
     */
    public static void updateCurrentAndPrevious(
        @Nonnull Store<ChunkStore> store,
        @Nonnull World world,
        @Nonnull Vector3i blockPos,
        @Nonnull Vector3i extendDir,
        @Nonnull RotationTuple rotation
    ) {
        updateSingle(store, world, blockPos, extendDir, rotation);
        Vector3i back = extendDir.clone().scale(-1);
        Vector3i prev = blockPos.clone().add(back);
        updateSingle(store, world, prev, extendDir, rotation);
        prev.add(back);
        updateSingle(store, world, prev, extendDir, rotation);
        // update 1 forward
        Vector3i forward = extendDir.clone();
        Vector3i next = blockPos.clone().add(forward);
        updateSingle(store, world, next, extendDir, rotation);

    }

    private static void updateSingle(
        @Nonnull Store<ChunkStore> store,
        @Nonnull World world,
        @Nonnull Vector3i blockPos,
        @Nonnull Vector3i extendDir,
        @Nonnull RotationTuple rotation
    ) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
        if (chunk == null) return;

        BlockChunk blockChunk = store.getComponent(chunk.getReference(), BlockChunk.getComponentType());
        if (blockChunk == null) return;
        BlockSection section = blockChunk.getSectionAtBlockY(blockPos.y);
        if (section == null) return;

        int filler = section.getFiller(blockPos.x, blockPos.y, blockPos.z);
        if (filler != 0) return;

        int blockId = section.get(blockPos.x, blockPos.y, blockPos.z);
        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null || blockType.getConnectedBlockRuleSet() == null) return;

        int rotationIndex = rotation.index();
        Optional<ConnectedBlockResult> desired = ConnectedBlocksUtil.getDesiredConnectedBlockType(
            world,
            new Vector3i(blockPos),
            blockType,
            rotationIndex,
            extendDir,
            true);
        if (desired.isEmpty()) return;

        ConnectedBlockResult result = desired.get();
        if (result.blockTypeKey().equals(blockType.getId()) && result.rotationIndex() == rotationIndex) return;

        int newId = BlockType.getAssetMap().getIndex(result.blockTypeKey());
        BlockType newType = BlockType.getAssetMap().getAsset(newId);
        Holder<ChunkStore> holder = chunk.getBlockComponentHolder(blockPos.x, blockPos.y, blockPos.z);
        chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, newId, newType, result.rotationIndex(), 0, SETTINGS);
        if (holder != null) {
            chunk.setState(blockPos.x, blockPos.y, blockPos.z, holder);
        }
        world.performBlockUpdate(blockPos.x, blockPos.y, blockPos.z, true);
    }
}

package com.arcanerelay.config.types;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcanePullerBlock;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ActivationExecutor;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.activation.ChunkStoreCommandBufferLike;
import com.arcanerelay.core.activation.EntityStoreChunkStoreAdapter;
import com.arcanerelay.util.ArcaneUtil;
import com.arcanerelay.resources.ArcaneMoveState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArcanePullerActivation extends Activation {
    private int range = 15;
    private static final double KNOCKBACK_MAX_SPEED = 4.5;
    private static final float KNOCKBACK_DURATION = 0.2f;
    private static final float KNOCKBACK_MIN_DURATION = 0.05f;

    public static final BuilderCodec<ArcanePullerActivation> CODEC = BuilderCodec.builder(
        ArcanePullerActivation.class,
        ArcanePullerActivation::new,
        Activation.ABSTRACT_CODEC)
        .documentation("Pulls blocks and entities toward the puller. Extends a chain, collides with blocks/entities, then pulls back.")
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

    /** Per-tick behavior for active pullers. */
    public static ArcaneSection.BlockTickStrategy tickPuller(
        @Nonnull ChunkStoreCommandBufferLike commandBuffer,
        @Nonnull World world,
        @Nonnull WorldChunk worldChunkComponent,
        @Nonnull BlockType pullerBlockType,
        @Nonnull ArcanePullerBlock puller,
        int worldX,
        int worldY,
        int worldZ
    ) {
        Vector3i pullerPos = new Vector3i(worldX, worldY, worldZ);
        Vector3i globalForward = getGlobalForward(worldChunkComponent, pullerBlockType, pullerPos);
        if (globalForward.length() == 0) return ArcaneSection.BlockTickStrategy.PROCESSED;

        // syncExtensionChain(world, puller, pullerPos, globalForward, maxRange);

        Activation activation = ArcaneUtil.getActivationForBlock(pullerBlockType);
        int maxRange = activation instanceof ArcanePullerActivation pullerActivation
            ? Math.max(1, pullerActivation.getRange())
            : 15;

        if (puller.getPhase() == ArcanePullerBlock.Phase.EXTENDING) {
            return handleExtending(commandBuffer, world, puller, pullerPos, worldChunkComponent, globalForward, pullerBlockType, maxRange, activation);
        }

        if (puller.getPhase() == ArcanePullerBlock.Phase.PULLING_BACK) {
            return handlePullingBack(commandBuffer, world, puller, pullerPos, globalForward, activation);
        }

        return ArcaneSection.BlockTickStrategy.PROCESSED;
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
        ArcaneRelayPlugin.LOGGER.atInfo().log("Executing ArcanePullerActivation at %d,%d,%d", worldX, worldY, worldZ);

        if (blockRef == null || !blockRef.isValid()) {
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        ArcanePullerBlock puller = commandBuffer.ensureAndGetComponent(blockRef, ArcaneRelayPlugin.get().getArcanePullerBlockComponentType());

        World world = commandBuffer.getExternalData().getWorld();
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
        if (chunk == null) return ArcaneSection.BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;

        BlockType pullerBlockType = chunk.getBlockType(worldX, worldY, worldZ);
        if (pullerBlockType == null) return ArcaneSection.BlockTickStrategy.PROCESSED;

        if (puller.getPhase() != ArcanePullerBlock.Phase.IDLE) {
            ArcaneUtil.clearTicking(commandBuffer, worldX, worldY, worldZ);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        puller.clearExtensionPositions();
        puller.setPhase(ArcanePullerBlock.Phase.EXTENDING);
        setBlockTicking(commandBuffer, world, worldX, worldY, worldZ, true);
        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    private static ArcaneSection.BlockTickStrategy handleExtending(
        ChunkStoreCommandBufferLike commandBuffer,
        World world,
        ArcanePullerBlock puller,
        Vector3i pullerPos,
        WorldChunk pullerChunk,
        Vector3i globalForward,
        BlockType pullerBlockType,
        int maxRange,
        Activation activation
    ) {
        int extLen = puller.getExtensionLength();
        int maxExtend = Math.max(0, maxRange - 1);

        int tipX = pullerPos.x + globalForward.x * (extLen + 1);
        int tipY = pullerPos.y + globalForward.y * (extLen + 1);
        int tipZ = pullerPos.z + globalForward.z * (extLen + 1);

        WorldChunk tipChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(tipX, tipZ));
        if (tipChunk == null) return ArcaneSection.BlockTickStrategy.CONTINUE;

        int tipBlockId = tipChunk.getBlock(tipX, tipY, tipZ);
        BlockType tipBlockType = BlockType.getAssetMap().getAsset(tipBlockId);

        if (isPullable(tipBlockType, tipBlockId)) {
             if (extLen == 0) {
                puller.setPhase(ArcanePullerBlock.Phase.IDLE);
                puller.clearExtensionPositions();
                setBlockTicking(commandBuffer, world, pullerPos.x, pullerPos.y, pullerPos.z, false);
                return ArcaneSection.BlockTickStrategy.PROCESSED;
             }


            commandBuffer.run((Store<ChunkStore> s) -> {
                puller.setPhase(ArcanePullerBlock.Phase.PULLING_BACK);
                ActivationExecutor.playEffects(world, tipX, tipY, tipZ,
                    activation != null ? activation.getEffects() : null);
            });
          
            ArcaneRelayPlugin.LOGGER.atInfo().log(
                "Puller hit block %d at %d,%d,%d; starting pull-back",
                tipBlockId, tipX, tipY, tipZ);
            return ArcaneSection.BlockTickStrategy.CONTINUE;
        }

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        if (entityStore != null) {
            Set<Ref<EntityStore>> entitiesInTip = new HashSet<>();
            collectEntitiesInBlock(entityStore, new Vector3i(tipX, tipY, tipZ), entitiesInTip);
            if (!entitiesInTip.isEmpty()) {
                Vector3i knockbackDir = globalForward.clone().scale(-1);
                Vector3i targetPos = new Vector3i(
                    pullerPos.x + globalForward.x,
                    pullerPos.y + globalForward.y,
                    pullerPos.z + globalForward.z
                );
                for (Ref<EntityStore> ref : entitiesInTip) {
                    if (ref != null && ref.isValid()) {
                        float duration = computePullDuration(entityStore, ref, targetPos);
                        applyKnockbackToward(entityStore, ref, knockbackDir, duration);
                    }
                }
                ActivationExecutor.playEffects(world, pullerPos.x, pullerPos.y, pullerPos.z,
                    activation != null ? activation.getEffects() : null);
                ArcaneRelayPlugin.LOGGER.atInfo().log(
                    "Puller hit %d entities at %d,%d,%d; applied knockback",
                    entitiesInTip.size(), tipX, tipY, tipZ);
                puller.setPhase(ArcanePullerBlock.Phase.PULLING_BACK);
                setBlockTicking(commandBuffer, world, pullerPos.x, pullerPos.y, pullerPos.z, true);
                return ArcaneSection.BlockTickStrategy.CONTINUE;
            }
        }

        if (isEmpty(tipBlockType, tipBlockId)) {
            if (extLen >= maxExtend) {
                puller.setPhase(ArcanePullerBlock.Phase.PULLING_BACK);
                return ArcaneSection.BlockTickStrategy.CONTINUE;
            }
            commandBuffer.run((Store<ChunkStore> s) -> {
                boolean placed = puller.extend(world, pullerPos.x, pullerPos.y, pullerPos.z,
                    pullerBlockType, pullerChunk.getRotationIndex(pullerPos.x, pullerPos.y, pullerPos.z));
                if (placed) {
                    ActivationExecutor.playEffects(world, tipX, tipY, tipZ,
                        activation != null ? activation.getEffects() : null);
                    ArcaneRelayPlugin.LOGGER.atInfo().log(
                        "Puller extended to %d,%d,%d (len=%d)",
                        tipX, tipY, tipZ, puller.getExtensionLength());
                    setBlockTicking(commandBuffer, world, tipX, tipY, tipZ, true);
                }
            });
         
            return ArcaneSection.BlockTickStrategy.CONTINUE;
        }

        if (extLen == 0) { // No extension blocks, so we're done
            puller.setPhase(ArcanePullerBlock.Phase.IDLE);
            setBlockTicking(commandBuffer, world, pullerPos.x, pullerPos.y, pullerPos.z, false);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }
        return ArcaneSection.BlockTickStrategy.CONTINUE;
    }

    private static ArcaneSection.BlockTickStrategy handlePullingBack(
        ChunkStoreCommandBufferLike commandBuffer,
        World world,
        ArcanePullerBlock puller,
        Vector3i pullerPos,
        Vector3i globalForward,
        Activation activation
    ) {
        int extLen = puller.getExtensionLength();
        if (extLen <= 0) {
            puller.setPhase(ArcanePullerBlock.Phase.IDLE);
            puller.clearExtensionPositions();
            setBlockTicking(commandBuffer, world, pullerPos.x, pullerPos.y, pullerPos.z, false);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        Vector3i tipPos = new Vector3i(
            pullerPos.x + globalForward.x * (extLen + 1),
            pullerPos.y + globalForward.y * (extLen + 1),
            pullerPos.z + globalForward.z * (extLen + 1)
        );
        Vector3i lastPos = new Vector3i(
            pullerPos.x + globalForward.x * extLen,
            pullerPos.y + globalForward.y * extLen,
            pullerPos.z + globalForward.z * extLen
        );

        WorldChunk tipChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(tipPos.x, tipPos.z));
        if (tipChunk == null) return ArcaneSection.BlockTickStrategy.CONTINUE;

        int tipBlockId = tipChunk.getBlock(tipPos.x, tipPos.y, tipPos.z);
        BlockType tipBlockType = BlockType.getAssetMap().getAsset(tipBlockId);
        Holder<ChunkStore> holder = tipChunk.getBlockComponentHolder(tipPos.x, tipPos.y, tipPos.z);
        int rotation = tipChunk.getRotationIndex(tipPos.x, tipPos.y, tipPos.z);
        int filler = tipChunk.getFiller(tipPos.x, tipPos.y, tipPos.z);

        commandBuffer.run((Store<ChunkStore> s) -> {
            WorldChunk lastChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(lastPos.x, lastPos.z));
            if (lastChunk != null) {
                lastChunk.breakBlock(lastPos.x, lastPos.y, lastPos.z, lastChunk.getFiller(lastPos.x, lastPos.y, lastPos.z), 4 | 2048);
            }

            if (isPullable(tipBlockType, tipBlockId)) {
                ArcaneMoveState moveState = s.getResource(ArcaneMoveState.getResourceType());
                if (moveState == null) return;
                moveState.addMoveEntry(tipPos, globalForward.clone().scale(-1), tipBlockType, tipBlockId,
                    rotation, filler, 0, holder);
            }
        });

        puller.removeLastExtensionPosition();
        if (puller.getExtensionLength() == 0) {
            puller.setPhase(ArcanePullerBlock.Phase.IDLE);
            puller.clearExtensionPositions();
            setBlockTicking(commandBuffer, world, pullerPos.x, pullerPos.y, pullerPos.z, false);
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }
        return ArcaneSection.BlockTickStrategy.CONTINUE;
    }

    public static Vector3i getGlobalForward(WorldChunk chunk, BlockType blockType, Vector3i pullerPos) {
        int rotationIndex = chunk.getRotationIndex(pullerPos.x, pullerPos.y, pullerPos.z);
        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        Vector3d localForward = getLocalForward(blockType);
        Vector3d global = rotationTuple.rotate(localForward);
        return new Vector3i((int) Math.round(global.getX()), (int) Math.round(global.getY()), (int) Math.round(global.getZ()));
    }

    private static Vector3d getLocalForward(BlockType blockType) {
        return switch (blockType.getVariantRotation()) {
            case UpDown -> new Vector3d(0, 1, 0);
            case Wall -> new Vector3d(0, -1, 0);
            default -> new Vector3d(0, 0, -1);
        };
    }

    private static boolean isEmpty(@Nullable BlockType blockType, int blockId) {
        if (blockId == 0) return true;
        if (blockType == null) return true;
        return blockType.getMaterial() == BlockMaterial.Empty;
    }

    private static boolean isPullable(@Nullable BlockType blockType, int blockId) {
        if (blockId == 0) return false;
        if (blockType == null) return false;
        return blockType.getMaterial() == BlockMaterial.Solid;
    }

    public static boolean isExtensionBlock(@Nullable BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId();
        if (id == null) return false;
        String lower = id.toLowerCase();
        return lower.contains("puller") && lower.contains("extension");
    }

    public static ArcaneSection.BlockTickStrategy handleOrphanExtension(
        @Nonnull ChunkStoreCommandBufferLike commandBuffer,
        @Nonnull Store<ChunkStore> store,
        @Nonnull World world,
        @Nonnull WorldChunk worldChunkComponent,
        @Nonnull BlockType extensionBlockType,
        int worldX,
        int worldY,
        int worldZ
    ) {
        Vector3i pos = new Vector3i(worldX, worldY, worldZ);
        Vector3i forward = getGlobalForward(worldChunkComponent, extensionBlockType, pos);
        if (forward.length() == 0) return ArcaneSection.BlockTickStrategy.CONTINUE;

        Vector3i back = forward.clone().scale(-1);
        Vector3i scan = pos.clone();
        for (int i = 0; i < 32; i++) {
            scan.add(back);
            WorldChunk scanChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(scan.x, scan.z));
            if (scanChunk == null) break;

            int scanId = scanChunk.getBlock(scan.x, scan.y, scan.z);
            BlockType scanType = BlockType.getAssetMap().getAsset(scanId);
            if (isExtensionBlock(scanType)) {
                continue;
            }

            Ref<ChunkStore> pullerRef = BlockModule.getBlockEntity(world, scan.x, scan.y, scan.z);
            if (pullerRef != null && pullerRef.isValid()) {
                ArcanePullerBlock puller = store.getComponent(pullerRef, ArcaneRelayPlugin.get().getArcanePullerBlockComponentType());
                if (puller != null) {
                    Vector3i pullerForward = getGlobalForward(scanChunk, scanType, scan);
                    syncExtensionChain(world, puller, scan, pullerForward, 32);
                    puller.setPhase(ArcanePullerBlock.Phase.PULLING_BACK);
                    setBlockTicking(commandBuffer, world, scan.x, scan.y, scan.z, true);
                }
                return ArcaneSection.BlockTickStrategy.CONTINUE;
            }
            break;
        }

        commandBuffer.run((Store<ChunkStore> s) -> {
            WorldChunk extChunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
            if (extChunk == null) return;
            int extBlockId = extChunk.getBlock(worldX, worldY, worldZ);
            BlockType extBlockType = BlockType.getAssetMap().getAsset(extBlockId);
            if (isExtensionBlock(extBlockType)) {
                extChunk.breakBlock(worldX, worldY, worldZ, extChunk.getFiller(worldX, worldY, worldZ), 4 | 2048);
            }
        });
        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    private static void syncExtensionChain(World world, ArcanePullerBlock puller, Vector3i pullerPos, Vector3i forward, int maxRange) {
        int actualLen = computeActualExtensionLength(world, pullerPos, forward, puller, maxRange);
        if (actualLen == puller.getExtensionLength()) return;

        puller.clearExtensionPositions();
        for (int i = 0; i < actualLen; i++) {
            puller.addExtensionPosition(i);
        }
        if (puller.getPhase() == ArcanePullerBlock.Phase.EXTENDING) {
            puller.setPhase(ArcanePullerBlock.Phase.PULLING_BACK);
        }
    }

    private static int computeActualExtensionLength(World world, Vector3i pullerPos, Vector3i forward, ArcanePullerBlock puller, int maxRange) {
        BlockType expectedType = BlockType.getAssetMap().getAsset(puller.getExtensionBlockKey());
        if (expectedType == null) return 0;

        int length = 0;
        int limit = Math.max(0, maxRange - 1);
        for (int i = 1; i <= limit; i++) {
            int x = pullerPos.x + forward.x * i;
            int y = pullerPos.y + forward.y * i;
            int z = pullerPos.z + forward.z * i;
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) break;
            int blockId = chunk.getBlock(x, y, z);
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType == null || !expectedType.getId().equals(blockType.getId())) break;
            length++;
        }
        return length;
    }

    private static void setBlockTicking(
        ChunkStoreCommandBufferLike commandBuffer,
        World world,
        int worldX,
        int worldY,
        int worldZ,
        boolean ticking
    ) {
        commandBuffer.run((Store<ChunkStore> s) -> {
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));
            if (chunk == null) return;
            BlockChunk blockChunk = s.getComponent(chunk.getReference(), BlockChunk.getComponentType());
            if (blockChunk == null) return;
            BlockSection section = blockChunk.getSectionAtBlockY(worldY);
            if (section == null) return;
            section.setTicking(worldX, worldY, worldZ, ticking);
        });
    }

    private static void applyKnockbackToward(Store<EntityStore> entityStore, Ref<EntityStore> ref, Vector3i direction, float duration) {
        Vector3d velocity = new Vector3d(direction.clone().normalize());
        velocity.scale(KNOCKBACK_MAX_SPEED);
        KnockbackComponent knockback = entityStore.ensureAndGetComponent(ref, KnockbackComponent.getComponentType());
        knockback.setVelocity(velocity);
        knockback.setVelocityType(ChangeVelocityType.Set);
        knockback.setVelocityConfig(new VelocityConfig());
        knockback.setDuration(duration);
    }

    private static float computePullDuration(Store<EntityStore> entityStore, Ref<EntityStore> ref, Vector3i targetPos) {
        TransformComponent transform = entityStore.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return KNOCKBACK_DURATION;
        Vector3d current = transform.getPosition();
        Vector3d target = new Vector3d(targetPos.x + 0.5, targetPos.y + 0.5, targetPos.z + 0.5);
        double distance = current.distanceTo(target);
        float duration = (float) (distance / KNOCKBACK_MAX_SPEED);
        if (duration < KNOCKBACK_MIN_DURATION) duration = KNOCKBACK_MIN_DURATION;
        return duration;
    }

    private static void collectEntitiesInBlock(Store<EntityStore> entityStore, Vector3i blockPos, Set<Ref<EntityStore>> out) {
        Vector3d min = new Vector3d(blockPos.x - 0.1, blockPos.y - 0.1, blockPos.z - 0.1);
        Vector3d max = new Vector3d(blockPos.x + 1.1, blockPos.y + 1.1, blockPos.z + 1.1);
        for (Ref<EntityStore> ref : TargetUtil.getAllEntitiesInBox(min, max, entityStore)) {
            if (ref != null && ref.isValid()) out.add(ref);
        }
    }
}

package com.arcanerelay.config.types;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.components.ArcaneSection.BlockTickStrategy;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.blocktype.component.BlockPhysics;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealth;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthChunk;
import com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

public class HitActivation extends Activation {
    private static final int SUPPRESS_HIT_SFX_FLAG = 1024;
    private static final int DEFAULT_SET_BLOCK_SETTINGS = 4;
    private static final float DEFAULT_APPLY_DAMAGE = 0.3f;

    public float damage;
    public Vector3i direction;
    public ItemTool itemTool;

    public static final BuilderCodec<HitActivation> CODEC = BuilderCodec.builder(
            HitActivation.class,
            HitActivation::new,
            Activation.ABSTRACT_CODEC)
            .documentation(
                    "Hits a target on the specified direction (relative to block orientation) with the given damage.")
            .appendInherited(
                    new KeyedCodec<>("Damage", Codec.FLOAT),
                    (a, r) -> a.damage = r,
                    a -> a.damage,
                    (a, p) -> a.damage = p.damage)
            .documentation("Amount of damage to deal to the block (default: 0).")
            .add()
            .appendInherited(
                    new KeyedCodec<>("RelativeDirection", Vector3i.CODEC),
                    (a, r) -> a.direction = r,
                    a -> a.direction,
                    (a, p) -> a.direction = p.direction)
            .documentation("Relative direction to hit the target in (default: [0, 1, 0]).")
            .add()
            .appendInherited(
                new KeyedCodec<>("ItemTool", ItemTool.CODEC),
                (a, r) -> a.itemTool = r,
                a -> a.itemTool,
                (a, p) -> a.itemTool = p.itemTool)
            .add()
            .build();

    @Override
    public BlockTickStrategy execute(
            @Nonnull ArcaneCachedAccessor accessor,
            @Nullable Ref<ChunkStore> sectionRef,
            @Nullable Ref<ChunkStore> blockRef,
            int worldX, int worldY, int worldZ,
            @Nonnull List<int[]> sources) {
        Vector3i currentPosition = new Vector3i(worldX, worldY, worldZ);
        World world = accessor.getCommandBuffer().getExternalData().getWorld();
        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        WorldChunk worldChunkComponent = (WorldChunk) chunkStore.getStore().getComponent(chunkRef,
            WorldChunk.getComponentType());
        if (worldChunkComponent == null) {
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        BlockType currentBlockType = worldChunkComponent.getBlockType(worldX, worldY,
                worldZ);
        if (currentBlockType == null) {
            return ArcaneSection.BlockTickStrategy.PROCESSED;
        }

        Vector3i globalUp = getGlobalUp(worldChunkComponent, currentBlockType, currentPosition);
        Vector3i targetPosition = currentPosition.clone().add(globalUp);

        EntityStore entityStore = world.getEntityStore();

        world.execute(() -> {
        BlockHarvestUtils.performBlockDamage(
            (LivingEntity)null, (Ref)null, targetPosition, null, itemTool,(String)null, false, .4f, 1024 & 4, chunkRef, entityStore.getStore(), chunkStore.getStore()
         );

        });
        // performBlockDamage(
        //         targetPosition,
        //         SUPPRESS_HIT_SFX_FLAG & DEFAULT_SET_BLOCK_SETTINGS,
        //         chunkRef,
        //         entityStore.getStore(),
        //         accessor.getCommandBuffer());

        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    public boolean performBlockDamage(@Nonnull Vector3i targetBlockPos, int setBlockSettings,
            @Nonnull Ref<ChunkStore> chunkReference, @Nonnull ComponentAccessor<EntityStore> entityStore,
            @Nonnull ComponentAccessor<ChunkStore> chunkStore) {

        World world = ((EntityStore) entityStore.getExternalData()).getWorld();
        WorldChunk worldChunkComponent = (WorldChunk) chunkStore.getComponent(chunkReference,
                WorldChunk.getComponentType());
        if (worldChunkComponent == null) {
            return false;
        }

        BlockChunk blockChunkComponent = (BlockChunk) chunkStore.getComponent(chunkReference,
                BlockChunk.getComponentType());

        assert blockChunkComponent != null;

        BlockSection targetSection = blockChunkComponent.getSectionAtBlockY(targetBlockPos.y);
        int targetRotationIndex = targetSection.getRotationIndex(targetBlockPos.x, targetBlockPos.y,
                targetBlockPos.z);
        boolean brokeBlock = false;
        int environmentId = blockChunkComponent.getEnvironment(targetBlockPos.x, targetBlockPos.y,
                targetBlockPos.z);
        Environment environmentAsset = (Environment) Environment.getAssetMap().getAsset(environmentId);

        if (environmentAsset != null && !environmentAsset.isBlockModificationAllowed()) {
            targetSection.invalidateBlock(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
            return false;
        } else {
            BlockType targetBlockType = worldChunkComponent.getBlockType(targetBlockPos.x, targetBlockPos.y,
                    targetBlockPos.z);
            if (targetBlockType == null) {
                return false;
            } else {
                BlockGathering blockGathering = targetBlockType.getGathering();
                if (blockGathering == null) {
                    return false;
                } else {
                        Vector3d targetBlockCenterPos = new Vector3d();
                        targetBlockType.getBlockCenter(targetRotationIndex, targetBlockCenterPos);
                        targetBlockCenterPos.add(targetBlockPos);
                        Vector3i originBlock = new Vector3i(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
                        if (!targetBlockType.isUnknown()) {
                            int filler = targetSection.getFiller(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
                            int fillerX = FillerBlockUtil.unpackX(filler);
                            int fillerY = FillerBlockUtil.unpackY(filler);
                            int fillerZ = FillerBlockUtil.unpackZ(filler);
                            if (fillerX != 0 || fillerY != 0 || fillerZ != 0) {
                                originBlock = originBlock.clone().subtract(fillerX, fillerY, fillerZ);
                                String oldBlockTypeKey = targetBlockType.getId();
                                targetBlockType = world.getBlockType(originBlock.getX(), originBlock.getY(),
                                        originBlock.getZ());
                                if (targetBlockType == null) {
                                    return false;
                                }

                                if (!oldBlockTypeKey.equals(targetBlockType.getId())) {
                                    worldChunkComponent.breakBlock(targetBlockPos.x, targetBlockPos.y,
                                            targetBlockPos.z);
                                    return true;
                                }

                                blockGathering = targetBlockType.getGathering();
                                if (blockGathering == null) {
                                    return false;
                                }
                            }
                        }

                        float specPower = itemTool != null && itemTool.getSpecs() != null
                                ? itemTool.getSpecs()[0].getPower()
                                : 0.0F;
      
                        int dropQuantity = 1;
                        String itemId = null;
                        String dropListId = null;
                        float applyDamage = DEFAULT_APPLY_DAMAGE;
                        if (specPower != 0.0F) {
                           BlockBreakingDropType breaking = blockGathering.getBreaking();
                           applyDamage = specPower;
                           dropQuantity = breaking.getQuantity();
                           itemId = breaking.getItemId();
                           dropListId = breaking.getDropListId();
                        }
                
                        ChunkColumn chunkColumnComponent = (ChunkColumn) chunkStore.getComponent(chunkReference,
                                ChunkColumn.getComponentType());
                        Ref<ChunkStore> chunkSectionRef = chunkColumnComponent != null
                                ? chunkColumnComponent.getSection(ChunkUtil.chunkCoordinate(targetBlockPos.y))
                                : null;
                        if (targetBlockType.getGathering().shouldUseDefaultDropWhenPlaced()) {
                            BlockPhysics decoBlocks = chunkSectionRef != null
                                    ? (BlockPhysics) chunkStore.getComponent(chunkSectionRef,
                                            BlockPhysics.getComponentType())
                                    : null;
                            boolean isDeco = decoBlocks != null
                                    && decoBlocks.isDeco(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
                            if (isDeco) {
                                itemId = null;
                                dropListId = null;
                            }
                        }

                        TimeResource timeResource = (TimeResource) entityStore
                                .getResource(TimeResource.getResourceType());
                        BlockHealthChunk blockHealthComponent = (BlockHealthChunk) chunkStore.getComponent(
                                chunkReference, BlockHealthModule.get().getBlockHealthChunkComponentType());

                        assert blockHealthComponent != null;

                        float current = blockHealthComponent.getBlockHealth(originBlock);

                        DamageBlockEvent event = new DamageBlockEvent(null, originBlock, targetBlockType, current,
                                applyDamage);
                 
                        entityStore.invoke(event);
                 

                    if (event.isCancelled()) {
                        targetSection.invalidateBlock(targetBlockPos.x, targetBlockPos.y, targetBlockPos.z);
                        return false;
                    } else {
                            targetBlockType = event.getBlockType();
                            targetBlockPos = event.getTargetBlock();
                            targetSection = blockChunkComponent.getSectionAtBlockY(targetBlockPos.y);
                            targetRotationIndex = targetSection.getRotationIndex(targetBlockPos.x, targetBlockPos.y,
                                    targetBlockPos.z);
                            targetBlockType.getBlockCenter(targetRotationIndex, targetBlockCenterPos);
                            targetBlockCenterPos.add(targetBlockPos);
                            BlockHealth blockDamage = blockHealthComponent.damageBlock(timeResource.getNow(), world,
                                    targetBlockPos, damage);
                            if (!blockHealthComponent.isBlockFragile(targetBlockPos) && !blockDamage.isDestroyed()) {
                                if ((setBlockSettings & SUPPRESS_HIT_SFX_FLAG) == 0) { // SFX
                                    BlockSoundSet soundSet = (BlockSoundSet) BlockSoundSet.getAssetMap()
                                            .getAsset(targetBlockType.getBlockSoundSetIndex());
                                    if (soundSet != null) {
                                        int soundEventIndex = soundSet.getSoundEventIndices()
                                                .getOrDefault(BlockSoundEvent.Hit, 0);
                                        if (soundEventIndex != 0) {
                                            SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX,
                                                    targetBlockCenterPos, entityStore);
                                        }
                                    }
                                }

                        } else {
                            Vector3i ctargetBlockPos = targetBlockPos;
                            BlockType ctargetBlockType = targetBlockType;
                            int cdropQuantity = dropQuantity;
                            String cItemId = itemId;
                            String cDropListId = dropListId;
                            world.execute(() -> {
                                BlockHarvestUtils.performBlockBreak(world, ctargetBlockPos, ctargetBlockType,
                                        (ItemStack) null, cdropQuantity, cItemId, cDropListId, setBlockSettings,
                                        null, chunkReference, entityStore, chunkStore);
                            });

                            brokeBlock = true;
                        }

                        return brokeBlock;
                    }
                }
            }
        }
    }

    public Vector3i getGlobalUp(WorldChunk chunk, BlockType blockType, Vector3i blockPos) {
        int rotationIndex = chunk.getRotationIndex(blockPos.x, blockPos.y, blockPos.z);
        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        Vector3d localUp = new Vector3d(this.direction);
        Vector3d global = rotationTuple.rotatedVector(localUp);
        return new Vector3i(
                (int) Math.round(global.getX()),
                (int) Math.round(global.getY()),
                (int) Math.round(global.getZ()));
    }
}

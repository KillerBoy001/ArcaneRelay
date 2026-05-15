package com.arcanerelay.config.types;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.components.ArcaneSection.BlockTickStrategy;
import com.arcanerelay.config.Activation;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.util.BlockFlags;
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
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage.EnvironmentSource;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
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
import com.hypixel.hytale.server.core.util.TargetUtil;

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
        
         hitBlock(accessor, sectionRef, blockRef, worldX, worldY, worldZ, sources);
         hitEntity(accessor, sectionRef, blockRef, worldX, worldY, worldZ, sources);


        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }

    private void hitEntity(@Nonnull ArcaneCachedAccessor accessor,
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
                    return;
                }
        
                BlockType currentBlockType = worldChunkComponent.getBlockType(worldX, worldY,
                        worldZ);
                if (currentBlockType == null) {
                    return;
                }
        
                Vector3i globalUp = getGlobalUp(worldChunkComponent, currentBlockType, currentPosition);
                Vector3i targetBlock = currentPosition.clone().add(globalUp);

                
                List<Ref<EntityStore>> targets = TargetUtil.getAllEntitiesInSphere(targetBlock.toVector3d(),1f, world.getEntityStore().getStore() );

                if (targets.isEmpty()) {
                        return;
                    }
                
                    for (Ref<EntityStore> target : targets) {
                
                        if (target == null || !target.isValid()) {
                            continue;
                        }
                
                        world.execute(() -> {
                            Damage damageEvent = new Damage(
                                new EnvironmentSource("hit_activation"),
                                    DamageCause.PHYSICAL,
                                    this.damage
                            );
                
                            world.getEntityStore().getStore().invoke(target, damageEvent);
                        });
                    }
        }

    private void hitBlock(@Nonnull ArcaneCachedAccessor accessor,
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
            return;
        }

        BlockType currentBlockType = worldChunkComponent.getBlockType(worldX, worldY,
                worldZ);
        if (currentBlockType == null) {
            return;
        }

        Vector3i globalUp = getGlobalUp(worldChunkComponent, currentBlockType, currentPosition);
        Vector3i targetPosition = currentPosition.clone().add(globalUp);
        
        EntityStore entityStore = world.getEntityStore();
        
        BlockFlags damageFlags = new BlockFlags(BlockFlags.BREAK_BLOCK_VFX)
                .add(BlockFlags.BREAK_BLOCK_SFX);

        world.execute(() -> {
                BlockHarvestUtils.performBlockDamage(
                (LivingEntity)null, (Ref<EntityStore>)null, targetPosition, null, itemTool,(String)null, false, .4f, damageFlags.getValue(), chunkRef, entityStore.getStore(), chunkStore.getStore()
                );

        });
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

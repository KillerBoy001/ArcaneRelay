package com.arcanerelay.systems;

import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;
import com.arcanerelay.core.blockmovement.BlockMovementExecutor;
import com.arcanerelay.state.ArcaneMoveState;
import com.arcanerelay.state.ArcaneTickMetricsResource;
import com.arcanerelay.state.ArcaneTickSchedule;
import com.arcanerelay.util.ArcaneUtil;
import com.arcanerelay.config.Activation;
import com.arcanerelay.config.ActivationBinding;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.builtin.blocktick.system.ChunkBlockTickSystem;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;

import javax.annotation.Nonnull;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public class ArcaneSystems {

    
    public static class EnsureArcaneSection extends HolderSystem<ChunkStore> {
        @Nonnull
        private static final Query<ChunkStore> QUERY = Query.and(ChunkSection.getComponentType(), Query.not(ArcaneSection.getComponentType()));

        @Override
        public void onEntityAdd(@Nonnull Holder<ChunkStore> holder, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store) {
            // ArcaneRelayPlugin.LOGGER.atInfo().log("Ensuring ArcaneSection component on chunk store");
            holder.ensureComponent(ArcaneSection.getComponentType());
        }

        @Override
        public void onEntityRemoved(@Nonnull Holder<ChunkStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store) {
             holder.removeComponent(ArcaneSection.getComponentType());
        }

        @Override
        public Query<ChunkStore> getQuery() {
            return QUERY;
        }

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
           return RootDependency.firstSet();
        }
    }

    public static class PreTick extends EntityTickingSystem<ChunkStore> {
        @Nonnull
        private static final Query<ChunkStore> QUERY = Query.and(ChunkSection.getComponentType(), ArcaneSection.getComponentType());
        

        @SuppressWarnings("null")
        @Nonnull
        private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
           new SystemDependency<>(Order.AFTER, ChunkBlockTickSystem.PreTick.class), new SystemDependency<>(Order.BEFORE, ChunkBlockTickSystem.Ticking.class)
        );

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
            return DEPENDENCIES;
        }

        public PreTick() {
        }

        @Nonnull
        @Override
        public Query<ChunkStore> getQuery() {
            return QUERY;
        }

        @Override
        public boolean isParallel(int archetypeChunkSize, int taskCount) {
            return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
        }


        @Override
        public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
            Instant time = commandBuffer.getExternalData().getWorld().getEntityStore().getStore().getResource(WorldTimeResource.getResourceType()).getGameTime();

            Ref<ChunkStore> sectionRef = archetypeChunk.getReferenceTo(index);
            if (sectionRef == null) return;
            
            ArcaneSection arcaneSection = commandBuffer.getComponent(sectionRef, ArcaneSection.getComponentType());
            if (arcaneSection == null) return;

            arcaneSection.preTick(time);
        }
    }

    /**
     * Runs once per world tick; updates {@link ArcaneTickSchedule} so that arcane block processing
     * only runs when the configured interval (e.g. 250ms) has elapsed.
     */
    public static class TickRateLimit extends TickingSystem<ChunkStore> {
        @Nonnull
        private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, ArcaneSystems.PreTick.class),
            new SystemDependency<>(Order.BEFORE, ArcaneSystems.Ticking.class)
        );

        @Override
        public void tick(float dt, int index, @Nonnull Store<ChunkStore> store) {
            var worldTime = store.getExternalData().getWorld().getEntityStore().getStore()
                .getResource(WorldTimeResource.getResourceType());
            if (worldTime == null) return;

            ArcaneTickSchedule schedule = store.getResource(ArcaneTickSchedule.getResourceType());
            if (schedule != null) {
                schedule.update(worldTime.getGameTime());
            }
        }

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
            return DEPENDENCIES;
        }
    }

    public static class Ticking extends EntityTickingSystem<ChunkStore> {
        @Nonnull
        private static final Query<ChunkStore> QUERY = Query.and(ChunkSection.getComponentType(), BlockSection.getComponentType(), ArcaneSection.getComponentType());
        
        @SuppressWarnings("null")
        @Nonnull
        private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, ArcaneSystems.TickRateLimit.class),
            new SystemDependency<>(Order.BEFORE, ChunkBlockTickSystem.Ticking.class)
        );

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
            return DEPENDENCIES;
        }

        public Ticking() {
        }

        /** Single-threaded to avoid deadlock: activations use world/commandBuffer in ways that are not safe from parallel workers. */
        @Override
        public boolean isParallel(int archetypeChunkSize, int taskCount) {
           return false;
        }

        @Override
        public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store,
            @Nonnull CommandBuffer<ChunkStore> commandBuffer
        ) {
            ArcaneTickSchedule schedule = store.getResource(ArcaneTickSchedule.getResourceType());
            if (schedule == null || !schedule.isProcessingAllowed()) {
                return;
            }

            Ref<ChunkStore> sectionRef = archetypeChunk.getReferenceTo(index);

            BlockSection blockSection = commandBuffer.getComponent(sectionRef, BlockSection.getComponentType());
            if (blockSection == null) return;

            ChunkSection chunkSection = commandBuffer.getComponent(sectionRef, ChunkSection.getComponentType());
            if (chunkSection == null) return;

            ArcaneSection arcaneSection = commandBuffer.getComponent(sectionRef, ArcaneSection.getComponentType());
            if (arcaneSection == null) return;

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(chunkSection.getChunkColumnReference(), BlockComponentChunk.getComponentType());
            if (blockComponentChunk == null) return;

            WorldChunk worldChunkComponent = commandBuffer.getComponent(chunkSection.getChunkColumnReference(), WorldChunk.getComponentType());
            if (worldChunkComponent == null) return;

            ArcaneCachedAccessor accessor = ArcaneCachedAccessor.of(
                commandBuffer, 
                arcaneSection, 
                blockSection, 
                chunkSection, 
                1);

            

            int arcaneTicksProcessed = arcaneSection.forEachTicking(accessor, commandBuffer, blockSection, chunkSection.getY(),
                (commandBuffer1, arcaneSection1, x, y, z, blockId) -> {
                    BlockType blockType = accessor.getBlockType(x, y, z);
                    if (blockType == null) return ArcaneSection.BlockTickStrategy.PROCESSED;

                    Activation activation = ArcaneUtil.getActivationForBlock(blockType);
                    if (activation == null) return ArcaneSection.BlockTickStrategy.PROCESSED;

                    // Block Reference may be null, as some blcoks do not have a corresponding entity
                    Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(ChunkUtil.indexBlockInColumn(x, y, z));
                    if (blockRef == null) 
                    {
                        ArcaneRelayPlugin.LOGGER.atWarning().log("Block reference is null for block at " + x + ", " + y + ", " + z + " with block type " + blockType.getId());
                        Holder<ChunkStore> blockEntity = blockType.getBlockEntity();
                        if (blockEntity == null) {
                            // if we have we could create the holder upon adding the output collection
                            ArcaneRelayPlugin.LOGGER.atInfo().log("And no block entity either");
                        }
                    }

                    int sectionStartY = chunkSection.getY() << 5;
                    int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getX(), x);
                    int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getZ(), z);

                    try {
                        ArcaneSection.BlockTickStrategy strategy = activation.execute(
                            accessor, sectionRef, blockRef, worldX, y, worldZ,
                            // TODO: add source position
                            List.of(new int[] { }));
                        return strategy != null ? strategy : ArcaneSection.BlockTickStrategy.PROCESSED;
                    } catch (Throwable t) {
                        ArcaneRelayPlugin.LOGGER.atSevere().withCause(t).log("Activation %s failed at %d,%d,%d",
                            activation.getId(), worldX, y, worldZ);
                        return ArcaneSection.BlockTickStrategy.PROCESSED;
                    }
                });

            ArcaneTickMetricsResource metrics = store.getResource(ArcaneTickMetricsResource.getResourceType());
            if (metrics != null) {
                metrics.recordArcaneTicksProcessed(arcaneTicksProcessed);
            }
        }

        @Nonnull
        @Override
        public Query<ChunkStore> getQuery() {
           return QUERY;
        }
    }

    /**
     * Runs after Ticking to aggregate metrics recorded during the tick (e.g. from Ticking and any
     * future parallel metric producers) and flush them into the historic metric set.
     */
    public static class FlushArcaneMetrics extends TickingSystem<ChunkStore> {
        @SuppressWarnings("null")
        @Nonnull
        private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, ArcaneSystems.Ticking.class),
            new SystemDependency<>(Order.BEFORE, ArcaneSystems.MoveBlock.class)
        );

        @Override
        public void tick(float dt, int index, @Nonnull Store<ChunkStore> store) {
            ArcaneTickMetricsResource metrics = store.getResource(ArcaneTickMetricsResource.getResourceType());
            if (metrics != null) {
                metrics.flushMetrics();
            }
        }

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
            return DEPENDENCIES;
        }
    }

    /**
     * Runs after Ticking to execute block moves queued by MoveBlockActivation.
     * Processes ArcaneMoveState entries via BlockMovementExecutor and clears the state.
     */
    public static class MoveBlock extends TickingSystem<ChunkStore> {
        @Override
        public void tick(
            float dt,
            int index,
            @Nonnull Store<ChunkStore> store
        ) {
            ArcaneMoveState moveState = store.getResource(ArcaneMoveState.getResourceType());
            if (moveState == null) return;

            var entries = moveState.getMoveEntries();
            if (entries.isEmpty()) return;

            var world = store.getExternalData().getWorld();
            if (world == null) return;

            // here it's safe to use world.execute() as we are not using the command buffer because it's running on the main thread
            BlockMovementExecutor.execute(world, entries);
            moveState.clear();
        }
        
        @SuppressWarnings("null")
        @Nonnull
        private static final Set<Dependency<ChunkStore>> DEPENDENCIES = Set.of(
            new SystemDependency<>(Order.AFTER, ArcaneSystems.FlushArcaneMetrics.class),
            new SystemDependency<>(Order.BEFORE, ChunkBlockTickSystem.Ticking.class)
        );

        @Nonnull
        @Override
        public Set<Dependency<ChunkStore>> getDependencies() {
           return DEPENDENCIES;
        }
    }
}

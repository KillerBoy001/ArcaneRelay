package com.arcanerelay.core.activation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.arcanerelay.components.ArcaneSection;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.AbstractCachedAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Cached accessor for activations. Mirrors
 * {@link com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker.CachedAccessor}: inserts
 * section components (ArcaneSection, BlockSection), exposes self* and get*Section.
 */
public final class ArcaneCachedAccessor extends AbstractCachedAccessor implements ArcaneActivationAccessor {

    private static final ThreadLocal<ArcaneCachedAccessor> THREAD_LOCAL =
        ThreadLocal.withInitial(ArcaneCachedAccessor::new);
    private static final int ARCANE_COMPONENT = 0;
    private static final int BLOCK_COMPONENT = 1;
    private static final int CHUNK_COMPONENT = 2;
    protected ArcaneSection selfArcaneSection;
    protected BlockSection selfBlockSection;
    protected ChunkSection selfChunkSection;
    protected CommandBuffer<ChunkStore> selfCommandBuffer;

    ArcaneCachedAccessor() {
        super(3);
    }

    @Nonnull
    public static ArcaneCachedAccessor of(
        @Nonnull ComponentAccessor<ChunkStore> commandBuffer,
        @Nonnull ArcaneSection section,
        @Nonnull BlockSection blockSection, 
        @Nonnull ChunkSection chunkSection,
        int radius
    ) {
        ArcaneCachedAccessor accessor = THREAD_LOCAL.get();
        accessor.init(commandBuffer, section, blockSection, chunkSection, radius);
        return accessor;
    }

    private void init(
        @Nonnull ComponentAccessor<ChunkStore> commandBuffer,
        @Nonnull ArcaneSection section,
        @Nonnull BlockSection blockSection,
        @Nonnull ChunkSection chunkSection,
        int radius
    ) {
        super.init(commandBuffer, chunkSection.getX(), chunkSection.getY(), chunkSection.getZ(), radius);
        insertSectionComponent(ARCANE_COMPONENT, section, chunkSection.getX(), chunkSection.getY(), chunkSection.getZ());
        insertSectionComponent(BLOCK_COMPONENT, blockSection, chunkSection.getX(), chunkSection.getY(), chunkSection.getZ());
        insertSectionComponent(CHUNK_COMPONENT, chunkSection, chunkSection.getX(), chunkSection.getY(), chunkSection.getZ());
    }


    @Override
    @Nullable
    public ArcaneSection getArcaneSection(int cx, int cy, int cz) {
        return getComponentSection(cx, cy, cz, ARCANE_COMPONENT, ArcaneSection.getComponentType());
    }

    @Override
    @Nullable
    public BlockSection getBlockSection(int cx, int cy, int cz) {
        return getComponentSection(cx, cy, cz, BLOCK_COMPONENT, BlockSection.getComponentType());
    }

    @Override
    @Nullable
    public ChunkSection getChunkSection(int cx, int cy, int cz) {
        return getComponentSection(cx, cy, cz, CHUNK_COMPONENT, ChunkSection.getComponentType());
    }

    @Override
    @Nonnull
    public CommandBuffer<ChunkStore> getCommandBuffer() {
        return this.selfCommandBuffer;
    }
}

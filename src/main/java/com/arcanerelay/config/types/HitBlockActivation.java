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
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class HitBlockActivation extends Activation {
    public int damage;
    public Vector3i direction;

    public static final BuilderCodec<HitBlockActivation> CODEC = BuilderCodec.builder(
            HitBlockActivation.class,
            HitBlockActivation::new,
            Activation.ABSTRACT_CODEC
        ).documentation("Hits a block on the specified direction (relative to block orientation) with the given damage.")
        .appendInherited(
            new KeyedCodec<>("Damage", Codec.INTEGER),
            (a, r) -> a.damage = r,
            a -> a.damage,
            (a, p) -> a.damage = p.damage)
        .documentation("Amount of damage to deal to the block (default: 0).")
        .add()
        .appendInherited(
            new KeyedCodec<>("Direction", Vector3i.CODEC),
            (a, r) -> a.direction = r,
            a -> a.direction,
            (a, p) -> a.direction = p.direction)
        .documentation("Direction to hit the block in (default: [0, 1, 0]).")
        .add()
        .build();

    @Override
    public BlockTickStrategy execute(
        @Nonnull ArcaneCachedAccessor accessor, 
        @Nullable Ref<ChunkStore> sectionRef,
        @Nullable Ref<ChunkStore> blockRef, 
        int worldX, int worldY, int worldZ, 
        @Nonnull List<int[]> sources) 
    {
        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }
    
}

package com.arcanerelay.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

/** Block component for Arcane Suspender: Stores if a block is suspended, Toggleable variant will be used by the Suspender wand */
public class ArcaneSuspenderComponent implements Component<ChunkStore> {

    public static final BuilderCodec<ArcaneSuspenderComponent> CODEC = BuilderCodec.builder(ArcaneSuspenderComponent.class, ArcaneSuspenderComponent::new)
            .append(new KeyedCodec<>("Unmoveable toggle", Codec.BOOLEAN),
                    (c, v) -> c.UnmoveableToggle = v, c -> c.UnmoveableToggle)
            .documentation("Stores if a block has the unmoveable setting, This variant will be set by the Void wand")
            .add()
            .append(new KeyedCodec<>("Unmoveable forced", Codec.BOOLEAN),
                    (c, v) -> c.ForcedUnmoveable = v, c -> c.ForcedUnmoveable)
            .documentation("Stores if a block has the unmoveable setting, This variant can be enforced in the Asset editor for any given block")
            .add()
        .build();

    public ArcaneSuspenderComponent() { }

    private boolean UnmoveableToggle = false;
    private boolean ForcedUnmoveable = false;

    public boolean IsUnmoveable() {
        if (UnmoveableToggle) {
            return true;
        } else if (ForcedUnmoveable) {
            return true;
        } else {
            return false;
        }
    }

    public boolean ToggleUnmoveable() {
        UnmoveableToggle = !UnmoveableToggle;
        return UnmoveableToggle;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        ArcaneSuspenderComponent clone = new ArcaneSuspenderComponent();
        clone.UnmoveableToggle = this.UnmoveableToggle;
        clone.ForcedUnmoveable = this.ForcedUnmoveable;
        return clone;
    }
}

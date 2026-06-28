package com.arcanerelay.ui;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.ArcaneRelayConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Supplies {@link ArcaneRelayConfigSettingsPage} when the player interacts with a block
 * using OpenCustomUI with Page "ArcaneRelayConfig". Loads the configuration and
 * creates the UI page.
 */
public class ArcaneRelayConfigPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {

    public static final BuilderCodec<ArcaneRelayConfigPageSupplier> CODEC =
            BuilderCodec.builder(ArcaneRelayConfigPageSupplier.class, ArcaneRelayConfigPageSupplier::new).build();

    private ArcaneRelayConfig config;

    public ArcaneRelayConfigPageSupplier() {
        this.config = ArcaneRelayPlugin.get().getConfig();
    }

    public ArcaneRelayConfigPageSupplier(@Nonnull ArcaneRelayConfig config) {
        this.config = config;
    }

    @Nullable
    @Override
    public CustomUIPage tryCreate(
            @Nonnull Ref<EntityStore> ref,
            ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull PlayerRef playerRef,
            @Nonnull InteractionContext context) {
        return new ArcaneRelayConfigSettingsPage(playerRef, config);
    }
}

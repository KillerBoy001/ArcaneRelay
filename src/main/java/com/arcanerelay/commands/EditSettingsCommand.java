package com.arcanerelay.commands;

import javax.annotation.Nonnull;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.ui.ArcaneRelayConfigSettingsPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EditSettingsCommand extends AbstractPlayerCommand {
    public EditSettingsCommand() {
        super("settings", "Edit Arcane Relay settings");
        addAliases("s");
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        playerComponent.getPageManager().openCustomPage(ref, store,
            new ArcaneRelayConfigSettingsPage(playerRef, ArcaneRelayPlugin.getConfig()));
    }
}

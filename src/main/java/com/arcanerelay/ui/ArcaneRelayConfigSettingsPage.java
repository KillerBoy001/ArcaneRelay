package com.arcanerelay.ui;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.ArcaneRelayConfig;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Custom UI page for Arcane Relay Config: view and modify configuration settings.
 * Opened when the player opens the ArcaneRelayConfig settings UI.
 */
public class ArcaneRelayConfigSettingsPage extends InteractiveCustomUIPage<ArcaneRelayConfigSettingsPage.PageEventData> {
    @Nonnull
    private final ArcaneRelayConfig config;

    public ArcaneRelayConfigSettingsPage(@Nonnull PlayerRef playerRef, @Nonnull ArcaneRelayConfig config) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.config = config != null ? config : new ArcaneRelayConfig();
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ArcaneRelayConfig.ui");

        // Set values for all numeric settings
        commandBuilder.set("#TriggerDistanceInput.Value", String.valueOf(config.getTriggerDistance()));
        commandBuilder.set("#TargetDistanceInput.Value", String.valueOf(config.getTargetDistance()));
        commandBuilder.set("#PusherRangeInput.Value", String.valueOf(config.getPusherRange()));
        commandBuilder.set("#PullerRangeInput.Value", String.valueOf(config.getPullerRange()));
        commandBuilder.set("#BreakerDamageInput.Value", String.format("%.2f", config.getBreakerDamage()));
        commandBuilder.set("#NonMovableListInput.Value", String.join(", ", config.getNoneMoveableBlocks()));
        commandBuilder.set("#NonRotatableBlacklistInput.Value", String.join(", ", config.getNoneRotatableBlocks()));

        // Set up textbox event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TriggerDistanceInput", EventData.of("@TriggerDistanceInput", "#TriggerDistanceInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TargetDistanceInput", EventData.of("@TargetDistanceInput", "#TargetDistanceInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PusherRangeInput", EventData.of("@PusherRangeInput", "#PusherRangeInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PullerRangeInput", EventData.of("@PullerRangeInput", "#PullerRangeInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BreakerDamageInput", EventData.of("@BreakerDamageInput", "#BreakerDamageInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonMovableListInput", EventData.of("@NonMovableListInput", "#NonMovableListInput.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonRotatableBlacklistInput", EventData.of("@NonRotatableBlacklistInput", "#NonRotatableBlacklistInput.Value"), false);

        // Set up button event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "Save"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Action", "Cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        super.handleDataEvent(ref, store, data);
        boolean changed = false;

        if (data.TriggerDistanceInput != null) {
            try {
                int value = Integer.parseInt(data.TriggerDistanceInput);
                if (value >= 0) {
                    this.playerRef.sendMessage(Message.raw("Trigger Distance updated to: " + data.TriggerDistanceInput));
                    config.setTriggerDistance(value);
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.TargetDistanceInput != null) {
            try {
                int value = Integer.parseInt(data.TargetDistanceInput);
                if (value >= 0) {
                    this.playerRef.sendMessage(Message.raw("Target Distance updated to: " + data.TargetDistanceInput));
                    config.setTargetDistance(value);
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.PusherRangeInput != null) {
            try {
                int value = Integer.parseInt(data.PusherRangeInput);
                if (value >= 0) {
                    this.playerRef.sendMessage(Message.raw("Pusher Range updated to: " + data.PusherRangeInput));
                    config.setPusherRange(value);
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.PullerRangeInput != null) {
            try {
                int value = Integer.parseInt(data.PullerRangeInput);
                if (value >= 0) {
                    this.playerRef.sendMessage(Message.raw("Puller Range updated to: " + data.PullerRangeInput));
                    config.setPullerRange(value);
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.BreakerDamageInput != null) {
            try {
                double value = Double.parseDouble(data.BreakerDamageInput);
                if (value >= 0.0) {
                    this.playerRef.sendMessage(Message.raw("Breaker DMG updated to: " + data.BreakerDamageInput));
                    config.setBreakerDamage(value);
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.NonMovableListInput != null) {
            if (!data.NonMovableListInput.isEmpty()) {
                String[] blocks = Arrays.stream(data.NonMovableListInput.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                config.setNoneMoveableBlocks(blocks);
                this.playerRef.sendMessage(Message.raw("Non-Moveable updated to: " + data.NonMovableListInput));
            }
            changed = true;
        }

        if (data.NonRotatableBlacklistInput != null) {
            if (!data.NonRotatableBlacklistInput.isEmpty()) {
                String[] blocks = Arrays.stream(data.NonRotatableBlacklistInput.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                config.setNoneRotatableBlocks(blocks);
                this.playerRef.sendMessage(Message.raw("Non-Moveable updated to: " + data.NonRotatableBlacklistInput));
            }
            changed = true;
        }

        if (data.action == null || data.action.isEmpty()) return;
        switch (data.action) {
            case "Save":
                ArcaneRelayPlugin.get().saveConfig();
                store.getExternalData().getWorld().execute(() -> {
                    closePage();
                });
                return;
            case "Cancel":
                store.getExternalData().getWorld().execute(() -> {
                    closePage();
                });
                return;
        }
    }

    private void closePage() {
        Ref<EntityStore> playerRef = this.playerRef.getReference();
        if (playerRef != null && playerRef.isValid()) {
            Player playerComponent = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.getPageManager().setPage(playerRef, playerRef.getStore(), Page.None);
            }
        }
    }

    public static final class PageEventData {
        public String action;
        public String textValue;
        public String TriggerDistanceInput;
        public String TargetDistanceInput;
        public String PusherRangeInput;
        public String PullerRangeInput;
        public String BreakerDamageInput;
        public String NonMovableListInput;
        public String NonRotatableBlacklistInput;

        public static final BuilderCodec<PageEventData> CODEC =
                BuilderCodec.builder(PageEventData.class, PageEventData::new)
                        .append(
                                new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v,
                                d -> d.action)
                        .add()
                        .append(
                                new KeyedCodec<>("TextValue", Codec.STRING),
                                (d, v) -> d.textValue = v,
                                d -> d.textValue)
                        .add()
                        .append(new KeyedCodec<>("@TriggerDistanceInput", Codec.STRING), (data, s) -> data.TriggerDistanceInput = s, data -> data.TriggerDistanceInput).add()
                        .append(new KeyedCodec<>("@TargetDistanceInput", Codec.STRING), (data, s) -> data.TargetDistanceInput = s, data -> data.TargetDistanceInput).add()
                        .append(new KeyedCodec<>("@PusherRangeInput", Codec.STRING), (data, s) -> data.PusherRangeInput = s, data -> data.PusherRangeInput).add()
                        .append(new KeyedCodec<>("@PullerRangeInput", Codec.STRING), (data, s) -> data.PullerRangeInput = s, data -> data.PullerRangeInput).add()
                        .append(new KeyedCodec<>("@BreakerDamageInput", Codec.STRING), (data, s) -> data.BreakerDamageInput = s, data -> data.BreakerDamageInput).add()
                        .append(new KeyedCodec<>("@NonMovableListInput", Codec.STRING), (data, s) -> data.NonMovableListInput = s, data -> data.NonMovableListInput).add()
                        .append(new KeyedCodec<>("@NonRotatableBlacklistInput", Codec.STRING), (data, s) -> data.NonRotatableBlacklistInput = s, data -> data.NonRotatableBlacklistInput).add()
                        .build();
    }
}

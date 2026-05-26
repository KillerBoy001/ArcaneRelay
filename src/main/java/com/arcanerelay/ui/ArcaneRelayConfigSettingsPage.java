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
    private ArcaneRelayConfig config;

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
        EventData triggerData = EventData.of("Action", "UpdateTriggerDistance");
        triggerData.put("TriggerDistanceValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TriggerDistanceInput", triggerData, false);

        EventData targetData = EventData.of("Action", "UpdateTargetDistance");
        targetData.put("TargetDistanceValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TargetDistanceInput", targetData, false);

        EventData pusherData = EventData.of("Action", "UpdatePusherRange");
        pusherData.put("PusherRangeValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PusherRangeInput", pusherData, false);

        EventData pullerData = EventData.of("Action", "UpdatePullerRange");
        pullerData.put("PullerRangeValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PullerRangeInput", pullerData, false);

        EventData breakerData = EventData.of("Action", "UpdateBreakerDamage");
        breakerData.put("BreakerDamageValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BreakerDamageInput", breakerData, false);

        EventData nonMovableData = EventData.of("Action", "UpdateNonMovableList");
        nonMovableData.put("NonMovableListValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonMovableListInput", nonMovableData, false);

        EventData nonRotatableData = EventData.of("Action", "UpdateNonRotatableList");
        nonRotatableData.put("NonRotatableListValue", "$el.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonRotatableBlacklistInput", nonRotatableData, false);

        // Set up button event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "Save"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Action", "Cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        if (data.action == null || data.action.isEmpty()) return;

        switch (data.action) {
            case "UpdateTriggerDistance":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            config.setTriggerDistance(value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return;
            case "UpdateTargetDistance":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            config.setTargetDistance(value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return;
            case "UpdatePusherRange":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            config.setPusherRange(value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return;
            case "UpdatePullerRange":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            config.setPullerRange(value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return;
            case "UpdateBreakerDamage":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        double value = Double.parseDouble(data.textValue    );
                        if (value >= 0.0) {
                            config.setBreakerDamage(value);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                return;
            case "UpdateNonMovableList":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    String[] blocks = Arrays.stream(data.textValue.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    config.setNoneMoveableBlocks(blocks);
                }
                return;
            case "UpdateNonRotatableList":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    String[] blocks = Arrays.stream(data.textValue.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    config.setNoneRotatableBlocks(blocks);
                }
                return;
            case "Save":
                ArcaneRelayPlugin.saveConfig();
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
                .build();
    }
}

package com.arcanerelay.features.config.ui;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.features.config.ArcaneRelayConfig;
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
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom UI page for Arcane Relay Config: view and modify configuration settings.
 * Opened when the player opens the ArcaneRelayConfig settings UI.
 */
public class ArcaneRelayConfigSettingsPage extends InteractiveCustomUIPage<ArcaneRelayConfigSettingsPage.PageEventData> {
    @Nonnull
    private ArcaneRelayConfig originalConfig;

    @Nonnull
    private ArcaneRelayConfig stagingConfig;

    private final Set<String> invalidFields = new HashSet<>();

    public ArcaneRelayConfigSettingsPage(@Nonnull PlayerRef playerRef, @Nonnull ArcaneRelayConfig config) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.originalConfig = config != null ? config : new ArcaneRelayConfig();
        this.stagingConfig = new ArcaneRelayConfig(this.originalConfig);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/ArcaneRelayConfig.ui");

        // Set values for all numeric settings
        commandBuilder.set("#RelayDistanceInput.Value", String.valueOf(stagingConfig.getRelayDistance()));
        commandBuilder.set("#PusherRangeInput.Value", String.valueOf(stagingConfig.getPusherRange()));
        commandBuilder.set("#PullerRangeInput.Value", String.valueOf(stagingConfig.getPullerRange()));
        commandBuilder.set("#BreakerDamageInput.Value", String.format("%.2f", stagingConfig.getBreakerEntityDamage()));
        commandBuilder.set("#BreakerBlockDamageScalarInput.Value", String.format("%.2f", stagingConfig.getBreakerBlockDamageScalar()));
        commandBuilder.set("#NonMovableListInput.Value", String.join(", ", stagingConfig.getNoneMoveableBlocks()));
        commandBuilder.set("#NonRotatableBlacklistInput.Value", String.join(", ", stagingConfig.getNoneRotatableBlocks()));

        // Set up textbox event bindings
        EventData triggerData = EventData.of("Action", "UpdateRelayDistance");
        triggerData.put("@TextValue", "#RelayDistanceInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RelayDistanceInput", triggerData, false);

        EventData pusherData = EventData.of("Action", "UpdatePusherRange");
        pusherData.put("@TextValue", "#PusherRangeInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PusherRangeInput", pusherData, false);

        EventData pullerData = EventData.of("Action", "UpdatePullerRange");
        pullerData.put("@TextValue", "#PullerRangeInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PullerRangeInput", pullerData, false);

        EventData breakerData = EventData.of("Action", "UpdateBreakerDamage");
        breakerData.put("@TextValue", "#BreakerDamageInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BreakerDamageInput", breakerData, false);

        EventData breakerBlockData = EventData.of("Action", "UpdateBreakerBlockDamageScalar");
        breakerBlockData.put("@TextValue", "#BreakerBlockDamageScalarInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BreakerBlockDamageScalarInput", breakerBlockData, false);

        EventData nonMovableData = EventData.of("Action", "UpdateNonMovableList");
        nonMovableData.put("@TextValue", "#NonMovableListInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonMovableListInput", nonMovableData, false);

        EventData nonRotatableData = EventData.of("Action", "UpdateNonRotatableList");
        nonRotatableData.put("@TextValue", "#NonRotatableBlacklistInput.Value");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NonRotatableBlacklistInput", nonRotatableData, false);

        // Set up button event bindings
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Action", "Save"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Action", "Cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        if (data.action == null || data.action.isEmpty()) return;

        switch (data.action) {
            case "UpdateRelayDistance":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            stagingConfig.setRelayDistance(value);
                            invalidFields.remove(data.action);
                        }
                    } catch (NumberFormatException ignored) {
                        invalidFields.add(data.action);
                    }
                }
                return;
            case "UpdatePusherRange":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            stagingConfig.setPusherRange(value);
                            invalidFields.remove(data.action);
                        }
                    } catch (NumberFormatException ignored) {
                        invalidFields.add(data.action);
                    }
                }
                return;
            case "UpdatePullerRange":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(data.textValue);
                        if (value >= 0) {
                            stagingConfig.setPullerRange(value);
                            invalidFields.remove(data.action);
                        }
                    } catch (NumberFormatException ignored) {
                        invalidFields.add(data.action);
                    }
                }
                return;
            case "UpdateBreakerDamage":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        float value = Float.parseFloat(data.textValue);
                        if (value >= 0.0f) {
                            stagingConfig.setBreakerEntityDamage(value);
                            invalidFields.remove(data.action);
                        }
                    } catch (NumberFormatException ignored) {
                        invalidFields.add(data.action);
                    }
                }
                return;
            case "UpdateBreakerBlockDamageScalar":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    try {
                        float value = Float.parseFloat(data.textValue);
                        if (value >= 0.0f) {
                            stagingConfig.setBreakerBlockDamageScalar(value);
                            invalidFields.remove(data.action);
                        }
                    } catch (NumberFormatException ignored) {
                        invalidFields.add(data.action);
                    }
                }
                return;
            case "UpdateNonMovableList":
                 if (data.textValue != null && !data.textValue.isEmpty()) {
                    String[] blocks = Arrays.stream(data.textValue.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    stagingConfig.setNoneMoveableBlocks(blocks);
                }
                return;
            case "UpdateNonRotatableList":
                if (data.textValue != null && !data.textValue.isEmpty()) {
                    String[] blocks = Arrays.stream(data.textValue.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    stagingConfig.setNoneRotatableBlocks(blocks);
                }
                return;
            case "Save":
                if (!originalConfig.equals(stagingConfig)) { 
                    originalConfig.copyFrom(stagingConfig); 
                    ArcaneRelayPlugin.get().saveConfig();

                    if (invalidFields.isEmpty()) {
                        NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsSucces"), NotificationStyle.Success);
                    } else {
                        NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsSuccesWithInvalid"), NotificationStyle.Warning);
                    }
                } else {
                    if (invalidFields.isEmpty()) {
                        NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsSuccesNoChange"), NotificationStyle.Success);
                    } else {
                        NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsSuccesNoChangeWithInvalid"), NotificationStyle.Warning);
                    }
                }

                store.getExternalData().getWorld().execute(this::closePage);
                return;
            case "Cancel":
                NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsNoChange"), NotificationStyle.Warning);
                store.getExternalData().getWorld().execute(this::closePage);
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
                    new KeyedCodec<>("@TextValue", Codec.STRING),
                    (d, v) -> d.textValue = v,
                    d -> d.textValue)
                .add()
                .build();
    }
}

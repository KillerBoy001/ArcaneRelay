package com.arcanerelay.ui;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.config.ArcaneRelayConfig;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
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
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Custom UI page for Arcane Relay Config: view and modify configuration settings.
 * Opened when the player opens the ArcaneRelayConfig settings UI.
 */
public class ArcaneRelayConfigSettingsPage extends InteractiveCustomUIPage<ArcaneRelayConfigSettingsPage.PageEventData> {
    @Nonnull
    private final ArcaneRelayConfig config;
    private Integer TmpTriggerDist;
    private Integer TmpTargetDist;
    private Integer TmpPusherRange;
    private Integer TmpPullerRange;
    private Double TmpBreakerDMG;
    private String[] TmpMoveableBlacklist;
    private String[] TmpRotatableBlacklist;

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
                    TmpTriggerDist = value;
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.TargetDistanceInput != null) {
            try {
                int value = Integer.parseInt(data.TargetDistanceInput);
                if (value >= 0) {
                    TmpTargetDist = value;
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.PusherRangeInput != null) {
            try {
                int value = Integer.parseInt(data.PusherRangeInput);
                if (value >= 0) {
                    TmpPusherRange = value;
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.PullerRangeInput != null) {
            try {
                int value = Integer.parseInt(data.PullerRangeInput);
                if (value >= 0) {
                    TmpPullerRange = value;
                }
            } catch (NumberFormatException ignored) {
            }
            changed = true;
        }

        if (data.BreakerDamageInput != null) {
            try {
                double value = Double.parseDouble(data.BreakerDamageInput);
                if (value >= 0.0) {
                    TmpBreakerDMG = value;
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
                TmpMoveableBlacklist = blocks;
            }
            changed = true;
        }

        if (data.NonRotatableBlacklistInput != null) {
            if (!data.NonRotatableBlacklistInput.isEmpty()) {
                String[] blocks = Arrays.stream(data.NonRotatableBlacklistInput.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                TmpRotatableBlacklist = blocks;
            }
            changed = true;
        }

        if (data.action == null || data.action.isEmpty()) return;
        switch (data.action) {
            case "Save":
                boolean c = false;
                if (TmpTriggerDist!=null){config.setTriggerDistance(TmpTriggerDist);TmpTriggerDist=null;c=true;}
                if (TmpTargetDist!=null){config.setTargetDistance(TmpTargetDist);TmpTargetDist=null;c=true;}
                if (TmpPusherRange!=null){config.setPusherRange(TmpPusherRange);TmpPusherRange=null;c=true;}
                if (TmpPullerRange!=null){config.setPullerRange(TmpPullerRange);TmpPullerRange=null;c=true;}
                if (TmpBreakerDMG!=null){config.setBreakerDamage(TmpBreakerDMG);TmpBreakerDMG=null;c=true;}
                if (TmpMoveableBlacklist!=null){config.setNoneMoveableBlocks(TmpMoveableBlacklist);TmpMoveableBlacklist=null;c=true;}
                if (TmpRotatableBlacklist!=null){config.setNoneRotatableBlocks(TmpRotatableBlacklist);TmpRotatableBlacklist=null;c=true;}

                if (c) {
                    ArcaneRelayPlugin.get().saveConfig();
                    NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsSucces"), NotificationStyle.Success);
                }else{
                    NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsNoChange"), NotificationStyle.Warning);
                }

                store.getExternalData().getWorld().execute(() -> {
                    closePage();
                });
                return;
            case "Cancel":
                NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.SettingsNoChange"), NotificationStyle.Warning);
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

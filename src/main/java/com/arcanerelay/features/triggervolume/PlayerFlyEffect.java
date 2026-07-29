package com.arcanerelay.features.triggervolume;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.features.signal.util.ArcaneUtil;
import com.arcanerelay.features.signaltrigger.components.ArcaneTriggerBlock;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.builtin.triggervolumes.shape.TriggerVolumeShape;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.protocol.packets.player.UpdateMovementSettings;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

public class PlayerFlyEffect extends TriggerEffect {

    @Nonnull
    public static final BuilderCodec<PlayerFlyEffect> CODEC =BuilderCodec.builder(PlayerFlyEffect.class, PlayerFlyEffect::new, BASE_CODEC)
            .append(new KeyedCodec<>("Action", new EnumCodec<>(EffectAction.class)),
                    (e, v) -> e.Action = v,
                    (e) -> e.Action)
            .add()
            .append(new KeyedCodec<>("HorizontalSpeed", Codec.FLOAT),
                    (e, v) -> e.HSpeed = v,
                    (e) -> e.HSpeed)
            .add()
            .append(new KeyedCodec<>("VerticalSpeed", Codec.FLOAT),
                    (e, v) -> e.VSpeed = v,
                    (e) -> e.VSpeed)
            .add()
            .build();

    @Nonnull
    private EffectAction Action;

    public enum EffectAction {
        ENABLE_FLIGHT,
        DISABLE_FLIGHT;
    }
    public static float HSpeed = 5.0f;
    public static float VSpeed = 7.0f;


    public PlayerFlyEffect() {
        this.Action = EffectAction.ENABLE_FLIGHT;
    }


    @Override
    public void execute(@Nonnull TriggerContext context) {
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> entityRef = context.getEntityRef();
        switch(this.Action) {
            case ENABLE_FLIGHT:
                EnableFly(store,entityRef);
                break;

            case DISABLE_FLIGHT:
                DisableFly(store,entityRef);
                break;
        }
    }

    public static void EnableFly(Store<EntityStore> buffer, Ref<EntityStore> playerRef)
    {
        MovementManager mm = buffer.getComponent(playerRef, MovementManager.getComponentType());
        if (mm == null) return;

        MovementSettings settings = mm.getSettings();
        settings.canFly = true;
        settings.horizontalFlySpeed = HSpeed;
        settings.verticalFlySpeed = VSpeed;

        PacketHandler handler = getPacketHandler(buffer, playerRef);
        if (handler == null) return;

        handler.writeNoCache(new UpdateMovementSettings(settings));
        handler.writeNoCache(new SetMovementStates(new SavedMovementStates(true)));

    }

    public static void DisableFly(Store<EntityStore> buffer, Ref<EntityStore> playerRef) {
        MovementManager mm = buffer.getComponent(playerRef, MovementManager.getComponentType());
        if (mm == null) return;

        PacketHandler handler = getPacketHandler(buffer, playerRef);
        if (handler == null) return;

        handler.writeNoCache(new SetMovementStates(new SavedMovementStates(false)));

        mm.applyDefaultSettings();
        handler.writeNoCache(new UpdateMovementSettings(mm.getSettings()));
    }

    private static PacketHandler getPacketHandler(Store<EntityStore> buffer,
                                                  Ref<EntityStore> playerRef) {
        PlayerRef pr = buffer.getComponent(playerRef, PlayerRef.getComponentType());
        return pr != null ? pr.getPacketHandler() : null;
    }

}



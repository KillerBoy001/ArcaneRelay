package com.arcanerelay.interactions;

import com.arcanerelay.ArcaneRelayPlugin;
import com.arcanerelay.components.ArcaneConfiguratorComponent;
import com.arcanerelay.components.ArcaneSuspenderComponent;
import com.arcanerelay.components.ArcaneTriggerBlock;
import com.arcanerelay.util.VisualsUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.joml.Vector3i;

import javax.annotation.Nonnull;

/**
 * Primary interaction: select a block to set the suspension state to.
 */
public class ToggleSuspendInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<ToggleSuspendInteraction> CODEC = BuilderCodec.builder(
            ToggleSuspendInteraction.class, ToggleSuspendInteraction::new, SimpleInstantInteraction.CODEC)
            .documentation("ArcaneRelay: select a block to toggle SuspensionState")
            .build();

    public ToggleSuspendInteraction() { }

    public ToggleSuspendInteraction(String id) {
        super(id);
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> cb = context.getCommandBuffer();
        if (cb == null) {
            context.getState().state = InteractionState.Failed; 
            return;
        };

        Ref<EntityStore> ref = context.getEntity();
        Player player = cb.getComponent(ref, Player.getComponentType());

        PlayerRef playerRef = cb.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) 
        {
            context.getState().state = InteractionState.Failed; 
            return;
        }

        BlockPosition targetPosition = context.getTargetBlock();
        if (targetPosition == null) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.translation("server.arcanerelay.notifications.noBlockInRange"), NotificationStyle.Warning);
            context.getState().state = InteractionState.Failed; 
            return;
        }

        Vector3i target = new Vector3i(targetPosition.x, targetPosition.y, targetPosition.z);
        
        World world = cb.getExternalData().getWorld();
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(target.x, target.z));
        BlockType BT = world.getBlockType(target);
        if (chunk == null) {
            context.getState().state = InteractionState.Failed; 
            return;
        }


        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(target.x, target.y, target.z);
        if (blockRef == null ||  !blockRef.isValid()) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw("No BlockEntity ref"), NotificationStyle.Warning);
            context.getState().state = InteractionState.Failed;

            return;
        }

        Store<ChunkStore> store = world.getChunkStore().getStore();

        ArcaneSuspenderComponent Component = BlockModule.getComponent(ArcaneRelayPlugin.get().getArcaneSuspenderComponentType(), world, target.x, target.y, target.z);
        if (Component == null) {
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw("Added Suspend component"), NotificationStyle.Success);
            store.addComponent(blockRef,ArcaneRelayPlugin.get().getArcaneSuspenderComponentType());

            Component.ToggleUnmoveable();
            boolean currentState = Component.IsUnmoveable();
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw("Toggled Component to: "+currentState), NotificationStyle.Success);
            context.getState().state = InteractionState.Finished;
        }
        else {
            Component.ToggleUnmoveable();
            boolean currentState = Component.IsUnmoveable();
            NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw("Toggled Component to: "+currentState), NotificationStyle.Success);
            context.getState().state = InteractionState.Finished;
        }


        boolean cycleColor = true;
        VisualsUtil.displayTriggerConnections(world, target, cycleColor);
    }
}

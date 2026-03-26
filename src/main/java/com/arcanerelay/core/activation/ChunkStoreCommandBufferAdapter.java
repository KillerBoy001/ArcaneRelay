package com.arcanerelay.core.activation;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.event.EntityHolderEventType;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Wraps CommandBuffer as ChunkStoreCommandBufferLike for the tick path. */
public final class ChunkStoreCommandBufferAdapter implements ChunkStoreCommandBufferLike {
    private final CommandBuffer<ChunkStore> buffer;

    public ChunkStoreCommandBufferAdapter(@Nonnull CommandBuffer<ChunkStore> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run(@Nonnull Consumer<Store<ChunkStore>> consumer) {
        buffer.run(consumer);
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> T getComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType) {
        return buffer.getComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> T ensureAndGetComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType) {
        return buffer.ensureAndGetComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public com.hypixel.hytale.component.Archetype<ChunkStore> getArchetype(@Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref) {
        return buffer.getArchetype(ref);
    }

    @Nonnull
    @Override
    public <T extends com.hypixel.hytale.component.Resource<ChunkStore>> T getResource(
            @Nonnull com.hypixel.hytale.component.ResourceType<ChunkStore, T> resourceType) {
        return buffer.getResource(resourceType);
    }

    @Nonnull
    @Override
    public ChunkStore getExternalData() {
        return buffer.getExternalData();
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> void putComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType,
            @Nonnull T component) {
        buffer.putComponent(ref, componentType, component);
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> void addComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType,
            @Nonnull T component) {
        buffer.addComponent(ref, componentType, component);
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> T addComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType) {
        return buffer.addComponent(ref, componentType);
    }

    @Override
    public com.hypixel.hytale.component.Ref<ChunkStore>[] addEntities(
            @Nonnull com.hypixel.hytale.component.Holder<ChunkStore>[] holders,
            @Nonnull com.hypixel.hytale.component.AddReason reason) {
        return buffer.addEntities(holders, reason);
    }

    @Override
    public com.hypixel.hytale.component.Ref<ChunkStore> addEntity(
            @Nonnull com.hypixel.hytale.component.Holder<ChunkStore> holder,
            @Nonnull com.hypixel.hytale.component.AddReason reason) {
        return buffer.addEntity(holder, reason);
    }

    @Nonnull
    @Override
    public com.hypixel.hytale.component.Holder<ChunkStore> removeEntity(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.Holder<ChunkStore> holder,
            @Nonnull com.hypixel.hytale.component.RemoveReason reason) {
        return buffer.removeEntity(ref, holder, reason);
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> void removeComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType) {
        buffer.removeComponent(ref, componentType);
    }

    @Override
    public <T extends com.hypixel.hytale.component.Component<ChunkStore>> void tryRemoveComponent(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull com.hypixel.hytale.component.ComponentType<ChunkStore, T> componentType) {
        buffer.tryRemoveComponent(ref, componentType);
    }

    @Override
    public <Event extends com.hypixel.hytale.component.system.EcsEvent> void invoke(
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull Event event) {
        buffer.invoke(ref, event);
    }

    @Override
    public <Event extends com.hypixel.hytale.component.system.EcsEvent> void invoke(
            @Nonnull com.hypixel.hytale.component.event.EntityEventType<ChunkStore, Event> eventType,
            @Nonnull com.hypixel.hytale.component.Ref<ChunkStore> ref,
            @Nonnull Event event) {
        buffer.invoke(eventType, ref, event);
    }

    @Override
    public <Event extends com.hypixel.hytale.component.system.EcsEvent> void invoke(@Nonnull Event event) {
        buffer.invoke(event);
    }

    @Override
    public <Event extends com.hypixel.hytale.component.system.EcsEvent> void invoke(
            @Nonnull com.hypixel.hytale.component.event.WorldEventType<ChunkStore, Event> eventType,
            @Nonnull Event event) {
        buffer.invoke(eventType, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(Holder<ChunkStore> holder, Event event) {
        buffer.invoke(holder, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(EntityHolderEventType<ChunkStore, Event> entityHolderEventType, Holder<ChunkStore> holder, Event event) {
        buffer.invoke(entityHolderEventType, holder, event);
    }
}

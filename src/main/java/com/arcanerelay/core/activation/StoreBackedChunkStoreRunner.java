package com.arcanerelay.core.activation;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.event.EntityEventType;
import com.hypixel.hytale.component.event.WorldEventType;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Wraps Store so it can be used as ChunkStoreCommandBufferLike from interactions; run() executes immediately. */
public final class StoreBackedChunkStoreRunner implements ChunkStoreCommandBufferLike {

    private final Store<ChunkStore> store;

    public StoreBackedChunkStoreRunner(@Nonnull Store<ChunkStore> store) {
        this.store = store;
    }

    @Override
    public void run(@Nonnull Consumer<Store<ChunkStore>> consumer) {
        consumer.accept(store);
    }

    @Nullable
    @Override
    public <T extends Component<ChunkStore>> T getComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return store.getComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public <T extends Component<ChunkStore>> T ensureAndGetComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return store.ensureAndGetComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public Archetype<ChunkStore> getArchetype(@Nonnull Ref<ChunkStore> ref) {
        return store.getArchetype(ref);
    }

    @Nonnull
    @Override
    public <T extends Resource<ChunkStore>> T getResource(@Nonnull ResourceType<ChunkStore, T> resourceType) {
        return store.getResource(resourceType);
    }

    @Nonnull
    @Override
    public ChunkStore getExternalData() {
        return store.getExternalData();
    }

    @Override
    public <T extends Component<ChunkStore>> void putComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType, @Nonnull T component) {
        store.putComponent(ref, componentType, component);
    }

    @Override
    public <T extends Component<ChunkStore>> void addComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType, @Nonnull T component) {
        store.addComponent(ref, componentType, component);
    }

    @Override
    public <T extends Component<ChunkStore>> T addComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return store.addComponent(ref, componentType);
    }

    @Override
    public Ref<ChunkStore>[] addEntities(@Nonnull Holder<ChunkStore>[] holders, @Nonnull AddReason reason) {
        return store.addEntities(holders, reason);
    }

    @Nullable
    @Override
    public Ref<ChunkStore> addEntity(@Nonnull Holder<ChunkStore> holder, @Nonnull AddReason reason) {
        return store.addEntity(holder, reason);
    }

    @Nonnull
    @Override
    public Holder<ChunkStore> removeEntity(@Nonnull Ref<ChunkStore> ref, @Nonnull Holder<ChunkStore> holder, @Nonnull RemoveReason reason) {
        return store.removeEntity(ref, holder, reason);
    }

    @Override
    public <T extends Component<ChunkStore>> void removeComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        store.removeComponent(ref, componentType);
    }

    @Override
    public <T extends Component<ChunkStore>> void tryRemoveComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        store.tryRemoveComponent(ref, componentType);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull Ref<ChunkStore> ref, @Nonnull Event event) {
        store.invoke(ref, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull EntityEventType<ChunkStore, Event> eventType, @Nonnull Ref<ChunkStore> ref, @Nonnull Event event) {
        store.invoke(eventType, ref, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull Event event) {
        store.invoke(event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull WorldEventType<ChunkStore, Event> eventType, @Nonnull Event event) {
        store.invoke(eventType, event);
    }
}

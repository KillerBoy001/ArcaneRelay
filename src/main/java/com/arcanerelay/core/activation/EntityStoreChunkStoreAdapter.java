package com.arcanerelay.core.activation;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Wraps Store so it can be used as ChunkStoreCommandBufferLike from interactions; run() executes immediately. */
public final class EntityStoreChunkStoreAdapter implements ChunkStoreCommandBufferLike {
    private final CommandBuffer<EntityStore> entityStore;
    private final Store<ChunkStore> chunkStore;

    public EntityStoreChunkStoreAdapter(@Nonnull CommandBuffer<EntityStore> store) {
        this.entityStore = store;
        this.chunkStore = store.getExternalData().getWorld().getChunkStore().getStore();
    }

    @Override
    public void run(@Nonnull Consumer<Store<ChunkStore>> consumer) {
        entityStore.run((_) -> {
            consumer.accept(chunkStore);
        });
    }

    @Nullable
    @Override
    public <T extends Component<ChunkStore>> T getComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return chunkStore.getComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public <T extends Component<ChunkStore>> T ensureAndGetComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return chunkStore.ensureAndGetComponent(ref, componentType);
    }

    @Nonnull
    @Override
    public Archetype<ChunkStore> getArchetype(@Nonnull Ref<ChunkStore> ref) {
        return chunkStore.getArchetype(ref);
    }

    @Nonnull
    @Override
    public <T extends Resource<ChunkStore>> T getResource(@Nonnull ResourceType<ChunkStore, T> resourceType) {
        return chunkStore.getResource(resourceType);
    }

    @Nonnull
    @Override
    public ChunkStore getExternalData() {
        return chunkStore.getExternalData();
    }

    @Override
    public <T extends Component<ChunkStore>> void putComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType, @Nonnull T component) {
        chunkStore.putComponent(ref, componentType, component);
    }

    @Override
    public <T extends Component<ChunkStore>> void addComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType, @Nonnull T component) {
        chunkStore.addComponent(ref, componentType, component);
    }

    @Override
    public <T extends Component<ChunkStore>> T addComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        return chunkStore.addComponent(ref, componentType);
    }

    @Override
    public Ref<ChunkStore>[] addEntities(@Nonnull Holder<ChunkStore>[] holders, @Nonnull AddReason reason) {
        return chunkStore.addEntities(holders, reason);
    }

    @Nullable
    @Override
    public Ref<ChunkStore> addEntity(@Nonnull Holder<ChunkStore> holder, @Nonnull AddReason reason) {
        return chunkStore.addEntity(holder, reason);
    }

    @Nonnull
    @Override
    public Holder<ChunkStore> removeEntity(@Nonnull Ref<ChunkStore> ref, @Nonnull Holder<ChunkStore> holder, @Nonnull RemoveReason reason) {
        return chunkStore.removeEntity(ref, holder, reason);
    }

    @Override
    public <T extends Component<ChunkStore>> void removeComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        chunkStore.removeComponent(ref, componentType);
    }

    @Override
    public <T extends Component<ChunkStore>> void tryRemoveComponent(@Nonnull Ref<ChunkStore> ref, @Nonnull ComponentType<ChunkStore, T> componentType) {
        chunkStore.tryRemoveComponent(ref, componentType);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull Ref<ChunkStore> ref, @Nonnull Event event) {
        chunkStore.invoke(ref, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull EntityEventType<ChunkStore, Event> eventType, @Nonnull Ref<ChunkStore> ref, @Nonnull Event event) {
        chunkStore.invoke(eventType, ref, event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull Event event) {
        chunkStore.invoke(event);
    }

    @Override
    public <Event extends EcsEvent> void invoke(@Nonnull WorldEventType<ChunkStore, Event> eventType, @Nonnull Event event) {
        chunkStore.invoke(eventType, event);
    }
}

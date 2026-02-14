package com.arcanerelay.core.activation;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Abstraction for command buffer or store-backed access when running activations. */
public interface ChunkStoreCommandBufferLike extends ComponentAccessor<ChunkStore> {

    void run(@Nonnull Consumer<Store<ChunkStore>> consumer);
}

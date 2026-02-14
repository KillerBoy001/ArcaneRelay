package com.arcanerelay.config;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActivationBinding implements JsonAssetWithMap<String, DefaultAssetMap<String, ActivationBinding>> {
    public static final AssetBuilderCodec<String, ActivationBinding> CODEC = AssetBuilderCodec.builder(ActivationBinding.class, ActivationBinding::new, Codec.STRING,
                (obj, id) -> obj.id = id, obj -> obj.id,
                (obj, data) -> obj.data = data, obj -> obj.data)
            .append(
                new KeyedCodec<>("Bindings", new MapCodec<>(Codec.STRING, HashMap::new), false),
                (obj, bindings) -> obj.bindings = bindings != null ? bindings : new HashMap<>(),
                obj -> obj.bindings)
            .add()
            .append(
                new KeyedCodec<>("Default", Codec.STRING, false),
                (obj, defaultActivation) -> obj.defaultActivation = defaultActivation,
                obj -> obj.defaultActivation)
            .add()
            .build();

    public static final String DEFAULT_ACTIVATION_ID = "use_block";
    private static final Map<String, String> blockToActivationId = new HashMap<>();
    private static String defaultActivationId = DEFAULT_ACTIVATION_ID;

    private Map<String, String> bindings = new HashMap<>();
    @Nullable
    private String defaultActivation;
    private AssetExtraInfo.Data data;
    private String id;


    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    /** Block type key → activation id (reference to Item/Activations asset). */
    @Nonnull
    public Map<String, String> getBindings() {
        return bindings != null ? bindings : Collections.emptyMap();
    }

    /** Fallback activation id when a block type is not in bindings. */
    @Nullable
    public String getDefaultActivation() {
        return defaultActivation;
    }

    // --- Registry: merged bindings and lookup ---

    /** Registers the ActivationBinding asset store (Item/ActivationBindings). */
    @SuppressWarnings("unchecked")
    public static void registerAssetStore() {
        HytaleAssetStore.Builder<String, ActivationBinding, DefaultAssetMap<String, ActivationBinding>> b =
            (HytaleAssetStore.Builder<String, ActivationBinding, DefaultAssetMap<String, ActivationBinding>>)
                (Object) HytaleAssetStore.builder(ActivationBinding.class, new DefaultAssetMap<>());
        AssetRegistry.register(
            b.setPath("Item/ActivationBindings")
                .setCodec(ActivationBinding.CODEC)
                .setKeyFunction(ActivationBinding::getId)
                .loadsAfter(Activation.class)
                .build());
    }

    /** Call after assets are loaded to merge all binding assets into block type → activation id. */
    public static void onBindingsLoaded() {
        AssetStore<String, ActivationBinding, ? extends AssetMap<String, ActivationBinding>> store =
            AssetRegistry.getAssetStore(ActivationBinding.class);
        if (store == null) return;
        AssetMap<String, ActivationBinding> assetMap = store.getAssetMap();
        if (assetMap == null) return;
        Map<String, ActivationBinding> all = ((DefaultAssetMap<String, ActivationBinding>) assetMap).getAssetMap();
        blockToActivationId.clear();
        List<ActivationBinding> sorted = new ArrayList<>(all.values());
        sorted.sort(Comparator.comparing(ActivationBinding::getId));
        for (ActivationBinding binding : sorted) {
            for (Map.Entry<String, String> e : binding.getBindings().entrySet()) {
                blockToActivationId.put(e.getKey(), e.getValue());
            }
            if (binding.getDefaultActivation() != null) {
                defaultActivationId = binding.getDefaultActivation();
            }
        }
    }

    /** Returns activation id for a block type key (default if not bound). */
    @Nonnull
    public static String getActivationId(@Nonnull String blockTypeKey) {
        return blockToActivationId.getOrDefault(blockTypeKey, defaultActivationId);
    }

    /** Returns the merged bindings map (block type key → activation id). */
    @Nonnull
    public static Map<String, String> getBlockToActivationId() {
        return Collections.unmodifiableMap(blockToActivationId);
    }
}

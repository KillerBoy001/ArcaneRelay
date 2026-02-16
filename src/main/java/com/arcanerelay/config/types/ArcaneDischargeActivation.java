package com.arcanerelay.config.types;

import com.arcanerelay.components.ArcaneSection;
import com.arcanerelay.config.Activation;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.arcanerelay.core.activation.ArcaneActivationAccessor;
import com.arcanerelay.core.activation.ArcaneCachedAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ArcaneDischargeActivation extends Activation {
    public static final BuilderCodec<ArcaneDischargeActivation> CODEC = BuilderCodec.builder(
            ArcaneDischargeActivation.class,
            ArcaneDischargeActivation::new,
            Activation.ABSTRACT_CODEC
        )
        .documentation("Cycles charge states; sends signals when transitioning from fully charged to off.")
        .<Map<String, String>>appendInherited(
            new KeyedCodec<>("Changes", new MapCodec<>(Codec.STRING, HashMap::new)),
            (a, m) -> a.changes = m,
            a -> a.changes,
            (a, p) -> a.changes = p.changes
        )
        .documentation("State transition map: current state -> next state (e.g. Off->One, One->Two, ..., On->Off).")
        .add()
        .appendInherited(
            new KeyedCodec<>("MaxChargeState", Codec.STRING),
            (a, s) -> a.maxChargeState = s,
            a -> a.maxChargeState,
            (a, p) -> a.maxChargeState = p.maxChargeState
        )
        .documentation("Exact state that triggers signal (default: On). Ignored if MaxChargeStateSuffix is set.")
        .add()
        .appendInherited(
            new KeyedCodec<>("MaxChargeStateSuffix", Codec.STRING),
            (a, s) -> a.maxChargeStateSuffix = s,
            a -> a.maxChargeStateSuffix,
            (a, p) -> a.maxChargeStateSuffix = p.maxChargeStateSuffix
        )
        .documentation("If set (e.g. _All_On), any state ending with this suffix is considered max charge.")
        .add()
        .build();

    private Map<String, String> changes;
    private String maxChargeState = "On";
    @Nullable
    private String maxChargeStateSuffix;

    public ArcaneDischargeActivation() {
    }

    @Nullable
    public Map<String, String> getChanges() {
        return changes;
    }

    public void setChanges(@Nullable Map<String, String> changes) {
        this.changes = changes;
    }

    public String getMaxChargeState() {
        return maxChargeState;
    }

    public void setMaxChargeState(String maxChargeState) {
        this.maxChargeState = maxChargeState;
    }

    @Nullable
    public String getMaxChargeStateSuffix() {
        return maxChargeStateSuffix;
    }

    public void setMaxChargeStateSuffix(@Nullable String maxChargeStateSuffix) {
        this.maxChargeStateSuffix = maxChargeStateSuffix;
    }

    private boolean isMaxChargeState(String newState) {
        if (maxChargeStateSuffix != null && !maxChargeStateSuffix.isEmpty()) {
            return newState != null && newState.endsWith(maxChargeStateSuffix);
        }
        return maxChargeState != null && maxChargeState.equals(newState);
    }

    @Override
    public ArcaneSection.BlockTickStrategy execute(
        @Nonnull ArcaneCachedAccessor accessor,
        @Nullable Ref<ChunkStore> sectionRef,
        @Nullable Ref<ChunkStore> blockRef,
        int worldX, int worldY, int worldZ,
        @Nonnull List<int[]> sources
    ) {
        
        return ArcaneSection.BlockTickStrategy.PROCESSED;
    }
}

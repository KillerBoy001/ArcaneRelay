package com.arcanerelay.features.config;

import java.util.Arrays;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.validator.RangeValidator;

public class ArcaneRelayConfig {
    public static final BuilderCodec<ArcaneRelayConfig> CODEC = BuilderCodec
        .builder(ArcaneRelayConfig.class, ArcaneRelayConfig::new)
        .documentation("Configuration file for Arcane Relay.")

        .append(new KeyedCodec<Integer>("RelayDistance", Codec.INTEGER),
                (config, value, info) -> config.relayDistance = value,
                (config, info) -> config.relayDistance)
        .documentation("Maximum distance at which the Arcane Staff can succesfully create a connection.")
        .addValidator(new RangeValidator<Integer>(0, Integer.MAX_VALUE, false))
        .add()

        .append(new KeyedCodec<Integer>("PusherRange", Codec.INTEGER),
                (config, value, info) -> config.pusherRange = value,
                (config, info) -> config.pusherRange)
        .documentation("Maximum number of blocks the pusher is allowed to succesfully block before being blocked.")
        .addValidator(new RangeValidator<Integer>(0, Integer.MAX_VALUE, true))
        .add()

        .append(new KeyedCodec<Integer>("PullerRange", Codec.INTEGER),
                (config, value, info) -> config.pullerRange = value,
                (config, info) -> config.pullerRange)
        .documentation("The range at which the Puller will extend to before starting to contract.")
        .addValidator(new RangeValidator<Integer>(0, Integer.MAX_VALUE, false))
        .add()

        .append(new KeyedCodec<Float>("BreakerEntityDamage", Codec.FLOAT),
                (config, value, info) -> config.breakerEntityDamage = value,
                (config, info) -> config.breakerEntityDamage)
        .documentation("The damage dealt by the Breaker to entities.")
        .addValidator(new RangeValidator<Float>(0.0f, Float.MAX_VALUE, true))
        .add()

        .append(new KeyedCodec<Float>("BreakerBlockDamageScalar", Codec.FLOAT),
                (config, value, info) -> config.breakerBlockDamageScalar = value,
                (config, info) -> config.breakerBlockDamageScalar)
        .documentation("The damage scalar applied to the damage the Breaker does to blocks.")
        .addValidator(new RangeValidator<Float>(0.0f, Float.MAX_VALUE, true))
        .add()

        .append(new KeyedCodec<String[]>("MoveableBlacklist", Codec.STRING_ARRAY),
                (config, value, info) -> config.noneMoveableBlocks = value,
                (config, info) -> config.noneMoveableBlocks)
        .documentation("List of blocks that cannot be moved.")
        .add()

        .append(new KeyedCodec<String[]>("RotatableBlacklist", Codec.STRING_ARRAY),
                (config, value, info) -> config.noneRotatableBlocks = value,
                (config, info) -> config.noneRotatableBlocks)
        .documentation("List of blocks that cannot be rotated.")
        .add()

        .build();

    private int relayDistance = 10;
    private int pusherRange = 15;
    private int pullerRange = 15;
    private float breakerEntityDamage = 5.0f;
    private float breakerBlockDamageScalar = 1.0f;
    private String[] noneMoveableBlocks = { "Barrier", "Bedrock" };
    private String[] noneRotatableBlocks = { "Bench", "Bed", "Barrier", "Bedrock" };

    public void resetToDefaults() {
        this.relayDistance = 10;
        this.pusherRange = 15;
        this.pullerRange = 15;
        this.breakerEntityDamage = 5.0f;
        this.breakerBlockDamageScalar = 1.0f;
        this.noneMoveableBlocks = new String[] { "Barrier", "Bedrock" };
        this.noneRotatableBlocks = new String[] { "Bench", "Bed", "Barrier", "Bedrock" };
    }

    public ArcaneRelayConfig() {
    }

    public ArcaneRelayConfig(ArcaneRelayConfig other) {
        this.relayDistance = other.relayDistance;
        this.pusherRange = other.pusherRange;
        this.pullerRange = other.pullerRange;
        this.breakerEntityDamage = other.breakerEntityDamage;
        this.breakerBlockDamageScalar = other.breakerBlockDamageScalar;
        this.noneMoveableBlocks = other.noneMoveableBlocks.clone();
        this.noneRotatableBlocks = other.noneRotatableBlocks.clone();
    }

    public void copyFrom(ArcaneRelayConfig other) {
        this.relayDistance = other.relayDistance;
        this.pusherRange = other.pusherRange;
        this.pullerRange = other.pullerRange;
        this.breakerEntityDamage = other.breakerEntityDamage;
        this.breakerBlockDamageScalar = other.breakerBlockDamageScalar;
        this.noneMoveableBlocks = other.noneMoveableBlocks.clone();
        this.noneRotatableBlocks = other.noneRotatableBlocks.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ArcaneRelayConfig that = (ArcaneRelayConfig) o;
        return relayDistance == that.relayDistance &&
            pusherRange == that.pusherRange &&
            pullerRange == that.pullerRange &&
            Float.compare(that.breakerEntityDamage, breakerEntityDamage) == 0 &&
            Float.compare(that.breakerBlockDamageScalar, breakerBlockDamageScalar) == 0 &&
            Arrays.equals(noneMoveableBlocks, that.noneMoveableBlocks) &&
            Arrays.equals(noneRotatableBlocks, that.noneRotatableBlocks);
    }

    // Getters
    public int getRelayDistance() {
        return relayDistance;
    }

    public int getPusherRange() {
        return pusherRange;
    }

    public int getPullerRange() {
        return pullerRange;
    }

    public float getBreakerEntityDamage() {
        return breakerEntityDamage;
    }

    public float getBreakerBlockDamageScalar() {
        return breakerBlockDamageScalar;
    }

    public String[] getNoneMoveableBlocks() {
        return noneMoveableBlocks;
    }

    public String[] getNoneRotatableBlocks() {
        return noneRotatableBlocks;
    }

    public void setRelayDistance(int relayDistance) { this.relayDistance = relayDistance; }
    public void setPusherRange(int pusherRange) { this.pusherRange = pusherRange; }
    public void setPullerRange(int pullerRange) { this.pullerRange = pullerRange; }
    public void setBreakerEntityDamage(float breakerEntityDamage) { this.breakerEntityDamage = breakerEntityDamage; }
    public void setBreakerBlockDamageScalar(float breakerBlockDamageScalar) { this.breakerBlockDamageScalar = breakerBlockDamageScalar; }
    public void setNoneMoveableBlocks(String[] noneMoveableBlocks) { this.noneMoveableBlocks = noneMoveableBlocks; }
    public void setNoneRotatableBlocks(String[] noneRotatableBlocks) { this.noneRotatableBlocks = noneRotatableBlocks; }
}
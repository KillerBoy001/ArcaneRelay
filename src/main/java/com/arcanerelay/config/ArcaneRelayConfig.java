package com.arcanerelay.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.validator.RangeValidator;

public class ArcaneRelayConfig {
    public static final BuilderCodec<ArcaneRelayConfig> CODEC = BuilderCodec
            .builder(ArcaneRelayConfig.class, ArcaneRelayConfig::new)
            .documentation("Configuration file for Arcane Relay.")

            .append(new KeyedCodec<Integer>("TriggerDistance", Codec.INTEGER),
                    (config, value, info) -> config.triggerDistance = value,
                    (config, info) -> config.triggerDistance)
            .documentation("Maximum distance at which a Arcane block can trigger a connection")
            .addValidator(new RangeValidator<Integer>(0, Integer.MAX_VALUE, false))
            .add()

            .append(new KeyedCodec<Integer>("TargetDistance", Codec.INTEGER),
                    (config, value, info) -> config.targetDistance = value,
                    (config, info) -> config.targetDistance)
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

            .append(new KeyedCodec<Double>("BreakerDamage", Codec.DOUBLE),
                    (config, value, info) -> config.breakerDamage = value,
                    (config, info) -> config.breakerDamage)
            .documentation("The damage dealt by the Breaker.")
            .addValidator(new RangeValidator<Double>(0.0, Double.MAX_VALUE, true))
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

    private int triggerDistance = 10;
    private int targetDistance = 10;
    private int pusherRange = 15;
    private int pullerRange = 15;
    private double breakerDamage = 0.3;
    private String[] noneMoveableBlocks = { "Void_Suspender", "Barrier", "Bedrock" };
    private String[] noneRotatableBlocks = { "Void_Suspender", "Soil_Grass", "Bench", "Bed", "Rotator", "Barrier",
            "Bedrock" };

    public ArcaneRelayConfig() {
    }

    // Getters
    public int getTriggerDistance() {
        return triggerDistance;
    }

    public int getTargetDistance() {
        return targetDistance;
    }

    public int getPusherRange() {
        return pusherRange;
    }

    public int getPullerRange() {
        return pullerRange;
    }

    public double getBreakerDamage() {
        return breakerDamage;
    }

    public String[] getNoneMoveableBlocks() {
        return noneMoveableBlocks;
    }

    public String[] getNoneRotatableBlocks() {
        return noneRotatableBlocks;
    }

    public void setTriggerDistance(int triggerDistance) { this.triggerDistance = triggerDistance; }
    public void setTargetDistance(int targetDistance) { this.targetDistance = targetDistance; }
    public void setPusherRange(int pusherRange) { this.pusherRange = pusherRange; }
    public void setPullerRange(int pullerRange) { this.pullerRange = pullerRange; }
    public void setBreakerDamage(double breakerDamage) { this.breakerDamage = breakerDamage; }
    public void setNoneMoveableBlocks(String[] noneMoveableBlocks) { this.noneMoveableBlocks = noneMoveableBlocks; }
    public void setNoneRotatableBlocks(String[] noneRotatableBlocks) { this.noneRotatableBlocks = noneRotatableBlocks; }
}
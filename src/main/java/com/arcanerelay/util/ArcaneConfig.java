package com.arcanerelay.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.util.Config;

import java.util.List;

public class ArcaneConfig {
    public static final BuilderCodec<ArcaneConfig> CODEC =
            BuilderCodec.builder(ArcaneConfig.class, ArcaneConfig::new)

                    .append(new KeyedCodec<Double>("TriggerDistance", Codec.DOUBLE),
                            (config, value, info) -> config.TriggerDistance = value,
                            (config, info) -> config.TriggerDistance)
                    .add()

                    .append(new KeyedCodec<Double>("TargetDistance", Codec.DOUBLE),
                            (config, value, info) -> config.TargetDistance = value,
                            (config, info) -> config.TargetDistance)
                    .add()

                    .append(new KeyedCodec<Integer>("PusherRange", Codec.INTEGER),
                            (config, value, info) -> config.PusherRange = value,
                            (config, info) -> config.PusherRange)
                    .add()

                    .append(new KeyedCodec<Integer>("PullerRange", Codec.INTEGER),
                            (config, value, info) -> config.PullerRange = value,
                            (config, info) -> config.PullerRange)
                    .add()

                    .append(new KeyedCodec<Double>("BreakerDamage", Codec.DOUBLE),
                            (config, value, info) -> config.BreakerDamage = value,
                            (config, info) -> config.BreakerDamage)
                    .add()

                    .append(new KeyedCodec<String[]>("MoveableBlacklist", Codec.STRING_ARRAY),
                            (config, value, info) -> config.NoneMoveableBlocks = value,
                            (config, info) -> config.NoneMoveableBlocks)
                    .add()

                    .append(new KeyedCodec<String[]>("RotatableBlacklist", Codec.STRING_ARRAY),
                            (config, value, info) -> config.NoneRotatableBlocks = value,
                            (config, info) -> config.NoneRotatableBlocks)

                    .add()

                    .build();

    private Double TriggerDistance = 10.0;
    private Double TargetDistance = 10.0;
    private int PusherRange = 15;
    private int PullerRange = 15;
    private Double BreakerDamage = 25.0;
    private String[] NoneMoveableBlocks = {"Barrier", "Bedrock"};
    private String[] NoneRotatableBlocks= {"Soil_Grass", "Bench", "Bed", "Rotator", "Barrier", "Bedrock"};

    public ArcaneConfig() {
    }
    // Getters
    public Double getTriggerDistance() {return TriggerDistance;}
    public Double getTargetDistance() {return TargetDistance;}
    public int getPusherRange() { return PusherRange; }
    public int getPullerRange() { return PullerRange; }
    public Double getBreakerDamage() { return BreakerDamage; }
    public String[] getNoneMoveableBlocks() { return NoneMoveableBlocks; }
    public String[] getNoneRotatableBlocks() { return NoneRotatableBlocks; }

    // Setters
    public void setTriggerDistance(Double TriggerDistance) {this.TriggerDistance = TriggerDistance;}
    public void setTargetDistance(Double TargetDistance) {this.TargetDistance = TargetDistance;}
    public void setPusherRange(int PusherRange) {this.PusherRange = PusherRange;}
    public void setPullerRange(int PullerRange) {this.PullerRange = PullerRange;}
    public void setBreakerDamage(Double BreakerDamage) {this.BreakerDamage = BreakerDamage;}
    public void setNoneMoveableBlocks(String[] NotMoveableBlocks) {this.NoneMoveableBlocks = NotMoveableBlocks;}
    public void setNoneRotatableBlocks(String[] NotRotatableBlocks) {this.NoneRotatableBlocks = NotRotatableBlocks;}
}

package com.arcanerelay.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

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

                    .append(new KeyedCodec<String[]>("NotMoveableBlocks", Codec.STRING_ARRAY),
                            (config, value, info) -> config.NotMoveableBlocks = value,
                            (config, info) -> config.NotMoveableBlocks)
                    .add()

                    .append(new KeyedCodec<String[]>("NotRotateableBlocks", Codec.STRING_ARRAY),
                            (config, value, info) -> config.NotRotateableBlocks = value,
                            (config, info) -> config.NotRotateableBlocks)
                    .add()

                    .build();

    private Double TriggerDistance = 10.0;
    private Double TargetDistance = 10.0;
    private int PusherRange = 15;
    private int PullerRange = 15;
    private String[] NotMoveableBlocks = {"Void_Suspender", "Barrier", "Bedrock"};
    private String[] NotRotateableBlocks = {"Void_Suspender", "Soil_Grass", "Bench", "Bed", "Rotator", "Barrier", "Bedrock"};

    public ArcaneConfig() {
    }
    // Getters
    public Double getTriggerDistance() {return TriggerDistance;}
    public Double getTargetDistance() {return TargetDistance;}
    public int getPusherRange() { return PusherRange; }
    public int getPullerRange() { return PullerRange; }
    public String[] getNotMoveableBlocks() { return NotMoveableBlocks; }
    public String[] getNotRotateableBlocks() { return NotRotateableBlocks; }
}

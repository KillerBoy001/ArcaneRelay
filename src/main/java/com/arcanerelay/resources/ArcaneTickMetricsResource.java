package com.arcanerelay.state;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Resource that collects arcane tick metrics from systems (possibly in parallel)
 * and aggregates them once per tick when {@link #flushMetrics()} is called.
 */
public final class ArcaneTickMetricsResource implements Resource<ChunkStore> {

    public static ResourceType<ChunkStore, ArcaneTickMetricsResource> getResourceType() {
        return ArcaneRelayPlugin.get().getArcaneTickMetricsResourceType();
    }

    @Nonnull
    private final HistoricMetric arcaneTicksProcessedPerTickMetricSet = HistoricMetric.builder(33333333L, TimeUnit.NANOSECONDS)
        .addPeriod(1L, TimeUnit.SECONDS)
        .addPeriod(1L, TimeUnit.MINUTES)
        .addPeriod(5L, TimeUnit.MINUTES)
        .build();

    private final AtomicInteger arcaneTicksProcessedAccumulator = new AtomicInteger(0);

    /**
     * Record a number of arcane ticks processed. Safe to call from parallel workers.
     * Values are aggregated when {@link #flushMetrics()} is called after each tick.
     */
    public void recordArcaneTicksProcessed(int count) {
        arcaneTicksProcessedAccumulator.addAndGet(count);
    }

    /**
     * Aggregate accumulated values into the historic metric and reset. Call once per tick
     * from a single-threaded system after all metric-producing systems have run.
     */
    public void flushMetrics() {
        int total = arcaneTicksProcessedAccumulator.getAndSet(0);
        long ts = System.nanoTime();
        arcaneTicksProcessedPerTickMetricSet.add(ts, total);
    }

    @Nonnull
    public HistoricMetric getArcaneTicksProcessedPerTickMetricSet() {
        return arcaneTicksProcessedPerTickMetricSet;
    }

    @Override
    public Resource<ChunkStore> clone() {
        return new ArcaneTickMetricsResource();
    }
}

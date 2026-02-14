package com.arcanerelay.state;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import com.arcanerelay.ArcaneRelayPlugin;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Resource that throttles arcane block processing to a fixed interval (e.g. 250ms).
 * Updated once per world tick by {@link ArcaneSystems.TickRateLimit}; {@link ArcaneSystems.Ticking}
 * checks {@link #isProcessingAllowed()} and skips processing when the interval has not elapsed.
 */
public final class ArcaneTickSchedule implements Resource<ChunkStore> {

    public static final long DEFAULT_INTERVAL_MS = 250L;

    public static ResourceType<ChunkStore, ArcaneTickSchedule> getResourceType() {
        return ArcaneRelayPlugin.get().getArcaneTickScheduleResourceType();
    }

    private Instant lastProcessed = Instant.EPOCH;
    private final long intervalMs;
    private boolean allowProcessing;

    public ArcaneTickSchedule() {
        this(DEFAULT_INTERVAL_MS);
    }

    public ArcaneTickSchedule(long intervalMs) {
        this.intervalMs = intervalMs > 0 ? intervalMs : DEFAULT_INTERVAL_MS;
    }

    /**
     * Call once per world tick (from TickRateLimit). Updates lastProcessed when the interval
     * has elapsed and sets allowProcessing for this tick.
     */
    public void update(@Nonnull Instant gameTime) {
        long elapsedMs = Duration.between(lastProcessed, gameTime).toMillis();
        allowProcessing = elapsedMs >= intervalMs;
        if (allowProcessing) {
            lastProcessed = gameTime;
        }
    }

    public boolean isProcessingAllowed() {
        return allowProcessing;
    }

    @Override
    public Resource<ChunkStore> clone() {
        return new ArcaneTickSchedule(intervalMs);
    }
}

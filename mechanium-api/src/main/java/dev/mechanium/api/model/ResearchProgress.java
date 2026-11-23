package dev.mechanium.api.model;

import java.util.Locale;
import java.util.Objects;

/** Persisted timer for one technology research job. Times are Unix epoch milliseconds. */
public record ResearchProgress(String technologyId, long startedAtEpochMillis, long completesAtEpochMillis) {
    public ResearchProgress {
        Objects.requireNonNull(technologyId, "technologyId");
        technologyId = technologyId.trim().toLowerCase(Locale.ROOT);
        if (!technologyId.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
            throw new IllegalArgumentException("Invalid technologyId: " + technologyId);
        }
        if (startedAtEpochMillis < 0L || completesAtEpochMillis <= startedAtEpochMillis) {
            throw new IllegalArgumentException("Research timestamps are invalid.");
        }
    }

    public long durationMillis() {
        return completesAtEpochMillis - startedAtEpochMillis;
    }

    public long remainingMillis(long nowEpochMillis) {
        return nowEpochMillis >= completesAtEpochMillis ? 0L : completesAtEpochMillis - nowEpochMillis;
    }

    public double progressFraction(long nowEpochMillis) {
        if (nowEpochMillis <= startedAtEpochMillis) return 0.0d;
        if (nowEpochMillis >= completesAtEpochMillis) return 1.0d;
        return (double) (nowEpochMillis - startedAtEpochMillis) / (double) durationMillis();
    }
}

package dev.mechanium.plugin;

import java.util.HashMap;
import java.util.Map;

/** Owner-scoped energy counters accumulated since this plugin instance started. */
final class EnergyTelemetry {
    private final Map<String, Snapshot> totals = new HashMap<>();

    void recordStored(String ownerId, long amount) {
        record(ownerId, amount, 0);
    }

    void recordConsumed(String ownerId, long amount) {
        record(ownerId, 0, amount);
    }

    Snapshot snapshot(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Owner ID cannot be blank");
        }
        return totals.getOrDefault(ownerId, new Snapshot(0, 0));
    }

    private void record(String ownerId, long stored, long consumed) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Owner ID cannot be blank");
        }
        if (stored < 0 || consumed < 0) {
            throw new IllegalArgumentException("Energy telemetry increments cannot be negative");
        }
        Snapshot current = snapshot(ownerId);
        totals.put(ownerId, new Snapshot(
                Math.addExact(current.storedUnits(), stored),
                Math.addExact(current.consumedUnits(), consumed)));
    }

    record Snapshot(long storedUnits, long consumedUnits) { }
}

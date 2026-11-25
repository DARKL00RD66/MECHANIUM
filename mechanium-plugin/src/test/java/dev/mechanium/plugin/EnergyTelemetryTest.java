package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnergyTelemetryTest {
    @Test
    void accumulatesStoredAndConsumedEnergyPerOwner() {
        EnergyTelemetry telemetry = new EnergyTelemetry();
        telemetry.recordStored("player-a", 8);
        telemetry.recordStored("player-a", 2);
        telemetry.recordConsumed("player-a", 3);

        assertEquals(new EnergyTelemetry.Snapshot(10, 3), telemetry.snapshot("player-a"));
    }

    @Test
    void keepsDifferentOwnersIsolated() {
        EnergyTelemetry telemetry = new EnergyTelemetry();
        telemetry.recordStored("player-a", 10);
        telemetry.recordConsumed("player-b", 4);

        assertEquals(new EnergyTelemetry.Snapshot(10, 0), telemetry.snapshot("player-a"));
        assertEquals(new EnergyTelemetry.Snapshot(0, 4), telemetry.snapshot("player-b"));
    }

    @Test
    void unknownOwnerHasZeroCounters() {
        assertEquals(new EnergyTelemetry.Snapshot(0, 0),
                new EnergyTelemetry().snapshot("new-player"));
    }

    @Test
    void rejectsInvalidOwnersAndNegativeIncrements() {
        EnergyTelemetry telemetry = new EnergyTelemetry();
        assertThrows(IllegalArgumentException.class, () -> telemetry.snapshot(""));
        assertThrows(IllegalArgumentException.class, () -> telemetry.recordStored(null, 1));
        assertThrows(IllegalArgumentException.class, () -> telemetry.recordStored("owner", -1));
        assertThrows(IllegalArgumentException.class, () -> telemetry.recordConsumed("owner", -1));
    }

    @Test
    void rejectsCounterOverflowInsteadOfWrapping() {
        EnergyTelemetry telemetry = new EnergyTelemetry();
        telemetry.recordStored("owner", Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> telemetry.recordStored("owner", 1));
        assertEquals(Long.MAX_VALUE, telemetry.snapshot("owner").storedUnits());
    }
}

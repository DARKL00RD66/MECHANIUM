package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnergyAllocationPolicyTest {
    @Test
    void consumesAcrossMultipleBatteriesWhenNoSingleBatteryHasEnough() {
        assertEquals(List.of(0, 1, 4),
                EnergyAllocationPolicy.planConsumption(List.of(2, 3, 4), 4).orElseThrow());
    }

    @Test
    void insufficientChargeDoesNotReturnAPartialDebitPlan() {
        assertTrue(EnergyAllocationPolicy.planConsumption(List.of(1, 2, 3), 7).isEmpty());
        assertEquals(List.of(1, 2, 3),
                EnergyAllocationPolicy.planConsumption(List.of(1, 2, 3), 0).orElseThrow());
    }

    @Test
    void distributesGenerationWithoutExceedingBatteryCapacity() {
        assertEquals(List.of(10, 10, 6),
                EnergyAllocationPolicy.planGeneration(List.of(8, 9, 0), 9, 10));
    }

    @Test
    void generationNeverCreatesEnergyWithoutStorageCapacity() {
        assertEquals(List.of(10, 10),
                EnergyAllocationPolicy.planGeneration(List.of(10, 10), 20, 10));
    }

    @Test
    void rejectsInvalidAmountsAndChargeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> EnergyAllocationPolicy.planConsumption(List.of(1, -1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> EnergyAllocationPolicy.planConsumption(List.of(1), -1));
        assertThrows(IllegalArgumentException.class,
                () -> EnergyAllocationPolicy.planGeneration(List.of(0), -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> EnergyAllocationPolicy.planGeneration(List.of(11), 1, 10));
    }
}

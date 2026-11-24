package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClimateGrowthPolicyTest {
    @Test
    void growthRangeUsesThreeDimensionalDistance() {
        assertTrue(ClimateGrowthPolicy.withinRange(0, 64, 0, 3, 64, 0, 4));
        assertTrue(ClimateGrowthPolicy.withinRange(0, 64, 0, 2, 66, 2, 4));
        assertFalse(ClimateGrowthPolicy.withinRange(0, 64, 0, 4, 65, 0, 4));
    }

    @Test
    void growthBonusAdvancesOneStageAndNeverExceedsMaximumAge() {
        assertEquals(3, ClimateGrowthPolicy.boostedAge(2, 7, 1));
        assertEquals(7, ClimateGrowthPolicy.boostedAge(6, 7, 3));
        assertEquals(7, ClimateGrowthPolicy.boostedAge(7, 7, 1));
        assertEquals(2, ClimateGrowthPolicy.boostedAge(2, 7, 0));
    }

    @Test
    void rejectsInvalidGrowthInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> ClimateGrowthPolicy.withinRange(0, 0, 0, 1, 0, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> ClimateGrowthPolicy.boostedAge(1, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> ClimateGrowthPolicy.boostedAge(1, 3, -1));
        assertThrows(IllegalArgumentException.class,
                () -> ClimateGrowthPolicy.boostedAge(4, 3, 1));
    }
}

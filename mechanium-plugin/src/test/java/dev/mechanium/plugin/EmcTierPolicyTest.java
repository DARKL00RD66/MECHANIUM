package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmcTierPolicyTest {
    @Test
    void eachTierHasAnIncreasingItemValueLimit() {
        assertEquals(16L, EmcTierPolicy.maximumItemValue(1));
        assertEquals(64L, EmcTierPolicy.maximumItemValue(2));
        assertEquals(256L, EmcTierPolicy.maximumItemValue(3));
        assertEquals(1_024L, EmcTierPolicy.maximumItemValue(4));
        assertEquals(4_096L, EmcTierPolicy.maximumItemValue(5));
    }

    @Test
    void tierAcceptsOnlyKnownPositiveItemsWithinItsLimit() {
        assertTrue(EmcTierPolicy.canProcess(1, 16L));
        assertFalse(EmcTierPolicy.canProcess(1, 17L));
        assertTrue(EmcTierPolicy.canProcess(5, 4_096L));
        assertFalse(EmcTierPolicy.canProcess(5, 4_097L));
        assertFalse(EmcTierPolicy.canProcess(3, 0L));
        assertFalse(EmcTierPolicy.canProcess(3, -1L));
    }

    @Test
    void conversionTotalUsesCheckedLongArithmetic() {
        assertEquals(256L, EmcTierPolicy.totalValue(16L, 16));
        assertThrows(ArithmeticException.class,
                () -> EmcTierPolicy.totalValue(Long.MAX_VALUE, 2));
    }

    @Test
    void invalidTierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EmcTierPolicy.maximumItemValue(0));
        assertThrows(IllegalArgumentException.class, () -> EmcTierPolicy.maximumItemValue(6));
    }
}

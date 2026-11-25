package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialTransferPolicyTest {
    @Test
    void capsBatchBySourceStackConfiguredLimitAndMinecraftStackLimit() {
        assertEquals(16, IndustrialTransferPolicy.batchSize(64, 64, 16));
        assertEquals(8, IndustrialTransferPolicy.batchSize(8, 64, 16));
        assertEquals(32, IndustrialTransferPolicy.batchSize(64, 32, 48));
        assertEquals(64, IndustrialTransferPolicy.batchSize(100, 99, 100));
    }

    @Test
    void returnsZeroForAnEmptySource() {
        assertEquals(0, IndustrialTransferPolicy.batchSize(0, 64, 16));
    }

    @Test
    void deductsOnlyItemsActuallyInsertedIntoTheTarget() {
        var result = IndustrialTransferPolicy.afterInsertion(64, 16, 5);
        assertEquals(53, result.sourceRemaining());
        assertEquals(11, result.insertedAmount());
    }

    @Test
    void preservesSourceWhenTargetAcceptsNothing() {
        var result = IndustrialTransferPolicy.afterInsertion(24, 16, 16);
        assertEquals(24, result.sourceRemaining());
        assertEquals(0, result.insertedAmount());
    }

    @Test
    void emptiesSourceWhenEntireStackWasInserted() {
        var result = IndustrialTransferPolicy.afterInsertion(16, 16, 0);
        assertEquals(0, result.sourceRemaining());
        assertEquals(16, result.insertedAmount());
    }

    @Test
    void rejectsInvalidBatchAndInsertionAccounting() {
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialTransferPolicy.batchSize(-1, 64, 16));
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialTransferPolicy.batchSize(10, 0, 16));
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialTransferPolicy.batchSize(10, 64, 0));
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialTransferPolicy.afterInsertion(10, 11, 0));
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialTransferPolicy.afterInsertion(10, 5, 6));
    }
}

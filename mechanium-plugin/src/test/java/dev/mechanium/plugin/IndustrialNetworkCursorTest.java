package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialNetworkCursorTest {
    @Test
    void sourceStartRotatesAfterSuccessfulTransfer() {
        IndustrialNetworkCursor cursor = new IndustrialNetworkCursor();
        assertEquals(0, cursor.start(9));
        cursor.advance(9, 0, 3, 1);
        assertEquals(1, cursor.start(9));
        cursor.advance(9, 7, 3, 1);
        assertEquals(8, cursor.start(9));
        cursor.advance(9, 8, 3, 1);
        assertEquals(0, cursor.start(9));
    }

    @Test
    void destinationOffsetRotatesWithoutSelectingSourceCrate() {
        IndustrialNetworkCursor cursor = new IndustrialNetworkCursor();
        assertEquals(1, cursor.targetOffset(4));
        cursor.advance(12, 0, 4, 1);
        assertEquals(2, cursor.targetOffset(4));
        cursor.advance(12, 1, 4, 2);
        assertEquals(3, cursor.targetOffset(4));
        cursor.advance(12, 2, 4, 3);
        assertEquals(1, cursor.targetOffset(4));
    }

    @Test
    void blockedCycleStillRotatesBothCursors() {
        IndustrialNetworkCursor cursor = new IndustrialNetworkCursor();
        cursor.rotateAfterBlockedCycle(6, 3);
        assertEquals(1, cursor.start(6));
        assertEquals(2, cursor.targetOffset(3));
    }

    @Test
    void rejectsInvalidNetworkSizesAndCursorPositions() {
        IndustrialNetworkCursor cursor = new IndustrialNetworkCursor();
        assertThrows(IllegalArgumentException.class, () -> cursor.start(0));
        assertThrows(IllegalArgumentException.class, () -> cursor.targetOffset(1));
        assertThrows(IllegalArgumentException.class, () -> cursor.advance(4, 4, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> cursor.advance(4, 0, 2, 0));
    }
}

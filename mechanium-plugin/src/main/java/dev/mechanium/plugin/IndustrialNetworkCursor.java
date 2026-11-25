package dev.mechanium.plugin;

/**
 * Keeps a fair starting point for source slots and destination crates between
 * industrial-network transfer cycles.
 */
final class IndustrialNetworkCursor {
    private int nextSourceSlot;
    private int nextTargetOffset = 1;

    int start(int totalSlots) {
        if (totalSlots < 1) throw new IllegalArgumentException("A network must have at least one source slot");
        nextSourceSlot = Math.floorMod(nextSourceSlot, totalSlots);
        return nextSourceSlot;
    }

    int targetOffset(int crateCount) {
        if (crateCount < 2) throw new IllegalArgumentException("A network needs at least two crates");
        int possibleOffsets = crateCount - 1;
        nextTargetOffset = 1 + Math.floorMod(nextTargetOffset - 1, possibleOffsets);
        return nextTargetOffset;
    }

    void advance(int totalSlots, int processedSourceSlot, int crateCount, int processedTargetOffset) {
        if (totalSlots < 1 || processedSourceSlot < 0 || processedSourceSlot >= totalSlots) {
            throw new IllegalArgumentException("Invalid source slot cursor");
        }
        if (crateCount < 2 || processedTargetOffset < 1 || processedTargetOffset >= crateCount) {
            throw new IllegalArgumentException("Invalid target crate cursor");
        }
        nextSourceSlot = (processedSourceSlot + 1) % totalSlots;
        nextTargetOffset = 1 + (processedTargetOffset % (crateCount - 1));
    }

    void rotateAfterBlockedCycle(int totalSlots, int crateCount) {
        int current = start(totalSlots);
        int target = targetOffset(crateCount);
        advance(totalSlots, current, crateCount, target);
    }
}

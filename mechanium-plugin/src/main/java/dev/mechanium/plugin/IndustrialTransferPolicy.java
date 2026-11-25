package dev.mechanium.plugin;

/** Pure transfer sizing and accounting for industrial item conduits. */
final class IndustrialTransferPolicy {
    private IndustrialTransferPolicy() { }

    static int batchSize(int sourceAmount, int stackLimit, int configuredLimit) {
        if (sourceAmount < 0) throw new IllegalArgumentException("Source amount cannot be negative");
        if (stackLimit < 1) throw new IllegalArgumentException("Stack limit must be positive");
        if (configuredLimit < 1) throw new IllegalArgumentException("Transfer limit must be positive");
        if (sourceAmount == 0) return 0;
        return Math.min(sourceAmount, Math.min(64, Math.min(stackLimit, configuredLimit)));
    }

    static TransferResult afterInsertion(int sourceAmount, int requestedAmount, int leftoverAmount) {
        if (sourceAmount < 0) throw new IllegalArgumentException("Source amount cannot be negative");
        if (requestedAmount < 0 || requestedAmount > sourceAmount) {
            throw new IllegalArgumentException("Requested amount must be within the source stack");
        }
        if (leftoverAmount < 0 || leftoverAmount > requestedAmount) {
            throw new IllegalArgumentException("Leftover amount must be within the requested batch");
        }
        int inserted = requestedAmount - leftoverAmount;
        return new TransferResult(sourceAmount - inserted, inserted);
    }

    record TransferResult(int sourceRemaining, int insertedAmount) { }
}

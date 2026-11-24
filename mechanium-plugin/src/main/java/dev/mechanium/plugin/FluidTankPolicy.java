package dev.mechanium.plugin;

/** Pure capacity, mixing and transfer rules for typed MECHANIUM fluids. */
final class FluidTankPolicy {
    private FluidTankPolicy() { }

    static FillResult fill(FluidTankContents current, String fluidId, int amountMb, int capacityMb) {
        if (current == null) throw new IllegalArgumentException("Current tank state cannot be null");
        if (fluidId == null || fluidId.isBlank()) throw new IllegalArgumentException("Fluid ID cannot be blank");
        if (amountMb < 0) throw new IllegalArgumentException("Fill amount cannot be negative");
        if (capacityMb < 1) throw new IllegalArgumentException("Tank capacity must be positive");
        if (!current.isEmpty() && !current.fluidId().equalsIgnoreCase(fluidId)) {
            return new FillResult(current, 0);
        }

        int accepted = Math.min(amountMb, capacityMb - current.amountMb());
        if (accepted <= 0) return new FillResult(current, 0);
        return new FillResult(new FluidTankContents(fluidId, current.amountMb() + accepted), accepted);
    }

    static DrainResult drain(FluidTankContents current, int amountMb) {
        if (current == null) throw new IllegalArgumentException("Current tank state cannot be null");
        if (amountMb < 0) throw new IllegalArgumentException("Drain amount cannot be negative");
        int removed = Math.min(current.amountMb(), amountMb);
        if (removed == 0) return new DrainResult(current, 0);
        int remaining = current.amountMb() - removed;
        return new DrainResult(remaining == 0
                ? FluidTankContents.empty()
                : new FluidTankContents(current.fluidId(), remaining), removed);
    }

    static TransferResult transfer(FluidTankContents source, FluidTankContents target,
                                   int requestedMb, int targetCapacityMb) {
        if (source == null || target == null) throw new IllegalArgumentException("Tank states cannot be null");
        if (requestedMb < 0) throw new IllegalArgumentException("Transfer amount cannot be negative");
        if (source.isEmpty() || requestedMb == 0) {
            return new TransferResult(source, target, 0);
        }

        FillResult fill = fill(target, source.fluidId(),
                Math.min(source.amountMb(), requestedMb), targetCapacityMb);
        if (fill.acceptedMb() == 0) return new TransferResult(source, target, 0);

        DrainResult drain = drain(source, fill.acceptedMb());
        return new TransferResult(drain.contents(), fill.contents(), fill.acceptedMb());
    }

    record FillResult(FluidTankContents contents, int acceptedMb) { }
    record DrainResult(FluidTankContents contents, int removedMb) { }
    record TransferResult(FluidTankContents source, FluidTankContents target, int transferredMb) { }
}

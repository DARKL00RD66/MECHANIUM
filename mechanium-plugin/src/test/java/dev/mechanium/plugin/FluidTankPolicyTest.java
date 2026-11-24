package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluidTankPolicyTest {
    @Test
    void fillsEmptyAndMatchingFluidWithoutExceedingCapacity() {
        var first = FluidTankPolicy.fill(FluidTankContents.empty(), "purified_water", 1_000, 2_000);
        assertEquals(1_000, first.acceptedMb());
        assertEquals(new FluidTankContents("purified_water", 1_000), first.contents());

        var second = FluidTankPolicy.fill(first.contents(), "purified_water", 1_500, 2_000);
        assertEquals(1_000, second.acceptedMb());
        assertEquals(new FluidTankContents("purified_water", 2_000), second.contents());
    }

    @Test
    void rejectsMixingDifferentFluidTypes() {
        var existing = new FluidTankContents("purified_water", 1_000);
        var attempt = FluidTankPolicy.fill(existing, "industrial_coolant", 250, 10_000);
        assertEquals(0, attempt.acceptedMb());
        assertEquals(existing, attempt.contents());
    }

    @Test
    void drainsAndClearsFluidIdentityWhenTankBecomesEmpty() {
        var drained = FluidTankPolicy.drain(new FluidTankContents("purified_water", 500), 500);
        assertEquals(500, drained.removedMb());
        assertTrue(drained.contents().isEmpty());
        assertNull(drained.contents().fluidId());
    }

    @Test
    void transfersOnlyAvailableAmountAndTargetCapacity() {
        var plan = FluidTankPolicy.transfer(
                new FluidTankContents("purified_water", 750),
                new FluidTankContents("purified_water", 900),
                500, 1_000);
        assertEquals(100, plan.transferredMb());
        assertEquals(new FluidTankContents("purified_water", 650), plan.source());
        assertEquals(new FluidTankContents("purified_water", 1_000), plan.target());
    }

    @Test
    void rejectsTransferIntoAnotherFluidOrFullTank() {
        var source = new FluidTankContents("purified_water", 1_000);
        var mixed = FluidTankPolicy.transfer(source,
                new FluidTankContents("industrial_coolant", 100), 250, 2_000);
        assertEquals(0, mixed.transferredMb());
        assertEquals(source, mixed.source());

        var full = FluidTankPolicy.transfer(source,
                new FluidTankContents("purified_water", 2_000), 250, 2_000);
        assertEquals(0, full.transferredMb());
        assertEquals(source, full.source());
    }

    @Test
    void rejectsInvalidFluidVolumesAndCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new FluidTankContents("purified_water", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new FluidTankContents(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> FluidTankPolicy.fill(FluidTankContents.empty(), "water", 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> FluidTankPolicy.drain(FluidTankContents.empty(), -1));
    }
}

package dev.mechanium.plugin;

import dev.mechanium.api.model.FluidContainerDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void preservesExistingVolumeWhenConfiguredCapacityIsReduced() {
        var existing = new FluidTankContents("purified_water", 20_000);
        var attempt = FluidTankPolicy.fill(existing, "purified_water", 250, 16_000);

        assertEquals(0, attempt.acceptedMb());
        assertEquals(existing, attempt.contents(),
                "Reducing configured capacity must not truncate fluid already stored in a tank.");
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
    void selectsLargestExtractionContainerThatFitsAvailableVolume() {
        var quarterBucket = new FluidContainerDefinition("water_phial", 250, true, true);
        var halfBucket = new FluidContainerDefinition("water_flask", 500, true, true);
        var bucket = new FluidContainerDefinition("water_bucket", 1_000, true, true);
        var fillOnly = new FluidContainerDefinition("water_canister", 2_000, true, false);

        assertEquals(bucket, FluidTankPolicy.selectExtractionContainer(
                List.of(quarterBucket, halfBucket, bucket, fillOnly), 1_500).orElseThrow());
        assertEquals(halfBucket, FluidTankPolicy.selectExtractionContainer(
                List.of(quarterBucket, halfBucket, bucket), 750).orElseThrow());
        assertEquals(quarterBucket, FluidTankPolicy.selectExtractionContainer(
                List.of(quarterBucket, halfBucket, bucket), 250).orElseThrow());
    }

    @Test
    void hasNoExtractionContainerWhenTankVolumeIsInsufficient() {
        var quarterBucket = new FluidContainerDefinition("water_phial", 250, false, true);
        var bucket = new FluidContainerDefinition("water_bucket", 1_000, false, true);

        assertTrue(FluidTankPolicy.selectExtractionContainer(
                List.of(quarterBucket, bucket), 249).isEmpty());
        assertTrue(FluidTankPolicy.selectExtractionContainer(
                List.of(bucket), 999).isEmpty());
    }

    @Test
    void rejectsInvalidExtractionSelectionArguments() {
        assertThrows(NullPointerException.class,
                () -> FluidTankPolicy.selectExtractionContainer(null, 1_000));
        assertThrows(IllegalArgumentException.class,
                () -> FluidTankPolicy.selectExtractionContainer(List.of(), -1));
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

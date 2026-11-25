package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MachineWorkQueueTest {
    @Test
    void queueDoesNotScheduleTheSameMachineTwice() {
        MachineWorkQueue queue = new MachineWorkQueue();
        queue.add("a");
        queue.add("a");
        queue.add("b");
        assertEquals(2, queue.size());
        assertEquals(List.of("a", "b"), queue.nextBatch(10));
    }

    @Test
    void nextBatchAndRequeueProvideRoundRobinFairness() {
        MachineWorkQueue queue = new MachineWorkQueue();
        for (String id : List.of("a", "b", "c", "d")) queue.add(id);

        List<String> first = queue.nextBatch(2);
        assertEquals(List.of("a", "b"), first);
        first.forEach(queue::add);

        List<String> second = queue.nextBatch(2);
        assertEquals(List.of("c", "d"), second);
        second.forEach(queue::add);

        assertEquals(List.of("a", "b"), queue.nextBatch(2));
    }

    @Test
    void removesMachinesThatUnloadOrBreakBeforeTheirTurn() {
        MachineWorkQueue queue = new MachineWorkQueue();
        queue.add("a");
        queue.add("b");
        queue.add("c");
        queue.remove("b");
        assertEquals(List.of("a", "c"), queue.nextBatch(10));
    }

    @Test
    void rejectsInvalidBatchSizesAndMachineKeys() {
        MachineWorkQueue queue = new MachineWorkQueue();
        assertThrows(IllegalArgumentException.class, () -> queue.add(""));
        assertThrows(IllegalArgumentException.class, () -> queue.nextBatch(0));
        assertThrows(IllegalArgumentException.class, () -> queue.nextBatch(-1));
    }

    @Test
    void clearRemovesBothQueuedAndDuplicateTrackingState() {
        MachineWorkQueue queue = new MachineWorkQueue();
        queue.add("a");
        queue.clear();
        assertEquals(0, queue.size());
        queue.add("a");
        assertEquals(List.of("a"), queue.nextBatch(1));
    }
}

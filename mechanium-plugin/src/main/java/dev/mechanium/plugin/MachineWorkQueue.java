package dev.mechanium.plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Round-robin queue for loaded machines; each registered ID appears at most once. */
final class MachineWorkQueue {
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private final Set<String> scheduled = new HashSet<>();

    void add(String machineKey) {
        if (machineKey == null || machineKey.isBlank()) {
            throw new IllegalArgumentException("Machine key cannot be blank");
        }
        if (scheduled.add(machineKey)) queue.addLast(machineKey);
    }

    void remove(String machineKey) {
        if (machineKey == null) return;
        if (scheduled.remove(machineKey)) queue.remove(machineKey);
    }

    List<String> nextBatch(int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("Batch size must be positive");
        int scan = Math.min(queue.size(), maximum);
        List<String> result = new ArrayList<>(scan);
        for (int i = 0; i < scan; i++) {
            String key = queue.removeFirst();
            scheduled.remove(key);
            result.add(key);
        }
        return List.copyOf(result);
    }

    int size() {
        return queue.size();
    }

    void clear() {
        queue.clear();
        scheduled.clear();
    }
}

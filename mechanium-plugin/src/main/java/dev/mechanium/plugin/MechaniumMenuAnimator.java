package dev.mechanium.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Small server-side reveal animation for MECHANIUM chest menus. */
final class MechaniumMenuAnimator {
    private static final int FRAME_BATCH_SIZE = 3;
    private static final int CONTENT_BATCH_SIZE = 2;
    private static final long STEP_INTERVAL_TICKS = 2L;

    private MechaniumMenuAnimator() { }

    static void open(JavaPlugin plugin, Player player, Inventory inventory, Runnable onFinished) {
        ItemStack[] layout = Arrays.stream(inventory.getContents())
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);

        List<Integer> frame = new ArrayList<>();
        List<Integer> contents = new ArrayList<>();
        for (int slot = 0; slot < layout.length; slot++) {
            if (layout[slot] == null) continue;
            if (isFrameSlot(slot)) frame.add(slot);
            else contents.add(slot);
        }

        // Reveal the outer frame first, then the controls from the centre outward.
        contents.sort(Comparator.comparingInt(slot -> {
            int x = slot % 9;
            int y = slot / 9;
            int dx = x - 4;
            int dy = y - 2;
            return dx * dx + dy * dy;
        }));

        inventory.clear();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.65f, 0.85f);

        int[] frameIndex = {0};
        int[] contentIndex = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                task[0].cancel();
                return;
            }

            if (frameIndex[0] < frame.size()) {
                for (int count = 0; count < FRAME_BATCH_SIZE && frameIndex[0] < frame.size(); count++) {
                    int slot = frame.get(frameIndex[0]++);
                    inventory.setItem(slot, layout[slot]);
                }
                return;
            }

            if (contentIndex[0] < contents.size()) {
                for (int count = 0; count < CONTENT_BATCH_SIZE && contentIndex[0] < contents.size(); count++) {
                    int slot = contents.get(contentIndex[0]++);
                    inventory.setItem(slot, layout[slot]);
                }
                return;
            }

            if (onFinished != null) onFinished.run();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.45f, 1.25f);
            task[0].cancel();
        }, 1L, STEP_INTERVAL_TICKS);
    }

    private static boolean isFrameSlot(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        return row == 0 || row == 5 || column == 0 || column == 8;
    }
}

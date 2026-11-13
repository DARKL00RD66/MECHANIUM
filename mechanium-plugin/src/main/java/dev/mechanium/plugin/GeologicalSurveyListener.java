package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Surveys a small, already-loaded area without generating chunks. */
final class GeologicalSurveyListener implements Listener {
    private static final int RADIUS = 4;
    private static final long COOLDOWN_MILLIS = 1500L;
    private static final String SURVEY_ITEM_ID = "geological_survey_kit";

    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final Map<UUID, Long> lastSurveyAt = new HashMap<>();

    GeologicalSurveyListener(JavaPlugin plugin, MechaniumApi api) {
        this.api = api;
        this.itemIdKey = new NamespacedKey("mechanium", "item_id");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSurvey(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isSurveyKit(held)) return;

        event.setCancelled(true);
        if (!api.canUseItem(player.getUniqueId(), SURVEY_ITEM_ID)) {
            player.sendMessage(Component.text("Você ainda não desbloqueou a prospecção geológica.", NamedTextColor.RED));
            return;
        }

        long now = System.currentTimeMillis();
        long previous = lastSurveyAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < COOLDOWN_MILLIS) {
            player.sendMessage(Component.text("Aguarde um instante antes de realizar outro levantamento.", NamedTextColor.GRAY));
            return;
        }
        lastSurveyAt.put(player.getUniqueId(), now);
        if (lastSurveyAt.size() > 1024) {
            lastSurveyAt.entrySet().removeIf(entry -> now - entry.getValue() > 60_000L);
        }

        Map<Material, Integer> deposits = survey(event.getClickedBlock());
        if (deposits.isEmpty()) {
            player.sendMessage(Component.text("Levantamento concluído: nenhum minério encontrado na área analisada.", NamedTextColor.YELLOW));
            return;
        }

        int total = deposits.values().stream().mapToInt(Integer::intValue).sum();
        player.sendMessage(Component.text("Levantamento geológico — ", NamedTextColor.GOLD)
                .append(Component.text(total + " blocos de minério identificados.", NamedTextColor.WHITE)));
        deposits.entrySet().stream()
                .sorted(Map.Entry.<Material, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(5)
                .forEach(entry -> player.sendMessage(Component.text("• " + displayName(entry.getKey()) + ": ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(entry.getValue()), NamedTextColor.WHITE))));
        player.sendMessage(Component.text("Área analisada: raio de " + RADIUS
                + " blocos ao redor do ponto selecionado; chunks não carregados foram ignorados.", NamedTextColor.DARK_GRAY));
    }

    private Map<Material, Integer> survey(Block center) {
        List<Material> found = new ArrayList<>();
        var world = center.getWorld();
        for (int x = center.getX() - RADIUS; x <= center.getX() + RADIUS; x++) {
            for (int y = Math.max(world.getMinHeight(), center.getY() - RADIUS);
                 y <= Math.min(world.getMaxHeight() - 1, center.getY() + RADIUS); y++) {
                for (int z = center.getZ() - RADIUS; z <= center.getZ() + RADIUS; z++) {
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (OreSurvey.isOre(material)) found.add(material);
                }
            }
        }
        return OreSurvey.summarize(found);
    }

    private boolean isSurveyKit(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return SURVEY_ITEM_ID.equals(id);
    }

    private String displayName(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

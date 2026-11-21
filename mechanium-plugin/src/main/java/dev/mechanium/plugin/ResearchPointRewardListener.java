package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;

/** Rewards scientific points for discovering configured ore types, once per player and material. */
final class ResearchPointRewardListener implements Listener {
    private final MechaniumPlugin plugin;
    private final MechaniumApi api;

    ResearchPointRewardListener(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreDiscovered(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String materialId = event.getBlock().getType().name();
        long reward = plugin.getConfig().getLong("research-rewards.blocks." + materialId, 0L);
        if (reward <= 0L) return;

        NamespacedKey discoveryKey = new NamespacedKey(
                plugin, "research_discovery_" + materialId.toLowerCase(java.util.Locale.ROOT));
        var data = player.getPersistentDataContainer();
        Byte discovered = data.get(discoveryKey, PersistentDataType.BYTE);
        if (discovered != null && discovered == (byte) 1) return;

        // Persist the discovery marker in player data before granting points to avoid repeated farming.
        data.set(discoveryKey, PersistentDataType.BYTE, (byte) 1);
        api.addResearchPoints(player.getUniqueId(), reward);
        player.sendMessage("§bDescoberta científica: §f" + materialId.toLowerCase(java.util.Locale.ROOT)
                .replace('_', ' ') + " §7(+" + reward + " pontos de pesquisa)");
    }
}

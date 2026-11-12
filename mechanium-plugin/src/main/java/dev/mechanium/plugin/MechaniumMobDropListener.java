package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.MobDropDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/** Applies registered, technology-gated byproduct drops without replacing vanilla loot. */
final class MechaniumMobDropListener implements Listener {
    private final MechaniumApi api;

    MechaniumMobDropListener(MechaniumApi api) {
        this.api = api;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        String entityType = event.getEntityType().name();
        Player killer = event.getEntity().getKiller();

        for (MobDropDefinition drop : api.getMobDrops()) {
            if (!drop.entityType().equals(entityType)) continue;
            if (drop.requiredTechnologyId() != null) {
                if (killer == null || !api.canUseTechnology(killer.getUniqueId(), drop.requiredTechnologyId())) {
                    continue;
                }
            }
            if (ThreadLocalRandom.current().nextDouble() >= drop.chance()) continue;

            int amount = ThreadLocalRandom.current().nextInt(drop.minAmount(), drop.maxAmount() + 1);
            ItemStack result = api.createItemStack(drop.resultItemId(), amount).orElse(null);
            if (result != null) event.getDrops().add(result);
        }
    }
}

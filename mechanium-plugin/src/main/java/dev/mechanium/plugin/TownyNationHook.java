package dev.mechanium.plugin;

import dev.mechanium.api.integration.NationIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Optional Towny bridge. Towny is accessed through its public API without becoming a hard dependency. */
final class TownyNationHook implements NationIdentityProvider {
    private final JavaPlugin plugin;
    private final NamespacedKey nationIdKey;
    private final NamespacedKey nationNameKey;
    private boolean available;

    TownyNationHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.nationIdKey = new NamespacedKey(plugin, "towny_nation_id");
        this.nationNameKey = new NamespacedKey(plugin, "towny_nation_name");
    }

    void start() {
        Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
        if (towny == null || !towny.isEnabled()) {
            plugin.getLogger().info("Towny não encontrado; integração nacional desativada.");
            return;
        }
        available = true;
        Bukkit.getOnlinePlayers().forEach(this::sync);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::sync), 20L, 1200L);
        plugin.getLogger().info("Hook Towny ativo: UUID e nome da nação sincronizados automaticamente.");
    }

    void sync(Player player) {
        if (!available) return;
        try {
            Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
            if (towny == null || !towny.isEnabled()) { available = false; clear(player); return; }
            ClassLoader loader = towny.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI", true, loader);
            Object townyApi = apiClass.getMethod("getInstance").invoke(null);
            Object nation = apiClass.getMethod("getNation", Player.class).invoke(townyApi, player);
            if (nation == null) { clear(player); return; }
            Method uuidMethod = nation.getClass().getMethod("getUUID");
            Method nameMethod = nation.getClass().getMethod("getName");
            UUID nationId = (UUID) uuidMethod.invoke(nation);
            String nationName = String.valueOf(nameMethod.invoke(nation));
            player.getPersistentDataContainer().set(nationIdKey, PersistentDataType.STRING, nationId.toString());
            player.getPersistentDataContainer().set(nationNameKey, PersistentDataType.STRING, nationName);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Não foi possível sincronizar a nação de " + player.getName() + ": " + exception.getMessage());
        }
    }

    private void clear(Player player) {
        player.getPersistentDataContainer().remove(nationIdKey);
        player.getPersistentDataContainer().remove(nationNameKey);
    }

    @Override
    public Optional<UUID> getNationId(Player player) {
        String value = player.getPersistentDataContainer().get(nationIdKey, PersistentDataType.STRING);
        if (value == null) return Optional.empty();
        try { return Optional.of(UUID.fromString(value)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    @Override
    public Optional<String> getNationName(Player player) {
        return Optional.ofNullable(player.getPersistentDataContainer().get(nationNameKey, PersistentDataType.STRING));
    }

    @Override public boolean isAvailable() { return available; }
}

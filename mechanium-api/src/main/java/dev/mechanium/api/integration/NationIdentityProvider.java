package dev.mechanium.api.integration;

import org.bukkit.entity.Player;
import java.util.Optional;
import java.util.UUID;

/** Optional identity bridge for plugins that associate players with geopolitical groups. */
public interface NationIdentityProvider {
    Optional<UUID> getNationId(Player player);
    Optional<String> getNationName(Player player);
    boolean isAvailable();
}

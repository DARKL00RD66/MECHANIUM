package dev.mechanium.plugin;

import java.util.Set;
import java.util.UUID;

/** Pure access rules for private storage, independent from Bukkit block state. */
final class PrivateStorageAccessPolicy {
    private PrivateStorageAccessPolicy() { }

    static boolean canAccess(UUID playerId, String ownerId, Set<String> trustedPlayerIds, boolean administrator) {
        if (playerId == null || ownerId == null) return false;
        String player = playerId.toString();
        return administrator || player.equals(ownerId)
                || (trustedPlayerIds != null && trustedPlayerIds.contains(player));
    }

    static boolean canManage(UUID playerId, String ownerId, boolean administrator) {
        return playerId != null && ownerId != null
                && (administrator || playerId.toString().equals(ownerId));
    }
}

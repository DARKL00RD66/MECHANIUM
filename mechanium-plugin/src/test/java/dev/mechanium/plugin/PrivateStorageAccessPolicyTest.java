package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrivateStorageAccessPolicyTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID trusted = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();

    @Test
    void ownerCanAccessAndManageStorage() {
        assertTrue(PrivateStorageAccessPolicy.canAccess(owner, owner.toString(), Set.of(), false));
        assertTrue(PrivateStorageAccessPolicy.canManage(owner, owner.toString(), false));
    }

    @Test
    void trustedPlayerCanAccessButCannotManageStorage() {
        assertTrue(PrivateStorageAccessPolicy.canAccess(
                trusted, owner.toString(), Set.of(trusted.toString()), false));
        assertFalse(PrivateStorageAccessPolicy.canManage(trusted, owner.toString(), false));
    }

    @Test
    void unrelatedPlayerCannotAccessOrManageStorage() {
        assertFalse(PrivateStorageAccessPolicy.canAccess(outsider, owner.toString(), Set.of(), false));
        assertFalse(PrivateStorageAccessPolicy.canManage(outsider, owner.toString(), false));
    }

    @Test
    void administratorCanAccessAndManageStorage() {
        assertTrue(PrivateStorageAccessPolicy.canAccess(outsider, owner.toString(), Set.of(), true));
        assertTrue(PrivateStorageAccessPolicy.canManage(outsider, owner.toString(), true));
    }

    @Test
    void malformedOrMissingOwnerFailsClosed() {
        assertFalse(PrivateStorageAccessPolicy.canAccess(outsider, null, Set.of(outsider.toString()), false));
        assertFalse(PrivateStorageAccessPolicy.canManage(outsider, "", false));
    }
}

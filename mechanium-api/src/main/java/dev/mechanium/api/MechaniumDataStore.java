package dev.mechanium.api;

import java.util.Optional;
import java.util.UUID;

/** Persistence contract. spendAndUnlock must debit and persist atomically. */
public interface MechaniumDataStore extends AutoCloseable {
    Optional<String> getPlayerEra(UUID playerId);
    void setPlayerEra(UUID playerId, String eraId);
    /** Era individual explicitamente definida para testes; vazio significa herdar a era global. */
    default Optional<String> getPlayerEraOverride(UUID playerId) { return getPlayerEra(playerId); }
    /** Era histórica ativa para todo o servidor. */
    default Optional<String> getServerEra() { return Optional.empty(); }
    default void setServerEra(String eraId) { throw new UnsupportedOperationException("Global server eras are not supported by this data store."); }
    default void clearPlayerEraOverride(UUID playerId) { throw new UnsupportedOperationException("Player era overrides cannot be cleared by this data store."); }
    long getResearchPoints(UUID playerId);
    void addResearchPoints(UUID playerId, long amount);
    boolean hasUnlocked(UUID playerId, String technologyId);
    boolean spendAndUnlock(UUID playerId, String technologyId, long cost);
    @Override void close();
}
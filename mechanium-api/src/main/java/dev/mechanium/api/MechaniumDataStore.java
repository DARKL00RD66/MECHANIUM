package dev.mechanium.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import dev.mechanium.api.model.ResearchProgress;
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
    /** Atomically debits points and creates a timed research job. */
    default boolean beginResearch(UUID playerId, String technologyId, long cost,
                                  long startedAtEpochMillis, long completesAtEpochMillis) { return false; }
    /** Returns all active jobs for a player, ordered by their completion deadline. */
    default Collection<ResearchProgress> getResearches(UUID playerId) { return List.of(); }
    /** Atomically converts all due jobs into unlocked technologies and returns their IDs. */
    default Collection<String> completeDueResearches(UUID playerId, long nowEpochMillis) { return List.of(); }
    @Override void close();
}
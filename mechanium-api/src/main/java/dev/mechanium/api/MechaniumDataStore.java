package dev.mechanium.api;

import java.util.Optional;
import java.util.UUID;

/** Persistence contract. spendAndUnlock must debit and persist atomically. */
public interface MechaniumDataStore extends AutoCloseable {
    Optional<String> getPlayerEra(UUID playerId);
    void setPlayerEra(UUID playerId, String eraId);
    long getResearchPoints(UUID playerId);
    void addResearchPoints(UUID playerId, long amount);
    boolean hasUnlocked(UUID playerId, String technologyId);
    boolean spendAndUnlock(UUID playerId, String technologyId, long cost);
    @Override void close();
}
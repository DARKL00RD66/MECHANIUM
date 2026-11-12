package dev.mechanium.api;

import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.UnlockResult;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Public, versioned entry point for MECHANIUM and third-party addons. */
public interface MechaniumApi {
    String API_VERSION = "1";
    String getApiVersion();
    boolean registerEra(EraDefinition era);
    Optional<EraDefinition> getEra(String eraId);
    Collection<EraDefinition> getEras();
    Optional<String> getPlayerEra(UUID playerId);
    void setPlayerEra(UUID playerId, String eraId);
    long getResearchPoints(UUID playerId);
    void addResearchPoints(UUID playerId, long amount);
    boolean registerTechnology(TechnologyDefinition technology);
    Optional<TechnologyDefinition> getTechnology(String technologyId);
    Collection<TechnologyDefinition> getTechnologies();
    UnlockResult unlockTechnology(UUID playerId, String technologyId);
    boolean canUseTechnology(UUID playerId, String technologyId);
    void registerModule(String moduleId);
    Set<String> getRegisteredModules();
}
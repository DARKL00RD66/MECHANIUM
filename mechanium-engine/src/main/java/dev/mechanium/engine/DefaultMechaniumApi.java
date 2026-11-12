package dev.mechanium.engine;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumDataStore;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.UnlockResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Default progression engine. All player state is delegated to the persistent data store. */
public final class DefaultMechaniumApi implements MechaniumApi {
    private final MechaniumDataStore dataStore;
    private final Map<String, EraDefinition> eras = new ConcurrentHashMap<>();
    private final Map<String, TechnologyDefinition> technologies = new ConcurrentHashMap<>();
    private final Set<String> modules = ConcurrentHashMap.newKeySet();

    public DefaultMechaniumApi(MechaniumDataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
    }

    @Override public String getApiVersion() { return API_VERSION; }

    @Override
    public boolean registerEra(EraDefinition era) {
        Objects.requireNonNull(era, "era");
        EraDefinition previous = eras.putIfAbsent(era.id(), era);
        return previous == null || previous.equals(era);
    }

    @Override public Optional<EraDefinition> getEra(String eraId) {
        return Optional.ofNullable(eras.get(normalize(eraId)));
    }

    @Override public Collection<EraDefinition> getEras() {
        return eras.values().stream().sorted(Comparator.comparingInt(EraDefinition::order)).toList();
    }

    @Override public Optional<String> getPlayerEra(UUID playerId) {
        return dataStore.getPlayerEra(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override
    public void setPlayerEra(UUID playerId, String eraId) {
        String normalized = normalize(eraId);
        if (!eras.containsKey(normalized)) throw new IllegalArgumentException("Unknown era: " + eraId);
        dataStore.setPlayerEra(Objects.requireNonNull(playerId, "playerId"), normalized);
    }

    @Override public long getResearchPoints(UUID playerId) {
        return dataStore.getResearchPoints(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override public void addResearchPoints(UUID playerId, long amount) {
        dataStore.addResearchPoints(Objects.requireNonNull(playerId, "playerId"), amount);
    }

    @Override
    public boolean registerTechnology(TechnologyDefinition technology) {
        Objects.requireNonNull(technology, "technology");
        if (!eras.containsKey(technology.requiredEraId())) {
            throw new IllegalArgumentException("Technology " + technology.id()
                    + " references unregistered era " + technology.requiredEraId());
        }
        TechnologyDefinition previous = technologies.putIfAbsent(technology.id(), technology);
        return previous == null || previous.equals(technology);
    }

    @Override public Optional<TechnologyDefinition> getTechnology(String technologyId) {
        return Optional.ofNullable(technologies.get(normalize(technologyId)));
    }

    @Override public Collection<TechnologyDefinition> getTechnologies() {
        return technologies.values().stream().sorted(Comparator.comparing(TechnologyDefinition::id)).toList();
    }

    @Override
    public UnlockResult unlockTechnology(UUID playerId, String technologyId) {
        Objects.requireNonNull(playerId, "playerId");
        TechnologyDefinition technology = technologies.get(normalize(technologyId));
        if (technology == null) {
            return UnlockResult.of(UnlockResult.Status.UNKNOWN_TECHNOLOGY, "Tecnologia desconhecida.");
        }
        if (dataStore.hasUnlocked(playerId, technology.id())) {
            return UnlockResult.of(UnlockResult.Status.ALREADY_UNLOCKED, "Tecnologia já desbloqueada.");
        }

        Optional<String> currentEraId = dataStore.getPlayerEra(playerId);
        if (currentEraId.isEmpty() || !eras.containsKey(currentEraId.get())) {
            return UnlockResult.of(UnlockResult.Status.UNKNOWN_PLAYER_ERA, "Jogador ainda não possui uma era válida.");
        }
        EraDefinition currentEra = eras.get(currentEraId.get());
        EraDefinition requiredEra = eras.get(technology.requiredEraId());
        if (currentEra.order() < requiredEra.order()) {
            return UnlockResult.of(UnlockResult.Status.WRONG_ERA,
                    "Esta tecnologia exige a era " + requiredEra.displayName() + ".");
        }

        List<String> missing = technology.prerequisites().stream()
                .filter(id -> !dataStore.hasUnlocked(playerId, id))
                .toList();
        if (!missing.isEmpty()) {
            return UnlockResult.of(UnlockResult.Status.MISSING_PREREQUISITES,
                    "Pesquisas necessárias: " + String.join(", ", missing));
        }
        if (dataStore.getResearchPoints(playerId) < technology.researchCost()) {
            return UnlockResult.of(UnlockResult.Status.INSUFFICIENT_RESEARCH_POINTS,
                    "Pontos científicos insuficientes. Custo: " + technology.researchCost() + ".");
        }

        if (!dataStore.spendAndUnlock(playerId, technology.id(), technology.researchCost())) {
            if (dataStore.hasUnlocked(playerId, technology.id())) {
                return UnlockResult.of(UnlockResult.Status.ALREADY_UNLOCKED, "Tecnologia já desbloqueada.");
            }
            return UnlockResult.of(UnlockResult.Status.CONCURRENT_UPDATE,
                    "O estado científico mudou simultaneamente. Tente novamente.");
        }
        return UnlockResult.of(UnlockResult.Status.UNLOCKED,
                "Tecnologia desbloqueada: " + technology.displayName());
    }

    @Override
    public boolean canUseTechnology(UUID playerId, String technologyId) {
        TechnologyDefinition technology = technologies.get(normalize(technologyId));
        if (technology == null || !dataStore.hasUnlocked(playerId, technology.id())) return false;
        Optional<String> eraId = dataStore.getPlayerEra(playerId);
        if (eraId.isEmpty()) return false;
        EraDefinition current = eras.get(eraId.get());
        EraDefinition required = eras.get(technology.requiredEraId());
        return current != null && required != null && current.order() >= required.order();
    }

    @Override public void registerModule(String moduleId) { modules.add(normalize(moduleId)); }

    @Override public Set<String> getRegisteredModules() {
        return Collections.unmodifiableSet(new TreeSet<>(modules));
    }

    private String normalize(String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

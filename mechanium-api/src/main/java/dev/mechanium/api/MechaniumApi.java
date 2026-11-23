package dev.mechanium.api;

import dev.mechanium.api.model.*;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/** Public, versioned entry point for MECHANIUM and third-party addons. */
public interface MechaniumApi {
    String API_VERSION = "1";
    String getApiVersion();
    /** Opens an atomic content-registration scope owned by a module or addon. */
    default MechaniumContentRegistrationScope openContentScope(String ownerId) {
        return MechaniumContentRegistrationScope.noop();
    }
    /** Removes persisted content owned by modules/addons that did not load this startup. */
    default void pruneInactiveContentOwners(Set<String> activeOwnerIds) { }
    boolean registerEra(EraDefinition era);
    Optional<EraDefinition> getEra(String eraId);
    Collection<EraDefinition> getEras();
    /** Effective era: a player's explicit test override, or the server-wide era when no override exists. */
    Optional<String> getPlayerEra(UUID playerId);
    void setPlayerEra(UUID playerId, String eraId);
    /** Era explicitly assigned to one player for isolated administrative tests. */
    default Optional<String> getPlayerEraOverride(UUID playerId) { return Optional.empty(); }
    default void clearPlayerEraOverride(UUID playerId) { throw new UnsupportedOperationException("Player era overrides cannot be cleared."); }
    /** Persistent historical era that applies to all players without an individual test override. */
    default Optional<String> getServerEra() { return Optional.empty(); }
    default void setServerEra(String eraId) { throw new UnsupportedOperationException("Global server eras are not supported."); }
    long getResearchPoints(UUID playerId);
    void addResearchPoints(UUID playerId, long amount);
    boolean registerTechnology(TechnologyDefinition technology);
    Optional<TechnologyDefinition> getTechnology(String technologyId);
    Collection<TechnologyDefinition> getTechnologies();
    UnlockResult unlockTechnology(UUID playerId, String technologyId);
    boolean canUseTechnology(UUID playerId, String technologyId);
    default boolean hasUnlockedTechnology(UUID playerId, String technologyId) {
        return canUseTechnology(playerId, technologyId);
    }
    void registerModule(String moduleId);
    Set<String> getRegisteredModules();

    boolean registerCategory(ContentCategory category);
    Optional<ContentCategory> getCategory(String categoryId);
    Collection<ContentCategory> getCategories();
    boolean registerItem(ItemDefinition item);
    Optional<ItemDefinition> getItem(String itemId);
    Collection<ItemDefinition> getItems();
    Optional<ItemStack> createItemStack(String itemId, int amount);
    boolean canUseItem(UUID playerId, String itemId);
    boolean registerRecipe(RecipeDefinition recipe);
    /** Replaces an already registered recipe in memory. Administrators persist overrides separately from module-owned definitions. */
    default boolean overrideRecipe(RecipeDefinition recipe) { return false; }
    Optional<RecipeDefinition> getRecipe(String recipeId);
    Collection<RecipeDefinition> getRecipes();
    boolean canCraft(UUID playerId, String recipeId);
    default boolean registerMobDrop(MobDropDefinition drop) { return false; }
    default Optional<MobDropDefinition> getMobDrop(String dropId) { return Optional.empty(); }
    default Collection<MobDropDefinition> getMobDrops() { return java.util.List.of(); }
    default boolean registerMachine(MachineDefinition machine) { return false; }
    default Optional<MachineDefinition> getMachine(String machineId) { return Optional.empty(); }
    default Collection<MachineDefinition> getMachines() { return java.util.List.of(); }
    java.util.List<String> validateContent();
}

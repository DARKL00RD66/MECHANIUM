package dev.mechanium.api;

import dev.mechanium.api.model.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Persistence contract for content definitions registered by modules and addons. */
public interface MechaniumContentStore {
    String CATEGORY = "category";
    String ITEM = "item";
    String RECIPE = "recipe";
    String MOB_DROP = "mob_drop";
    String MACHINE = "machine";
    String FLUID = "fluid";

    void saveCategory(ContentCategory category);
    Collection<ContentCategory> loadCategories();
    void saveItem(ItemDefinition item);
    Collection<ItemDefinition> loadItems();
    void saveRecipe(RecipeDefinition recipe);
    Collection<RecipeDefinition> loadRecipes();
    void saveMobDrop(MobDropDefinition drop);
    Collection<MobDropDefinition> loadMobDrops();
    void saveMachine(MachineDefinition machine);
    Collection<MachineDefinition> loadMachines();
    default void saveFluid(FluidDefinition fluid) {
        throw new UnsupportedOperationException("Typed fluids are not supported by this content store.");
    }
    default Collection<FluidDefinition> loadFluids() { return java.util.List.of(); }

    /** Begins a synchronous transaction for one module/addon registration callback. */
    void beginContentTransaction();
    /**
     * Claims the supplied content IDs for ownerId and removes IDs formerly owned by it
     * but not registered in this callback. Must run inside a content transaction.
     * Returns the removed IDs grouped by content type.
     */
    Map<String, Set<String>> reconcileContentOwner(String ownerId, Map<String, Set<String>> activeContent);
    /** Removes all owned content whose owner is not in activeOwnerIds. Must run in a transaction. */
    Map<String, Set<String>> pruneInactiveContentOwners(Set<String> activeOwnerIds);
    /** Renames an owner identity without deleting any registered content. Must run in a transaction. */
    void migrateContentOwner(String previousOwnerId, String newOwnerId);
    void commitContentTransaction();
    void rollbackContentTransaction();
}

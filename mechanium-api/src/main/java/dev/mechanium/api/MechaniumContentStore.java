package dev.mechanium.api;

import dev.mechanium.api.model.*;
import java.util.Collection;

/** Persistence contract for content definitions registered by modules and addons. */
public interface MechaniumContentStore {
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
}

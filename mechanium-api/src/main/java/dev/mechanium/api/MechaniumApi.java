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
    Optional<RecipeDefinition> getRecipe(String recipeId);
    Collection<RecipeDefinition> getRecipes();
    boolean canCraft(UUID playerId, String recipeId);
    java.util.List<String> validateContent();
}

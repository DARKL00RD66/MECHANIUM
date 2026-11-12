package dev.mechanium.api;
import dev.mechanium.api.model.*;
import java.util.Collection;
public interface MechaniumContentStore {
 void saveCategory(ContentCategory category); Collection<ContentCategory> loadCategories();
 void saveItem(ItemDefinition item); Collection<ItemDefinition> loadItems();
 void saveRecipe(RecipeDefinition recipe); Collection<RecipeDefinition> loadRecipes();
}
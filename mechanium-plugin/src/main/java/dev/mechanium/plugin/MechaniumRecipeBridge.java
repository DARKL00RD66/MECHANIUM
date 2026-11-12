package dev.mechanium.plugin;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.RecipeDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
/** Converts validated MECHANIUM recipes to native Bukkit crafting recipes. */
final class MechaniumRecipeBridge {
 private final JavaPlugin plugin;private final MechaniumApi api;private final Set<String> registered=new HashSet<>();
 MechaniumRecipeBridge(JavaPlugin plugin,MechaniumApi api){this.plugin=plugin;this.api=api;}
 void registerAll(){for(RecipeDefinition r:api.getRecipes())register(r);}
 void register(RecipeDefinition d){if(!registered.add(d.id()))return;ItemStack result=api.createItemStack(d.resultItemId(),d.resultAmount()).orElseThrow(()->new IllegalStateException("Missing result item "+d.resultItemId()));NamespacedKey key=new NamespacedKey(plugin,d.id());Recipe recipe;if(d.type()==RecipeDefinition.Type.SHAPED){ShapedRecipe shaped=new ShapedRecipe(key,result);shaped.shape(d.shape().toArray(String[]::new));d.ingredients().forEach((symbol,value)->shaped.setIngredient(symbol,choice(value)));recipe=shaped;}else{ShapelessRecipe shapeless=new ShapelessRecipe(key,result);d.ingredients().values().forEach(value->shapeless.addIngredient(choice(value)));recipe=shapeless;}if(!Bukkit.addRecipe(recipe))throw new IllegalStateException("Bukkit refused recipe "+d.id());}
 private RecipeChoice choice(String value){if(value.startsWith("mechanium:")){ItemStack item=api.createItemStack(value.substring(10),1).orElseThrow(()->new IllegalStateException("Unknown custom ingredient "+value));return new RecipeChoice.ExactChoice(item);}return new RecipeChoice.MaterialChoice(Material.valueOf(value));}
}
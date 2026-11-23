package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Clean-room environmental recovery gameplay inspired by GlobalWarming. */
final class GlobalWarmingModule extends AbstractBuiltinModule {
    @Override public String id() { return "global-warming"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("environment"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("carbon_sequestration", "Recuperação ambiental",
                "modern", 50, List.of("emissions_control")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("global_warming", "Recuperação ambiental",
                "Itens de recuperação do indicador ambiental do mundo"));
        registerItem(api, new ItemDefinition("carbon_capture_cell", "&aCélula de captura de carbono",
                "PAPER", List.of("&8Consumível ambiental", "&7Reduz 10 unidades do impacto industrial do mundo"),
                0, 16, "global_warming", "carbon_sequestration"));
        registerRecipe(api, new RecipeDefinition("carbon_capture_cell_recipe", RecipeDefinition.Type.SHAPELESS,
                "carbon_capture_cell", 2, List.of(),
                Map.of('C', "CHARCOAL", 'P', "PAPER", 'G', "GLASS_BOTTLE", 'R', "REDSTONE"),
                "carbon_sequestration"));
    }
}

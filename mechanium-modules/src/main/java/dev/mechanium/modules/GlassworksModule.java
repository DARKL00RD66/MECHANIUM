package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Industrial glass forming and precision glass products. */
final class GlassworksModule extends AbstractBuiltinModule {
    @Override public String id() { return "glassworks"; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("industrial_glass_forming", "Moldagem de vidro industrial", "industrial_revolution", 36, List.of("industrial_chemistry")),
            new TechnologyDefinition("tempered_glass_production", "Produção de vidro temperado", "modern", 52, List.of("industrial_glass_forming", "industrial_efficiency"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("glassworks_goods", "Vidro industrial", "Vidros técnicos, painéis e componentes de precisão"));
        registerItem(api, new ItemDefinition("silica_batch", "&eCarga de sílica", "SAND", List.of("&8Mistura preparada para fundição de vidro"), 0, 64, "glassworks_goods", "industrial_glass_forming"));
        registerItem(api, new ItemDefinition("industrial_glass", "&bVidro industrial", "GLASS", List.of("&8Vidro moldado para aplicações técnicas"), 0, 64, "glassworks_goods", "industrial_glass_forming"));
        registerItem(api, new ItemDefinition("tempered_glass_panel", "&fPainel de vidro temperado", "WHITE_STAINED_GLASS_PANE", List.of("&8Painel tratado de maior resistência"), 0, 64, "glassworks_goods", "tempered_glass_production"));
        registerItem(api, new ItemDefinition("precision_lens", "&bLente de precisão", "SPYGLASS", List.of("&8Componente óptico de fabricação controlada"), 0, 16, "glassworks_goods", "tempered_glass_production"));
        registerItem(api, new ItemDefinition("glass_furnace", "&6Forno de vidro", "DISPENSER", List.of("&8Equipamento para moldagem de vidro"), 0, 1, "glassworks_goods", "industrial_glass_forming"));

        registerRecipe(api, new RecipeDefinition("silica_batch_recipe", RecipeDefinition.Type.SHAPELESS, "silica_batch", 4, List.of(), Map.of('S', "SAND", 'Q', "QUARTZ"), "industrial_glass_forming"));
        registerRecipe(api, new RecipeDefinition("industrial_glass_recipe", RecipeDefinition.Type.SHAPELESS, "industrial_glass", 4, List.of(), Map.of('S', "mechanium:silica_batch", 'C', "COAL"), "industrial_glass_forming"));
        registerRecipe(api, new RecipeDefinition("tempered_glass_panel_recipe", RecipeDefinition.Type.SHAPED, "tempered_glass_panel", 8, List.of("GGG", "GIG", "GGG"), Map.of('G', "mechanium:industrial_glass", 'I', "IRON_INGOT"), "tempered_glass_production"));
        registerRecipe(api, new RecipeDefinition("precision_lens_recipe", RecipeDefinition.Type.SHAPED, "precision_lens", 1, List.of(" G ", "GAG", " G "), Map.of('G', "mechanium:industrial_glass", 'A', "AMETHYST_SHARD"), "tempered_glass_production"));
        registerRecipe(api, new RecipeDefinition("glass_furnace_recipe", RecipeDefinition.Type.SHAPED, "glass_furnace", 1, List.of("III", "IFI", "ICI"), Map.of('I', "IRON_INGOT", 'F', "FURNACE", 'C', "COPPER_INGOT"), "industrial_glass_forming"));
        registerMachine(api, new MachineDefinition("glass_furnace", "Forno de vidro", "glass_furnace", "DISPENSER", "mechanium:silica_batch", 2, "industrial_glass", 2, 1200, "industrial_glass_forming"));
    }
}

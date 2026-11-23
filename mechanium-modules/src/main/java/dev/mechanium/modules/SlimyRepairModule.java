package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consumable precision repair kit inspired by the SlimyRepair addon. */
final class SlimyRepairModule extends AbstractBuiltinModule {
    @Override public String id() { return "slimy-repair"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("advanced-metallurgy"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("precision_tool_repair", "Manutenção de precisão",
                "industrial_revolution", 40, List.of("advanced_metallurgy")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("slimy_repair", "Manutenção de equipamentos",
                "Kits consumíveis para reparo de ferramentas e armaduras"));
        registerItem(api, new ItemDefinition("precision_repair_kit", "&aKit de reparo de precisão",
                "PAPER", List.of("&8Consumível de manutenção", "&7Repara 50% da durabilidade máxima de um item danificado",
                        "&7Clique com o botão direito segurando o kit"),
                0, 4, "slimy_repair", "precision_tool_repair"));
        registerRecipe(api, new RecipeDefinition("precision_repair_kit_recipe", RecipeDefinition.Type.SHAPED,
                "precision_repair_kit", 2, List.of("IDI", "RNR", "IDI"),
                Map.of('I', "IRON_INGOT", 'D', "DIAMOND", 'R', "REDSTONE", 'N', "IRON_NUGGET"),
                "precision_tool_repair"));
    }
}

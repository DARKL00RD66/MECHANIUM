package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Advanced maintenance and reliability progression inspired by HardcoreSlimefun. */
final class HardcoreSlimefunModule extends AbstractBuiltinModule {
    @Override public String id() { return "hardcore-slimefun"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("equipment_reliability", "Confiabilidade de equipamentos",
                        "modern", 48, List.of("industrial_efficiency")),
                new TechnologyDefinition("preventive_maintenance", "Manutenção preventiva",
                        "information_age", 64, List.of("equipment_reliability", "automated_control"))
        );
    }

    @Override protected void registerExtra(MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_maintenance", "Manutenção industrial",
                "Componentes e ferramentas para prolongar a vida útil das máquinas"));

        registerItem(api, new ItemDefinition("reinforced_machine_casing", "&7Carcaça reforçada",
                "IRON_BLOCK", List.of("&8Estrutura reforçada para máquinas industriais"),
                0, 16, "industrial_maintenance", "equipment_reliability"));
        registerItem(api, new ItemDefinition("machine_maintenance_kit", "&eKit de manutenção",
                "SHEARS", List.of("&8Ferramenta de manutenção industrial",
                        "&7Agache e use em uma máquina com desgaste para restaurá-la"),
                0, 1, "industrial_maintenance", "preventive_maintenance"));

        registerRecipe(api, new RecipeDefinition("reinforced_machine_casing_recipe",
                RecipeDefinition.Type.SHAPED, "reinforced_machine_casing", 2,
                List.of("III", "ICI", "III"),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_BLOCK"),
                "equipment_reliability"));
        registerRecipe(api, new RecipeDefinition("machine_maintenance_kit_recipe",
                RecipeDefinition.Type.SHAPED, "machine_maintenance_kit", 1,
                List.of("ICI", "RGR", " I "),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'R', "REDSTONE", 'G', "GOLD_INGOT"),
                "preventive_maintenance"));
    }
}

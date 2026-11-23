package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class AutomationModule extends AbstractBuiltinModule {
    @Override public String id() { return "industrial-automation"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("industrial_automation", "Automação industrial",
                "information_age", 58, List.of("automated_control")));
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_automation", "Automação industrial",
                "Controladores e equipamentos de montagem"));
        registerItem(api, new ItemDefinition("control_circuit", "&bCircuito de controle", "COMPARATOR",
                List.of("&8Componente de automação"), 0, 64,
                "industrial_automation", "industrial_automation"));
        registerItem(api, new ItemDefinition("auto_assembler", "&dMontadora automática", "DISPENSER",
                List.of("&8Máquina de montagem automatizada"), 0, 1,
                "industrial_automation", "industrial_automation"));
        registerRecipe(api, new RecipeDefinition("control_circuit_recipe", RecipeDefinition.Type.SHAPELESS,
                "control_circuit", 2, List.of(),
                Map.of('R', "REDSTONE", 'C', "COPPER_INGOT", 'Q', "QUARTZ"),
                "industrial_automation"));
        registerRecipe(api, new RecipeDefinition("auto_assembler_recipe", RecipeDefinition.Type.SHAPELESS,
                "auto_assembler", 1, List.of(),
                Map.of('D', "DISPENSER", 'C', "mechanium:control_circuit", 'I', "IRON_INGOT"),
                "industrial_automation"));
    }
}
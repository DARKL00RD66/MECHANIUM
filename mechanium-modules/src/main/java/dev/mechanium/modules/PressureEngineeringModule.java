package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class PressureEngineeringModule extends AbstractBuiltinModule {
    @Override public String id() { return "pressure-engineering"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("pressure_engineering", "Engenharia de pressão",
                "modern", 42, List.of("industrial_efficiency")));
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("pressure_engineering", "Engenharia de pressão",
                "Câmaras pressurizadas e componentes compactados"));
        registerItem(api, new ItemDefinition("compressed_plate", "&fPlaca compactada", "IRON_BLOCK",
                List.of("&8Material produzido sob alta pressão"), 0, 64,
                "pressure_engineering", "pressure_engineering"));
        registerItem(api, new ItemDefinition("pressure_chamber", "&bCâmara de pressão", "DISPENSER",
                List.of("&8Máquina de compactação industrial"), 0, 1,
                "pressure_engineering", "pressure_engineering"));
        registerRecipe(api, new RecipeDefinition("compressed_plate_recipe", RecipeDefinition.Type.SHAPELESS,
                "compressed_plate", 1, List.of(),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'P', "PISTON"),
                "pressure_engineering"));
        registerRecipe(api, new RecipeDefinition("pressure_chamber_recipe", RecipeDefinition.Type.SHAPELESS,
                "pressure_chamber", 1, List.of(),
                Map.of('I', "IRON_BLOCK", 'P', "PISTON", 'G', "GLASS"),
                "pressure_engineering"));
    }
}
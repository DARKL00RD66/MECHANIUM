package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Abstract environmental mitigation gameplay; it does not model real-world pollution physics. */
final class GlobalWarmingModule extends AbstractBuiltinModule {
    @Override public String id() { return "global-warming"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("environment", "eco-power", "liquid"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("air_quality_management", "Gestão da qualidade do ar",
                        "modern", 50, List.of("emissions_control", "water_filtration")),
                new TechnologyDefinition("industrial_air_filtration", "Filtragem industrial do ar",
                        "information_age", 66, List.of("air_quality_management", "energy_accumulation"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("air_quality", "Qualidade do ar",
                "Equipamentos de filtragem e manutenção ambiental"));

        registerItem(api, new ItemDefinition("air_purifier", "&bPurificador de ar", "DISPENSER",
                List.of("&8Máquina de filtragem ambiental", "&7Entrada: balde de água", "&7Saída: cartucho de ar limpo",
                        "&7Requer energia de uma bateria adjacente"),
                0, 1, "air_quality", "air_quality_management"));
        registerItem(api, new ItemDefinition("clean_air_cartridge", "&aCartucho de ar limpo", "PAPER",
                List.of("&8Componente produzido pelo purificador de ar"),
                0, 16, "air_quality", "air_quality_management"));

        registerRecipe(api, new RecipeDefinition("air_purifier_recipe", RecipeDefinition.Type.SHAPED,
                "air_purifier", 1, List.of("IRI", "CWD", "IRI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'C', "mechanium:carbon_filter",
                        'W', "WATER_BUCKET", 'D', "DISPENSER"),
                "air_quality_management"));
        registerMachine(api, new MachineDefinition("air_purifier_machine", "Purificador de ar",
                "air_purifier", "DISPENSER", "WATER_BUCKET", 1,
                "clean_air_cartridge", 1, 600, "air_quality_management"));
    }
}

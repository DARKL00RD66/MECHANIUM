package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renewable-energy components; grid simulation and machine power accounting are future work. */
final class EcoPowerModule extends AbstractBuiltinModule {
    @Override public String id() { return "eco-power"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("energy", "industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("wind_generation", "Geração eólica", "medieval", 14, List.of("water_power")),
                new TechnologyDefinition("solar_cells", "Células solares", "electrification", 38, List.of("electrical_grid")),
                new TechnologyDefinition("energy_accumulation", "Acumulação de energia", "modern", 55, List.of("solar_cells"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("eco_power", "Energia renovável", "Componentes para tecnologias energéticas sustentáveis"));
        registerItem(api, new ItemDefinition("wind_rotor", "&fRotor eólico", "IRON_BARS",
                List.of("&8Componente de geração eólica"), 0, 16, "eco_power", "wind_generation"));
        registerItem(api, new ItemDefinition("solar_cell", "&bCélula solar", "LIGHT_BLUE_STAINED_GLASS",
                List.of("&8Componente de captação solar"), 0, 16, "eco_power", "solar_cells"));
        registerItem(api, new ItemDefinition("energy_cell", "&eCélula de armazenamento", "AMETHYST_BLOCK",
                List.of("&8Componente de armazenamento energético", "&7Ainda não participa de uma rede elétrica simulada"),
                0, 16, "eco_power", "energy_accumulation"));

        registerRecipe(api, new RecipeDefinition("wind_rotor_recipe", RecipeDefinition.Type.SHAPED,
                "wind_rotor", 2, List.of(" I ", "IRI", " I "),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE"), "wind_generation"));
        registerRecipe(api, new RecipeDefinition("solar_cell_recipe", RecipeDefinition.Type.SHAPED,
                "solar_cell", 2, List.of("GGG", "RCR", "GGG"),
                Map.of('G', "GLASS", 'R', "REDSTONE", 'C', "COPPER_INGOT"), "solar_cells"));
        registerRecipe(api, new RecipeDefinition("energy_cell_recipe", RecipeDefinition.Type.SHAPED,
                "energy_cell", 1, List.of("ACA", "RER", "ACA"),
                Map.of('A', "AMETHYST_SHARD", 'C', "COPPER_INGOT", 'R', "REDSTONE", 'E', "mechanium:solar_cell"),
                "energy_accumulation"));
    }
}

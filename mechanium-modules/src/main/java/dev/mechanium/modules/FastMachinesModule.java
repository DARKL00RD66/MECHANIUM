package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** High-throughput, powered production machines using the shared MECHANIUM machine runtime. */
final class FastMachinesModule extends AbstractBuiltinModule {
    @Override public String id() { return "fast-machines"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("eco-power", "industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("fast_machinery", "Maquinário de alta velocidade",
                        "modern", 54, List.of("industrial_efficiency")),
                new TechnologyDefinition("automated_high_throughput", "Produção de alto rendimento",
                        "digital_age", 72, List.of("fast_machinery", "automated_control"))
        );
    }

    @Override protected void registerExtra(MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("fast_machines", "Máquinas de alto rendimento",
                "Prensas e enroladores industriais com ciclos de produção acelerados"));

        registerItem(api, new ItemDefinition("rapid_press", "&bPrensa de alta velocidade", "DISPENSER",
                List.of("&8Máquina industrial acelerada", "&7Entrada: barra de ferro", "&7Saída: peças mecânicas"),
                0, 1, "fast_machines", "fast_machinery"));
        registerItem(api, new ItemDefinition("rapid_coil_winder", "&bEnrolador rápido de bobinas", "DISPENSER",
                List.of("&8Máquina elétrica de bobinas", "&7Entrada: lingote de cobre", "&7Saída: bobinas de cobre"),
                0, 1, "fast_machines", "fast_machinery"));

        registerRecipe(api, new RecipeDefinition("rapid_press_recipe", RecipeDefinition.Type.SHAPED,
                "rapid_press", 1, List.of("IRI", "RDR", "IPI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DISPENSER", 'P', "PISTON"),
                "fast_machinery"));
        registerRecipe(api, new RecipeDefinition("rapid_coil_winder_recipe", RecipeDefinition.Type.SHAPED,
                "rapid_coil_winder", 1, List.of("ICI", "RDR", "ICI"),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'R', "REDSTONE", 'D', "DISPENSER"),
                "fast_machinery"));

        registerMachine(api, new MachineDefinition("rapid_press_machine", "Prensa de alta velocidade",
                "rapid_press", "DISPENSER", "IRON_INGOT", 1,
                "mechanical_parts", 2, 20, "fast_machinery"));
        registerMachine(api, new MachineDefinition("rapid_coil_winder_machine", "Enrolador rápido de bobinas",
                "rapid_coil_winder", "DISPENSER", "COPPER_INGOT", 1,
                "copper_coil", 2, 40, "fast_machinery"));
    }
}

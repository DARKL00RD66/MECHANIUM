package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class IndustryModule extends AbstractBuiltinModule {
    @Override public String id() { return "industry"; }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_components", "Componentes industriais", "Peças e materiais para a indústria"));
        registerItem(api, new ItemDefinition("mechanical_parts", "&7Peças mecânicas", "IRON_INGOT",
                List.of("&8Componente industrial básico"), 0, 64, "industrial_components", "basic_mechanics"));
        registerItem(api, new ItemDefinition("copper_coil", "&6Bobina de cobre", "COPPER_INGOT",
                List.of("&8Componente para sistemas elétricos"), 0, 64, "industrial_components", "steam_power"));
        registerItem(api, new ItemDefinition("mechanical_press", "&6Prensa mecânica", "DISPENSER",
                List.of("&8Máquina industrial básica", "&7Entrada: primeiro espaço", "&7Saída: último espaço"),
                0, 1, "industrial_components", "basic_mechanics"));
        registerRecipe(api, new RecipeDefinition("mechanical_parts_recipe", RecipeDefinition.Type.SHAPED,
                "mechanical_parts", 4, List.of(" I ", "III"), Map.of('I', "IRON_INGOT"), "basic_mechanics"));
        registerRecipe(api, new RecipeDefinition("copper_coil_recipe", RecipeDefinition.Type.SHAPELESS,
                "copper_coil", 1, List.of(), Map.of('C', "COPPER_INGOT"), "steam_power"));
        registerRecipe(api, new RecipeDefinition("mechanical_press_recipe", RecipeDefinition.Type.SHAPED,
                "mechanical_press", 1, List.of("III", "IPI", "III"),
                Map.of('I', "IRON_INGOT", 'P', "PISTON"), "basic_mechanics"));
        registerMachine(api, new MachineDefinition("mechanical_press", "Prensa mecânica",
                "mechanical_press", "DISPENSER", "IRON_INGOT", 2,
                "mechanical_parts", 1, 100, "basic_mechanics"));
    }
    @Override protected List<TechnologyDefinition> technologies() {
        // Prerequisites are declared before dependent technologies because registration is ordered.
        return List.of(
            // Ancient
            new TechnologyDefinition("basic_mechanics", "Mecânica básica", "renaissance", 10, List.of()),
            new TechnologyDefinition("bronze_metallurgy", "Metalurgia do bronze", "ancient", 10, List.of()),
            new TechnologyDefinition("irrigation_systems", "Sistemas de irrigação", "ancient", 12, List.of()),
            new TechnologyDefinition("masonry_techniques", "Técnicas de alvenaria", "ancient", 8, List.of()),
            // Medieval
            new TechnologyDefinition("windmill_engineering", "Engenharia de moinhos", "medieval", 16, List.of("irrigation_systems")),
            new TechnologyDefinition("mechanical_clocks", "Relógios mecânicos", "medieval", 18, List.of("masonry_techniques")),
            new TechnologyDefinition("advanced_forging", "Forja avançada", "medieval", 20, List.of("bronze_metallurgy")),
            // Renaissance
            new TechnologyDefinition("printing_press", "Imprensa", "renaissance", 24, List.of("mechanical_clocks")),
            new TechnologyDefinition("navigation_instruments", "Instrumentos de navegação", "renaissance", 25, List.of("printing_press")),
            new TechnologyDefinition("scientific_method", "Método científico", "renaissance", 28, List.of("printing_press", "navigation_instruments")),
            // Industrial Revolution
            new TechnologyDefinition("steam_power", "Energia a vapor", "industrial_revolution", 35, List.of("basic_mechanics")),
            new TechnologyDefinition("mechanized_production", "Produção mecanizada", "industrial_revolution", 55, List.of("steam_power")),
            new TechnologyDefinition("textile_machinery", "Maquinário têxtil", "industrial_revolution", 32, List.of("steam_power")),
            new TechnologyDefinition("rail_transport", "Transporte ferroviário", "industrial_revolution", 38, List.of("mechanized_production")),
            new TechnologyDefinition("industrial_chemistry", "Química industrial", "industrial_revolution", 42, List.of("mechanized_production")),
            // Electrification
            new TechnologyDefinition("electric_motors", "Motores elétricos", "electrification", 44, List.of("industrial_chemistry")),
            new TechnologyDefinition("telegraph_network", "Rede telegráfica", "electrification", 32, List.of("electric_motors")),
            new TechnologyDefinition("electric_lighting", "Iluminação elétrica", "electrification", 40, List.of("electric_motors")),
            // Modern
            new TechnologyDefinition("industrial_efficiency", "Eficiência industrial", "modern", 52, List.of("mechanized_production")),
            new TechnologyDefinition("assembly_line", "Linha de montagem", "modern", 48, List.of("industrial_efficiency")),
            new TechnologyDefinition("synthetic_materials", "Materiais sintéticos", "modern", 46, List.of("industrial_efficiency")),
            new TechnologyDefinition("microelectronics", "Microeletrônica", "modern", 55, List.of("assembly_line")),
            // Atomic / post-war
            new TechnologyDefinition("nuclear_energy", "Energia nuclear civil", "atomic_age", 65, List.of("microelectronics")),
            new TechnologyDefinition("medical_imaging", "Diagnóstico por imagem", "atomic_age", 48, List.of("microelectronics")),
            new TechnologyDefinition("advanced_telecommunications", "Telecomunicações avançadas", "atomic_age", 50, List.of("microelectronics")),
            // Information age
            new TechnologyDefinition("computer_networks", "Redes de computadores", "information_age", 60, List.of("advanced_telecommunications")),
            new TechnologyDefinition("data_processing", "Processamento de dados", "information_age", 42, List.of("computer_networks")),
            new TechnologyDefinition("automated_control", "Controle automatizado", "information_age", 62, List.of("computer_networks")),
            // Digital age
            new TechnologyDefinition("internet_infrastructure", "Infraestrutura de internet", "digital_age", 70, List.of("computer_networks")),
            new TechnologyDefinition("cloud_computing", "Computação em nuvem", "digital_age", 72, List.of("internet_infrastructure")),
            new TechnologyDefinition("robotic_automation", "Automação robótica", "digital_age", 76, List.of("automated_control")),
            // Space age
            new TechnologyDefinition("satellite_navigation", "Navegação por satélite", "space_age", 80, List.of("internet_infrastructure")),
            new TechnologyDefinition("orbital_research", "Pesquisa orbital", "space_age", 85, List.of("satellite_navigation")),
            new TechnologyDefinition("space_industry", "Indústria espacial", "space_age", 90, List.of("orbital_research"))
        );
    }}
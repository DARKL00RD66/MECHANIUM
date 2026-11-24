package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Abstract, game-only biofuel production and energy storage items. */
final class BiofuelModule extends AbstractBuiltinModule {
    @Override public String id() { return "biofuel"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry","agriculture"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("biomass_processing", "Processamento de biomassa", "industrial_revolution", 28, List.of("crop_rotation","industrial_chemistry")),
            new TechnologyDefinition("biofuel_refining", "Refino de biocombustível", "modern", 44, List.of("biomass_processing","industrial_efficiency"))
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api=context.api();
        registerCategory(api,new ContentCategory("biofuel_goods","Biocombustíveis","Biomassa e combustíveis fictícios para o ecossistema do jogo"));
        registerItem(api,new ItemDefinition("biomass_pellet","&aPelota de biomassa","DRIED_KELP",List.of("&8Combustível sólido de origem vegetal"),0,64,"biofuel_goods","biomass_processing"));
        registerItem(api,new ItemDefinition("biofuel_cell","&eCélula de biocombustível","HONEY_BOTTLE",List.of("&8Combustível abstrato para máquinas MECHANIUM"),0,16,"biofuel_goods","biofuel_refining"));
        registerItem(api,new ItemDefinition("biomass_processor","&2Processador de biomassa","DISPENSER",List.of("&8Converte matéria vegetal em biomassa"),0,1,"biofuel_goods","biomass_processing"));
        registerRecipe(api,new RecipeDefinition("biomass_pellet_recipe",RecipeDefinition.Type.SHAPELESS,"biomass_pellet",4,List.of(),Map.of('K',"DRIED_KELP",'W',"WHEAT",'S',"SUGAR_CANE"),"biomass_processing"));
        registerRecipe(api,new RecipeDefinition("biofuel_cell_recipe",RecipeDefinition.Type.SHAPELESS,"biofuel_cell",1,List.of(),Map.of('B',"mechanium:biomass_pellet",'G',"GLASS_BOTTLE",'R',"REDSTONE"),"biofuel_refining"));
        registerRecipe(api,new RecipeDefinition("biomass_processor_recipe",RecipeDefinition.Type.SHAPELESS,"biomass_processor",1,List.of(),Map.of('D',"DISPENSER",'H',"HAY_BLOCK",'I',"IRON_INGOT"),"biomass_processing"));
        registerMachine(api,new MachineDefinition("biomass_processor","Processador de biomassa","biomass_processor","DISPENSER","WHEAT",4,"biomass_pellet",2,1200,1,"biomass_processing"));
    }
}

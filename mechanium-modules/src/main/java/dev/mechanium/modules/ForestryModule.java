package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Forestry products, sustainable timber processing and resin extraction. */
final class ForestryModule extends AbstractBuiltinModule {
    @Override public String id() { return "forestry"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("managed_forestry", "Manejo florestal", "medieval", 12, List.of("basic_mechanics")),
            new TechnologyDefinition("resin_extraction", "Extração de resina", "renaissance", 20, List.of("managed_forestry")),
            new TechnologyDefinition("industrial_sawmilling", "Serraria industrial", "industrial_revolution", 34, List.of("resin_extraction"))
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api=context.api();
        registerCategory(api,new ContentCategory("forestry_goods","Produtos florestais","Madeira processada, resinas e materiais de reflorestamento"));
        registerItem(api,new ItemDefinition("wood_pulp","&fPolpa de madeira","PAPER",List.of("&8Fibra vegetal processada"),0,64,"forestry_goods","managed_forestry"));
        registerItem(api,new ItemDefinition("tree_resin","&6Resina vegetal","HONEYCOMB",List.of("&8Subproduto da extração sustentável"),0,64,"forestry_goods","resin_extraction"));
        registerItem(api,new ItemDefinition("treated_planks","&6Tábuas tratadas","STRIPPED_OAK_LOG",List.of("&8Madeira preparada para uso industrial"),0,64,"forestry_goods","industrial_sawmilling"));
        registerItem(api,new ItemDefinition("forestry_station","&2Estação florestal","DISPENSER",List.of("&8Processa toras em fibras e resina"),0,1,"forestry_goods","industrial_sawmilling"));
        registerRecipe(api,new RecipeDefinition("wood_pulp_recipe",RecipeDefinition.Type.SHAPELESS,"wood_pulp",4,List.of(),Map.of('L',"OAK_LOG",'S',"SHEARS"),"managed_forestry"));
        registerRecipe(api,new RecipeDefinition("tree_resin_recipe",RecipeDefinition.Type.SHAPELESS,"tree_resin",2,List.of(),Map.of('H',"HONEYCOMB",'S',"SLIME_BALL"),"resin_extraction"));
        registerRecipe(api,new RecipeDefinition("treated_planks_recipe",RecipeDefinition.Type.SHAPELESS,"treated_planks",4,List.of(),Map.of('P',"mechanium:wood_pulp",'R',"mechanium:tree_resin",'O',"OAK_PLANKS"),"industrial_sawmilling"));
        registerRecipe(api,new RecipeDefinition("forestry_station_recipe",RecipeDefinition.Type.SHAPELESS,"forestry_station",1,List.of(),Map.of('D',"DISPENSER",'I',"IRON_INGOT",'L',"OAK_LOG"),"industrial_sawmilling"));
        registerMachine(api,new MachineDefinition("forestry_station","Estação florestal","forestry_station","DISPENSER","OAK_LOG",2,"wood_pulp",2,1200,"industrial_sawmilling"));
    }
}

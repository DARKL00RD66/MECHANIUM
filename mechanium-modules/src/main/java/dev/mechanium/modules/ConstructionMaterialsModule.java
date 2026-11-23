package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Construction materials and prefabricated structural components. */
final class ConstructionMaterialsModule extends AbstractBuiltinModule {
    @Override public String id() { return "construction-materials"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("industrial_ceramics", "Cerâmica industrial", "renaissance", 18, List.of("masonry_techniques")),
            new TechnologyDefinition("reinforced_concrete", "Concreto armado", "industrial_revolution", 34, List.of("industrial_ceramics","advanced_forging")),
            new TechnologyDefinition("prefabrication", "Construção pré-fabricada", "modern", 46, List.of("reinforced_concrete","industrial_efficiency"))
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api=context.api();
        registerCategory(api,new ContentCategory("construction_materials","Materiais de construção","Blocos e componentes estruturais industrializados"));
        registerItem(api,new ItemDefinition("ceramic_brick","&6Tijolo cerâmico","BRICK",List.of("&8Peça cerâmica padronizada"),0,64,"construction_materials","industrial_ceramics"));
        registerItem(api,new ItemDefinition("reinforced_panel","&7Painel reforçado","GRAY_CONCRETE",List.of("&8Painel estrutural pré-fabricado"),0,64,"construction_materials","reinforced_concrete"));
        registerItem(api,new ItemDefinition("prefab_frame","&fEstrutura pré-fabricada","IRON_BARS",List.of("&8Componente estrutural modular"),0,64,"construction_materials","prefabrication"));
        registerItem(api,new ItemDefinition("industrial_mixer","&6Misturador industrial","DISPENSER",List.of("&8Processa materiais de construção"),0,1,"construction_materials","reinforced_concrete"));
        registerRecipe(api,new RecipeDefinition("ceramic_brick_recipe",RecipeDefinition.Type.SHAPELESS,"ceramic_brick",4,List.of(),Map.of('C',"CLAY_BALL",'B',"BRICK"),"industrial_ceramics"));
        registerRecipe(api,new RecipeDefinition("reinforced_panel_recipe",RecipeDefinition.Type.SHAPELESS,"reinforced_panel",2,List.of(),Map.of('C',"CONCRETE",'I',"IRON_INGOT"),"reinforced_concrete"));
        registerRecipe(api,new RecipeDefinition("prefab_frame_recipe",RecipeDefinition.Type.SHAPELESS,"prefab_frame",2,List.of(),Map.of('I',"IRON_BARS",'S',"mechanium:steel_component"),"prefabrication"));
        registerRecipe(api,new RecipeDefinition("industrial_mixer_recipe",RecipeDefinition.Type.SHAPELESS,"industrial_mixer",1,List.of(),Map.of('D',"DISPENSER",'C',"CAULDRON",'I',"IRON_INGOT"),"reinforced_concrete"));
        registerMachine(api,new MachineDefinition("industrial_mixer","Misturador industrial","industrial_mixer","DISPENSER","CLAY_BALL",4,"ceramic_brick",2,1200,"reinforced_concrete"));
    }
}

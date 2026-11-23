package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Textile manufacturing chain, from fibers to industrial fabric. */
final class TextileIndustryModule extends AbstractBuiltinModule {
    @Override public String id() { return "textile-industry"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("mechanized_textiles", "Tecelagem mecanizada", "industrial_revolution", 26, List.of("textile_machinery")),
            new TechnologyDefinition("technical_fabrics", "Tecidos técnicos", "modern", 40, List.of("mechanized_textiles"))
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api=context.api();
        registerCategory(api,new ContentCategory("textile_goods","Indústria têxtil","Fibras, tecidos e componentes têxteis"));
        registerItem(api,new ItemDefinition("cotton_fiber","&fFibra de algodão","STRING",List.of("&8Fibra preparada para tecelagem"),0,64,"textile_goods","mechanized_textiles"));
        registerItem(api,new ItemDefinition("industrial_fabric","&bTecido industrial","WHITE_WOOL",List.of("&8Tecido produzido em linha mecanizada"),0,64,"textile_goods","mechanized_textiles"));
        registerItem(api,new ItemDefinition("reinforced_fabric","&7Tecido reforçado","GRAY_WOOL",List.of("&8Material técnico de alta resistência"),0,64,"textile_goods","technical_fabrics"));
        registerItem(api,new ItemDefinition("textile_loom","&6Tear industrial","DISPENSER",List.of("&8Máquina de produção têxtil"),0,1,"textile_goods","mechanized_textiles"));
        registerRecipe(api,new RecipeDefinition("cotton_fiber_recipe",RecipeDefinition.Type.SHAPELESS,"cotton_fiber",3,List.of(),Map.of('W',"WHITE_WOOL",'S',"STRING"),"mechanized_textiles"));
        registerRecipe(api,new RecipeDefinition("industrial_fabric_recipe",RecipeDefinition.Type.SHAPELESS,"industrial_fabric",2,List.of(),Map.of('F',"mechanium:cotton_fiber",'L',"LOOM"),"mechanized_textiles"));
        registerRecipe(api,new RecipeDefinition("reinforced_fabric_recipe",RecipeDefinition.Type.SHAPELESS,"reinforced_fabric",1,List.of(),Map.of('F',"mechanium:industrial_fabric",'I',"IRON_INGOT"),"technical_fabrics"));
        registerRecipe(api,new RecipeDefinition("textile_loom_recipe",RecipeDefinition.Type.SHAPELESS,"textile_loom",1,List.of(),Map.of('D',"DISPENSER",'S',"STRING",'I',"IRON_INGOT"),"mechanized_textiles"));
        registerMachine(api,new MachineDefinition("textile_loom","Tear industrial","textile_loom","DISPENSER","mechanium:cotton_fiber",2,"industrial_fabric",1,1600,"mechanized_textiles"));
    }
}

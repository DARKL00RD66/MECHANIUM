package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class AgricultureModule extends AbstractBuiltinModule {
    @Override public String id() { return "agriculture"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("crop_rotation", "Rotação de culturas", "medieval", 8, List.of()),
            new TechnologyDefinition("industrial_fertilizer", "Fertilizantes industriais", "industrial_revolution", 30, List.of("crop_rotation"))
        );
    }
}
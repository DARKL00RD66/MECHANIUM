package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

/** Speculative future branch. Separate from historical progression and disabled by default. */
public final class OptionalFuturisticModule extends AbstractBuiltinModule {
    @Override public String id() { return "optional-futuristic"; }
    @Override public boolean optional() { return true; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("synthetic_intelligence", "Inteligência sintética", "speculative_future", 90, List.of()),
            new TechnologyDefinition("advanced_materials", "Materiais especulativos", "speculative_future", 110, List.of("synthetic_intelligence"))
        );
    }
    @Override protected void registerExtra(MechaniumModuleContext context) {
        context.api().registerEra(new EraDefinition("speculative_future", "Futuro especulativo (opcional)", 110));
    }
}
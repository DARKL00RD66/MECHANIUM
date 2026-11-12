package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

/** Fictional branch. Separate, optional and disabled by default. */
public final class OptionalFantasyModule extends AbstractBuiltinModule {
    @Override public String id() { return "optional-fantasy"; }
    @Override public boolean optional() { return true; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("arcane_alchemy", "Alquimia arcana", "mythic_age", 40, List.of()),
            new TechnologyDefinition("arcane_conduits", "Condutores arcanos", "mythic_age", 65, List.of("arcane_alchemy"))
        );
    }
    @Override protected void registerExtra(MechaniumModuleContext context) {
        context.api().registerEra(new EraDefinition("mythic_age", "Era Mítica (opcional)", 100));
    }
}
package dev.mechanium.modules;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinModulesTest {
    @Test
    void includesAllRegisteredAddonDomainModules() {
        Set<String> ids = BuiltinModules.create().stream()
                .map(module -> module.id())
                .collect(Collectors.toSet());

        assertTrue(ids.containsAll(Set.of(
                "extra-gear",
                "eco-power",
                "sfcalc",
                "liquid",
                "advanced-tech",
                "simple-storage",
                "sensible-toolbox",
                "agriculture",
                "exotic-garden",
                "dank-tech2",
                "culinary",
                "environment",
                "electric-spawners",
                "fast-machines",
                "hardcore-slimefun",
                "air-filtration",
                "advanced-metallurgy",
                "pressure-engineering",
                "industrial-automation",
                "industrial-logistics",
                "forestry",
                "textile-industry",
                "biofuel",
                "construction-materials",
                "industrial-networks",
                "better-reactor",
                "material-generators",
                "water-treatment",
                "greenhouse",
                "industrial-recycling",
                "ore-chunks",
                "aquaculture",
                "glassworks",
                "rail-freight",
                "battery-industry",
                "cold-chain",
                "fn-amplifications", "head-collection", "slime-customizer", "industrial-chemistry", "mining-automation",
                "emc-exchange"
        )));
    }

    @Test
    void allBuiltinModuleDependenciesResolveWithoutCycles() {
        assertDoesNotThrow(() -> ModuleManager.orderByDependencies(BuiltinModules.create()));
    }

    @Test
    void customHistoricalTechnologyDistributionKeepsAntiquityFocusedOnFoundations() {
        Map<String, dev.mechanium.api.model.TechnologyDefinition> technologies = new HashMap<>();
        for (var module : BuiltinModules.create()) {
            if (!(module instanceof AbstractBuiltinModule builtin)) continue;
            for (var technology : builtin.technologies()) {
                technologies.put(technology.id(), technology);
            }
        }

        Set<String> ancientTechnologies = technologies.values().stream()
                .filter(technology -> technology.requiredEraId().equals("ancient"))
                .map(dev.mechanium.api.model.TechnologyDefinition::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of("bronze_metallurgy", "irrigation_systems", "masonry_techniques"),
                ancientTechnologies);
        assertEquals("renaissance", technologies.get("basic_mechanics").requiredEraId());
        assertEquals("industrial_revolution", technologies.get("steam_power").requiredEraId());
    }

    @Test
    void everyCrossModuleTechnologyPrerequisiteHasAnExplicitModuleDependency() {
        var modules = BuiltinModules.create();
        Map<String, String> technologyOwners = new HashMap<>();

        for (var module : modules) {
            if (!(module instanceof AbstractBuiltinModule builtin)) continue;
            for (var technology : builtin.technologies()) {
                String previous = technologyOwners.putIfAbsent(technology.id(), module.id());
                assertNull(previous, "Duplicate technology ID: " + technology.id());
            }
        }

        for (var module : modules) {
            if (!(module instanceof AbstractBuiltinModule builtin)) continue;
            for (var technology : builtin.technologies()) {
                for (String prerequisite : technology.prerequisites()) {
                    String provider = technologyOwners.get(prerequisite);
                    assertNotNull(provider, "Unknown prerequisite " + prerequisite + " in " + technology.id());
                    if (!provider.equals(module.id())) {
                        assertTrue(module.dependencies().contains(provider),
                                module.id() + " must explicitly depend on " + provider
                                        + " because " + technology.id() + " requires " + prerequisite);
                    }
                }
            }
        }
    }

    @Test
    void modulesDependingOnOptionalModulesMustAlsoBeOptional() {
        var modules = BuiltinModules.create();
        Map<String, dev.mechanium.api.MechaniumModule> byId = modules.stream()
                .collect(Collectors.toMap(dev.mechanium.api.MechaniumModule::id, module -> module));

        for (var module : modules) {
            for (String dependencyId : module.dependencies()) {
                var dependency = byId.get(dependencyId);
                assertNotNull(dependency, "Missing module dependency: " + dependencyId);
                if (dependency.optional()) {
                    assertTrue(module.optional(), module.id()
                            + " depends on optional module " + dependencyId + " and must also be optional");
                }
            }
        }
    }

}

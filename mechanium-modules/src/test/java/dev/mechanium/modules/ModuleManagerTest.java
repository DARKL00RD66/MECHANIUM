package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import dev.mechanium.api.MechaniumModuleContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerTest {
    @Test
    void sortsModulesIntoDependencyOrderInsteadOfRequiringCallerOrder() {
        MechaniumModule dependent = new StubModule("geology", Set.of("industry"));
        MechaniumModule base = new StubModule("industry", Set.of());

        List<MechaniumModule> ordered = ModuleManager.orderByDependencies(List.of(dependent, base));

        assertEquals(List.of("industry", "geology"), ordered.stream().map(MechaniumModule::id).toList());
    }

    @Test
    void rejectsUnknownInternalDependency() {
        MechaniumModule invalid = new StubModule("industry", Set.of("missing"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ModuleManager.orderByDependencies(List.of(invalid)));

        assertTrue(error.getMessage().contains("dependência interna inexistente"));
    }

    @Test
    void rejectsCircularDependencies() {
        MechaniumModule first = new StubModule("industry", Set.of("energy"));
        MechaniumModule second = new StubModule("energy", Set.of("industry"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ModuleManager.orderByDependencies(List.of(first, second)));

        assertTrue(error.getMessage().contains("Dependências circulares"));
    }

    @Test
    void rejectsDuplicateIds() {
        MechaniumModule first = new StubModule("industry", Set.of());
        MechaniumModule second = new StubModule("industry", Set.of());

        assertThrows(IllegalArgumentException.class,
                () -> ModuleManager.orderByDependencies(List.of(first, second)));
    }

    private record StubModule(String id, Set<String> dependencies) implements MechaniumModule {
        @Override public void onEnable(MechaniumModuleContext context) { }
        @Override public void onDisable() { }
    }
}

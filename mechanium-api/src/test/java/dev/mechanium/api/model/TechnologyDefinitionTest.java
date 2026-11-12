package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TechnologyDefinitionTest {
    @Test
    void normalizesIdentifiersAndDeduplicatesPrerequisites() {
        var value = new TechnologyDefinition(" Steam_Power ", "Steam Power", "Industrial_Revolution", 30,
                List.of("Basic_Mechanics", "basic_mechanics"));
        assertEquals("steam_power", value.id());
        assertEquals("industrial_revolution", value.requiredEraId());
        assertEquals(List.of("basic_mechanics"), value.prerequisites());
    }

    @Test
    void rejectsNegativeCost() {
        assertThrows(IllegalArgumentException.class, () ->
                new TechnologyDefinition("steam_power", "Steam Power", "industrial_revolution", -1, List.of()));
    }

    @Test
    void rejectsSelfDependency() {
        assertThrows(IllegalArgumentException.class, () ->
                new TechnologyDefinition("steam_power", "Steam Power", "industrial_revolution", 0, List.of("steam_power")));
    }
}
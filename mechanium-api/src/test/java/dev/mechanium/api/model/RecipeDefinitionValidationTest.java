package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecipeDefinitionValidationTest {
    @Test
    void acceptsShapedRecipeWithUniformRows() {
        assertDoesNotThrow(() -> new RecipeDefinition(
                "industrial_plate_recipe",
                RecipeDefinition.Type.SHAPED,
                "industrial_plate",
                1,
                List.of("III", " I "),
                Map.of('I', "IRON_INGOT"),
                null
        ));
    }

    @Test
    void rejectsShapedRecipeWithDifferentRowWidths() {
        assertThrows(IllegalArgumentException.class, () -> new RecipeDefinition(
                "invalid_recipe",
                RecipeDefinition.Type.SHAPED,
                "industrial_plate",
                1,
                List.of("III", " I"),
                Map.of('I', "IRON_INGOT"),
                null
        ));
    }

    @Test
    void rejectsRowsContainingOnlySpaces() {
        assertThrows(IllegalArgumentException.class, () -> new RecipeDefinition(
                "blank_row_recipe",
                RecipeDefinition.Type.SHAPED,
                "industrial_plate",
                1,
                List.of("   ", "III"),
                Map.of('I', "IRON_INGOT"),
                null
        ));
    }
}

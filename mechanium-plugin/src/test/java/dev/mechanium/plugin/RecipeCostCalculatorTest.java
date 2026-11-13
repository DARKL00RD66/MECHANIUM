package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.RecipeDefinition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RecipeCostCalculatorTest {
    @Test
    void expandsShapedIntermediateRecipesAndRoundsUpBatches() {
        RecipeDefinition rawPart = new RecipeDefinition(
                "raw_part_recipe", RecipeDefinition.Type.SHAPED, "raw_part", 2,
                List.of("II"), Map.of('I', "IRON_INGOT"), null);
        RecipeDefinition gear = new RecipeDefinition(
                "gear_recipe", RecipeDefinition.Type.SHAPED, "gear", 2,
                List.of("RRC"), Map.of('R', "mechanium:raw_part", 'C', "COPPER_INGOT"), null);

        RecipeCostCalculator.Calculation result =
                new RecipeCostCalculator(apiWith(rawPart, gear)).calculate("gear_recipe", 5);

        assertEquals(3, result.batches());
        assertEquals(6, result.produced());
        assertEquals(6L, result.materials().get("IRON_INGOT"));
        assertEquals(3L, result.materials().get("COPPER_INGOT"));
    }

    @Test
    void keepsUnknownMechaniumIntermediateAsExplicitCost() {
        RecipeDefinition recipe = new RecipeDefinition(
                "machine_recipe", RecipeDefinition.Type.SHAPELESS, "machine_parts", 1,
                List.of(), Map.of('P', "mechanium:unavailable_component"), null);

        RecipeCostCalculator.Calculation result =
                new RecipeCostCalculator(apiWith(recipe)).calculate("machine_recipe", 4);

        assertEquals(4L, result.materials().get("mechanium:unavailable_component"));
    }

    @Test
    void rejectsUnreasonablyLargeRequestedAmounts() {
        RecipeDefinition recipe = new RecipeDefinition(
                "simple_recipe", RecipeDefinition.Type.SHAPELESS, "simple_item", 1,
                List.of(), Map.of('I', "IRON_INGOT"), null);
        RecipeCostCalculator calculator = new RecipeCostCalculator(apiWith(recipe));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("simple_recipe", 100_001));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("simple_recipe", 0));
    }

    private MechaniumApi apiWith(RecipeDefinition... definitions) {
        List<RecipeDefinition> recipes = List.of(definitions);
        return (MechaniumApi) Proxy.newProxyInstance(
                MechaniumApi.class.getClassLoader(),
                new Class<?>[]{MechaniumApi.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRecipe" -> recipes.stream()
                            .filter(recipe -> recipe.id().equals(args[0]))
                            .findFirst();
                    case "getRecipes" -> recipes;
                    case "toString" -> "RecipeCostCalculatorTestApi";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}

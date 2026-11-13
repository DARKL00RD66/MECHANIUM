package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.RecipeDefinition;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

/** Expands a recipe tree into material totals, preserving unresolved MECHANIUM intermediates. */
final class RecipeCostCalculator {
    private final MechaniumApi api;

    RecipeCostCalculator(MechaniumApi api) {
        this.api = api;
    }

    Calculation calculate(String recipeId, long requestedOutput) {
        if (requestedOutput < 1 || requestedOutput > 100_000L) {
            throw new IllegalArgumentException("A quantidade deve estar entre 1 e 100.000.");
        }
        RecipeDefinition recipe = api.getRecipe(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada: " + recipeId));
        long batches = divideRoundUp(requestedOutput, recipe.resultAmount());
        Map<String, Long> materials = new TreeMap<>();
        expand(recipe, batches, new LinkedHashSet<>(), materials, 0);
        return new Calculation(recipe, batches, batches * recipe.resultAmount(), Map.copyOf(materials));
    }

    private void expand(RecipeDefinition recipe, long batches, LinkedHashSet<String> path,
                        Map<String, Long> totals, int depth) {
        if (depth > 8 || !path.add(recipe.resultItemId())) {
            totals.merge("mechanium:" + recipe.resultItemId(), batches * recipe.resultAmount(), Long::sum);
            return;
        }
        for (Map.Entry<Character, String> entry : recipe.ingredients().entrySet()) {
            long perBatch = ingredientCount(recipe, entry.getKey());
            long required = perBatch * batches;
            String ingredient = entry.getValue();
            if (ingredient.startsWith("mechanium:")) {
                String ingredientId = ingredient.substring(10);
                RecipeDefinition subRecipe = api.getRecipes().stream()
                        .filter(candidate -> candidate.resultItemId().equals(ingredientId))
                        .findFirst().orElse(null);
                if (subRecipe != null && !path.contains(ingredientId)) {
                    expand(subRecipe, divideRoundUp(required, subRecipe.resultAmount()), path, totals, depth + 1);
                } else {
                    totals.merge(ingredient, required, Long::sum);
                }
            } else {
                totals.merge(ingredient, required, Long::sum);
            }
        }
        path.remove(recipe.resultItemId());
    }

    private long ingredientCount(RecipeDefinition recipe, char key) {
        if (recipe.type() == RecipeDefinition.Type.SHAPELESS) return 1;
        long count = 0;
        for (String row : recipe.shape()) {
            for (int i = 0; i < row.length(); i++) if (row.charAt(i) == key) count++;
        }
        return count;
    }

    private long divideRoundUp(long numerator, long denominator) {
        return 1 + (numerator - 1) / denominator;
    }

    record Calculation(RecipeDefinition recipe, long batches, long produced, Map<String, Long> materials) { }
}

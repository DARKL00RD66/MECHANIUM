package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobDropDefinitionTest {
    @Test
    void normalizesIdsAndEntityType() {
        MobDropDefinition drop = new MobDropDefinition(
                " Cow_Fat ", "cow", "Animal_Fat", 1, 2, 0.35, "crop_rotation");
        assertEquals("cow_fat", drop.id());
        assertEquals("COW", drop.entityType());
        assertEquals("animal_fat", drop.resultItemId());
        assertEquals("crop_rotation", drop.requiredTechnologyId());
    }

    @Test
    void acceptsDropsWithoutResearchGate() {
        MobDropDefinition drop = new MobDropDefinition(
                "sheep_wool", "SHEEP", "wool_bundle", 1, 3, 1.0, null);
        assertNull(drop.requiredTechnologyId());
    }

    @Test
    void rejectsInvalidChanceAndAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad_chance", "COW", "animal_fat", 1, 1, Double.NaN, null));
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad_chance", "COW", "animal_fat", 1, 1, 1.01, null));
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad_amount", "COW", "animal_fat", 0, 1, 0.5, null));
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad_amount", "COW", "animal_fat", 3, 2, 0.5, null));
    }

    @Test
    void rejectsInvalidEntityTypeAndIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad_entity", "minecraft:cow", "animal_fat", 1, 1, 0.5, null));
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "unknown_entity", "NOT_A_REAL_MOB", "animal_fat", 1, 1, 0.5, null));
        assertThrows(IllegalArgumentException.class, () -> new MobDropDefinition(
                "bad id", "COW", "animal_fat", 1, 1, 0.5, null));
    }
}

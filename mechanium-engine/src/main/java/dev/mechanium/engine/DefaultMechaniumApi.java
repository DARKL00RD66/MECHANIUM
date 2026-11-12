package dev.mechanium.engine;

import dev.mechanium.api.*;
import dev.mechanium.api.event.*;
import dev.mechanium.api.model.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Progression and content registry. All content access is validated against research and era state. */
public final class DefaultMechaniumApi implements MechaniumApi {
    private final MechaniumDataStore dataStore;
    private final MechaniumContentStore contentStore;
    private final Map<String, EraDefinition> eras = new ConcurrentHashMap<>();
    private final Map<String, TechnologyDefinition> technologies = new ConcurrentHashMap<>();
    private final Map<String, ContentCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, ItemDefinition> items = new ConcurrentHashMap<>();
    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();
    private final Set<String> modules = ConcurrentHashMap.newKeySet();

    public DefaultMechaniumApi(MechaniumDataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.contentStore = dataStore instanceof MechaniumContentStore store ? store : null;
        if (contentStore != null) {
            contentStore.loadCategories().forEach(value -> categories.put(value.id(), value));
            contentStore.loadItems().forEach(value -> items.put(value.id(), value));
            contentStore.loadRecipes().forEach(value -> recipes.put(value.id(), value));
        }
        categories.putIfAbsent("general", new ContentCategory("general", "Geral", "Conteúdo geral"));
    }

    @Override public String getApiVersion() { return API_VERSION; }
    @Override public boolean registerEra(EraDefinition era) {
        Objects.requireNonNull(era, "era");
        EraDefinition old = eras.putIfAbsent(era.id(), era);
        return old == null || old.equals(era);
    }
    @Override public Optional<EraDefinition> getEra(String id) { return Optional.ofNullable(eras.get(normalize(id))); }
    @Override public Collection<EraDefinition> getEras() { return eras.values().stream().sorted(Comparator.comparingInt(EraDefinition::order)).toList(); }
    @Override public Optional<String> getPlayerEra(UUID id) { return dataStore.getPlayerEra(Objects.requireNonNull(id)); }
    @Override public void setPlayerEra(UUID player, String era) {
        String id = normalize(era);
        if (!eras.containsKey(id)) throw new IllegalArgumentException("Unknown era: " + era);
        dataStore.setPlayerEra(Objects.requireNonNull(player), id);
    }
    @Override public long getResearchPoints(UUID id) { return dataStore.getResearchPoints(Objects.requireNonNull(id)); }
    @Override public void addResearchPoints(UUID id, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Research point grants must be non-negative");
        dataStore.addResearchPoints(Objects.requireNonNull(id), amount);
    }
    @Override public boolean registerTechnology(TechnologyDefinition value) {
        Objects.requireNonNull(value);
        if (!eras.containsKey(value.requiredEraId())) throw new IllegalArgumentException("Technology references unregistered era: " + value.requiredEraId());
        for (String prerequisite : value.prerequisites()) if (!technologies.containsKey(prerequisite))
            throw new IllegalArgumentException("Unknown technology prerequisite: " + prerequisite);
        TechnologyDefinition old = technologies.putIfAbsent(value.id(), value);
        return old == null || old.equals(value);
    }
    @Override public Optional<TechnologyDefinition> getTechnology(String id) { return Optional.ofNullable(technologies.get(normalize(id))); }
    @Override public Collection<TechnologyDefinition> getTechnologies() { return technologies.values().stream().sorted(Comparator.comparing(TechnologyDefinition::id)).toList(); }

    @Override public UnlockResult unlockTechnology(UUID playerId, String technologyId) {
        Objects.requireNonNull(playerId);
        TechnologyDefinition technology = technologies.get(normalize(technologyId));
        if (technology == null) return UnlockResult.of(UnlockResult.Status.UNKNOWN_TECHNOLOGY, "Tecnologia desconhecida.");
        if (dataStore.hasUnlocked(playerId, technology.id())) return UnlockResult.of(UnlockResult.Status.ALREADY_UNLOCKED, "Tecnologia já desbloqueada.");
        Optional<String> currentId = dataStore.getPlayerEra(playerId);
        if (currentId.isEmpty() || !eras.containsKey(currentId.get())) return UnlockResult.of(UnlockResult.Status.UNKNOWN_PLAYER_ERA, "Jogador ainda não possui uma era válida.");
        if (eras.get(currentId.get()).order() < eras.get(technology.requiredEraId()).order())
            return UnlockResult.of(UnlockResult.Status.WRONG_ERA, "Esta tecnologia exige a era " + eras.get(technology.requiredEraId()).displayName() + ".");
        List<String> missing = technology.prerequisites().stream().filter(id -> !dataStore.hasUnlocked(playerId, id)).toList();
        if (!missing.isEmpty()) return UnlockResult.of(UnlockResult.Status.MISSING_PREREQUISITES, "Pesquisas necessárias: " + String.join(", ", missing));
        if (dataStore.getResearchPoints(playerId) < technology.researchCost()) return UnlockResult.of(UnlockResult.Status.INSUFFICIENT_RESEARCH_POINTS, "Pontos científicos insuficientes. Custo: " + technology.researchCost() + ".");
        if (!dataStore.spendAndUnlock(playerId, technology.id(), technology.researchCost())) {
            if (dataStore.hasUnlocked(playerId, technology.id())) return UnlockResult.of(UnlockResult.Status.ALREADY_UNLOCKED, "Tecnologia já desbloqueada.");
            return UnlockResult.of(UnlockResult.Status.CONCURRENT_UPDATE, "O estado científico mudou simultaneamente. Tente novamente.");
        }
        return UnlockResult.of(UnlockResult.Status.UNLOCKED, "Tecnologia desbloqueada: " + technology.displayName());
    }
    @Override public boolean canUseTechnology(UUID player, String id) {
        TechnologyDefinition technology = technologies.get(normalize(id));
        if (technology == null || !dataStore.hasUnlocked(player, technology.id())) return false;
        Optional<String> eraId = dataStore.getPlayerEra(player);
        if (eraId.isEmpty()) return false;
        EraDefinition current = eras.get(eraId.get()), required = eras.get(technology.requiredEraId());
        return current != null && required != null && current.order() >= required.order();
    }
    @Override public void registerModule(String id) { modules.add(normalize(id)); }
    @Override public Set<String> getRegisteredModules() { return Collections.unmodifiableSet(new TreeSet<>(modules)); }

    @Override public boolean registerCategory(ContentCategory value) {
        Objects.requireNonNull(value);
        ContentCategory old = categories.putIfAbsent(value.id(), value);
        if (old != null) return old.equals(value);
        persist(() -> contentStore.saveCategory(value));
        if (Bukkit.getServer() != null) Bukkit.getPluginManager().callEvent(new MechaniumCategoryRegisterEvent(value));
        return true;
    }
    @Override public Optional<ContentCategory> getCategory(String id) { return Optional.ofNullable(categories.get(normalize(id))); }
    @Override public Collection<ContentCategory> getCategories() { return categories.values().stream().sorted(Comparator.comparing(ContentCategory::id)).toList(); }

    @Override public boolean registerItem(ItemDefinition value) {
        Objects.requireNonNull(value);
        if (!categories.containsKey(value.categoryId())) throw new IllegalArgumentException("Unknown item category: " + value.categoryId());
        validateTechnology(value.requiredTechnologyId());
        ItemDefinition old = items.putIfAbsent(value.id(), value);
        if (old != null) return old.equals(value);
        persist(() -> contentStore.saveItem(value));
        if (Bukkit.getServer() != null) Bukkit.getPluginManager().callEvent(new MechaniumItemRegisterEvent(value));
        return true;
    }
    @Override public Optional<ItemDefinition> getItem(String id) { return Optional.ofNullable(items.get(normalize(id))); }
    @Override public Collection<ItemDefinition> getItems() { return items.values().stream().sorted(Comparator.comparing(ItemDefinition::id)).toList(); }
    @Override public Optional<ItemStack> createItemStack(String id, int amount) {
        ItemDefinition definition = items.get(normalize(id));
        if (definition == null || amount < 1 || amount > definition.maxStackSize()) return Optional.empty();
        ItemStack stack = new ItemStack(Material.valueOf(definition.material()), amount);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey("mechanium", "item_id"), PersistentDataType.STRING, definition.id());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', definition.displayName()));
        if (!definition.lore().isEmpty()) meta.setLore(definition.lore().stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
        if (definition.customModelData() > 0) meta.setCustomModelData(definition.customModelData());
        stack.setItemMeta(meta);
        return Optional.of(stack);
    }
    @Override public boolean canUseItem(UUID player, String id) {
        ItemDefinition item = items.get(normalize(id));
        return item != null && (item.requiredTechnologyId() == null || canUseTechnology(player, item.requiredTechnologyId()));
    }

    @Override public boolean registerRecipe(RecipeDefinition value) {
        Objects.requireNonNull(value);
        if (!items.containsKey(value.resultItemId())) throw new IllegalArgumentException("Recipe result item is not registered: " + value.resultItemId());
        if (value.requiredTechnologyId() != null) validateTechnology(value.requiredTechnologyId());
        RecipeDefinition old = recipes.putIfAbsent(value.id(), value);
        if (old != null) return old.equals(value);
        persist(() -> contentStore.saveRecipe(value));
        if (Bukkit.getServer() != null) Bukkit.getPluginManager().callEvent(new MechaniumRecipeRegisterEvent(value));
        return true;
    }
    @Override public Optional<RecipeDefinition> getRecipe(String id) { return Optional.ofNullable(recipes.get(normalize(id))); }
    @Override public Collection<RecipeDefinition> getRecipes() { return recipes.values().stream().sorted(Comparator.comparing(RecipeDefinition::id)).toList(); }
    @Override public boolean canCraft(UUID player, String id) {
        RecipeDefinition recipe = recipes.get(normalize(id));
        if (recipe == null) return false;
        ItemDefinition result = items.get(recipe.resultItemId());
        return result != null && canUseItem(player, result.id())
                && (recipe.requiredTechnologyId() == null || canUseTechnology(player, recipe.requiredTechnologyId()));
    }
    private void validateTechnology(String id) {
        if (id != null && !technologies.containsKey(id)) throw new IllegalArgumentException("Unknown required technology: " + id);
    }
    private void persist(Runnable action) {
        if (contentStore != null) action.run();
    }
    private String normalize(String value) {
        Objects.requireNonNull(value, "id");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

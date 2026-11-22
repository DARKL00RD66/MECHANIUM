package dev.mechanium.engine;

import dev.mechanium.api.*;
import dev.mechanium.api.event.*;
import dev.mechanium.api.model.*;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Progression and content registry. All content access is validated against research and era state. */
public final class DefaultMechaniumApi implements MechaniumApi {
    private static final Set<String> SUPPORTED_MACHINE_BLOCKS = Set.of("DISPENSER");
    private final MechaniumDataStore dataStore;
    private final MechaniumContentStore contentStore;
    private final Map<String, EraDefinition> eras = new ConcurrentHashMap<>();
    private final Map<String, TechnologyDefinition> technologies = new ConcurrentHashMap<>();
    private final Map<String, ContentCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, ItemDefinition> items = new ConcurrentHashMap<>();
    private final Map<String, RecipeDefinition> recipes = new ConcurrentHashMap<>();
    private final Map<String, MobDropDefinition> mobDrops = new ConcurrentHashMap<>();
    private final Map<String, MachineDefinition> machines = new ConcurrentHashMap<>();
    private final Set<String> modules = ConcurrentHashMap.newKeySet();
    private ContentScope activeContentScope;

    public DefaultMechaniumApi(MechaniumDataStore dataStore) {
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.contentStore = dataStore instanceof MechaniumContentStore store ? store : null;
        if (contentStore != null) {
            contentStore.loadCategories().forEach(value -> categories.put(value.id(), value));
            contentStore.loadItems().forEach(value -> items.put(value.id(), value));
            contentStore.loadRecipes().forEach(value -> recipes.put(value.id(), value));
            contentStore.loadMobDrops().forEach(value -> mobDrops.put(value.id(), value));
            contentStore.loadMachines().forEach(value -> machines.put(value.id(), value));
        }
        categories.putIfAbsent("general", new ContentCategory("general", "Geral", "Conteúdo geral"));
    }

    @Override public String getApiVersion() { return API_VERSION; }

    @Override
    public synchronized MechaniumContentRegistrationScope openContentScope(String ownerId) {
        String normalizedOwner = normalize(ownerId);
        if (normalizedOwner.isBlank()) throw new IllegalArgumentException("Content owner cannot be blank.");
        if (activeContentScope != null) throw new IllegalStateException("Nested MECHANIUM content scopes are not supported.");
        activeContentScope = new ContentScope(normalizedOwner);
        return activeContentScope;
    }

    @Override
    public synchronized void pruneInactiveContentOwners(Set<String> activeOwnerIds) {
        if (activeContentScope != null) throw new IllegalStateException("Cannot prune content while a registration scope is active.");
        if (contentStore == null) return;
        contentStore.beginContentTransaction();
        try {
            Map<String, Set<String>> removed = contentStore.pruneInactiveContentOwners(
                    activeOwnerIds == null ? Set.of() : Set.copyOf(activeOwnerIds));
            contentStore.commitContentTransaction();
            removeContent(removed);
        } catch (RuntimeException exception) {
            try { contentStore.rollbackContentTransaction(); }
            catch (RuntimeException rollbackException) { exception.addSuppressed(rollbackException); }
            throw exception;
        }
    }


    private void publishContentEvent(Runnable action) {
        if (Bukkit.getServer() == null) return;
        if (activeContentScope != null) activeContentScope.pendingEvents.add(action);
        else action.run();
    }

    private void trackContent(String type, String id) {
        if (activeContentScope != null) activeContentScope.track(type, id);
    }

    private void removeContent(Map<String, Set<String>> removed) {
        if (removed == null || removed.isEmpty()) return;
        removed.getOrDefault(MechaniumContentStore.CATEGORY, Set.of()).forEach(categories::remove);
        removed.getOrDefault(MechaniumContentStore.ITEM, Set.of()).forEach(items::remove);
        removed.getOrDefault(MechaniumContentStore.RECIPE, Set.of()).forEach(recipes::remove);
        removed.getOrDefault(MechaniumContentStore.MOB_DROP, Set.of()).forEach(mobDrops::remove);
        removed.getOrDefault(MechaniumContentStore.MACHINE, Set.of()).forEach(machines::remove);
    }
    @Override public boolean registerEra(EraDefinition era) {
        Objects.requireNonNull(era, "era");
        EraDefinition old = eras.putIfAbsent(era.id(), era);
        return old == null || old.equals(era);
    }
    @Override public Optional<EraDefinition> getEra(String id) { return Optional.ofNullable(eras.get(normalize(id))); }
    @Override public Collection<EraDefinition> getEras() { return eras.values().stream().sorted(Comparator.comparingInt(EraDefinition::order)).toList(); }
    @Override public Optional<String> getPlayerEra(UUID id) { return dataStore.getPlayerEra(Objects.requireNonNull(id)); }
    @Override public synchronized void setPlayerEra(UUID player, String era) {
        String id = normalize(era);
        if (!eras.containsKey(id)) throw new IllegalArgumentException("Unknown era: " + era);
        UUID playerId = Objects.requireNonNull(player);
        String previousEraId = dataStore.getPlayerEra(playerId).orElse(null);
        dataStore.setPlayerEra(playerId, id);
        if (!Objects.equals(previousEraId, id) && Bukkit.getServer() != null) {
            Bukkit.getPluginManager().callEvent(new MechaniumPlayerEraChangeEvent(playerId, previousEraId, id));
        }
    }
    @Override public long getResearchPoints(UUID id) { return dataStore.getResearchPoints(Objects.requireNonNull(id)); }
    @Override public synchronized void addResearchPoints(UUID id, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Research point grants must be non-negative");
        UUID playerId = Objects.requireNonNull(id);
        long previousPoints = dataStore.getResearchPoints(playerId);
        dataStore.addResearchPoints(playerId, amount);
        long newPoints = dataStore.getResearchPoints(playerId);
        if (previousPoints != newPoints && Bukkit.getServer() != null) {
            Bukkit.getPluginManager().callEvent(new MechaniumResearchPointsChangeEvent(playerId, previousPoints, newPoints));
        }
    }
    @Override public synchronized boolean registerTechnology(TechnologyDefinition value) {
        Objects.requireNonNull(value);
        if (!eras.containsKey(value.requiredEraId())) throw new IllegalArgumentException("Technology references unregistered era: " + value.requiredEraId());
        for (String prerequisite : value.prerequisites()) if (!technologies.containsKey(prerequisite))
            throw new IllegalArgumentException("Unknown technology prerequisite: " + prerequisite);
        TechnologyDefinition old = technologies.putIfAbsent(value.id(), value);
        return old == null || old.equals(value);
    }
    @Override public Optional<TechnologyDefinition> getTechnology(String id) { return Optional.ofNullable(technologies.get(normalize(id))); }
    @Override public Collection<TechnologyDefinition> getTechnologies() { return technologies.values().stream().sorted(Comparator.comparing(TechnologyDefinition::id)).toList(); }

    @Override public synchronized UnlockResult unlockTechnology(UUID playerId, String technologyId) {
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
        long previousPoints = dataStore.getResearchPoints(playerId);
        if (previousPoints < technology.researchCost()) return UnlockResult.of(UnlockResult.Status.INSUFFICIENT_RESEARCH_POINTS, "Pontos científicos insuficientes. Custo: " + technology.researchCost() + ".");
        if (!dataStore.spendAndUnlock(playerId, technology.id(), technology.researchCost())) {
            if (dataStore.hasUnlocked(playerId, technology.id())) return UnlockResult.of(UnlockResult.Status.ALREADY_UNLOCKED, "Tecnologia já desbloqueada.");
            return UnlockResult.of(UnlockResult.Status.CONCURRENT_UPDATE, "O estado científico mudou simultaneamente. Tente novamente.");
        }
        long remainingPoints = dataStore.getResearchPoints(playerId);
        if (Bukkit.getServer() != null) {
            Bukkit.getPluginManager().callEvent(new MechaniumTechnologyUnlockedEvent(playerId, technology, remainingPoints));
            if (previousPoints != remainingPoints) {
                Bukkit.getPluginManager().callEvent(new MechaniumResearchPointsChangeEvent(
                        playerId, previousPoints, remainingPoints));
            }
        }
        return UnlockResult.of(UnlockResult.Status.UNLOCKED, "Tecnologia desbloqueada: " + technology.displayName());
    }
    @Override public boolean hasUnlockedTechnology(UUID player, String id) {
        Objects.requireNonNull(player, "player");
        TechnologyDefinition technology = technologies.get(normalize(id));
        return technology != null && dataStore.hasUnlocked(player, technology.id());
    }

    @Override public boolean canUseTechnology(UUID player, String id) {
        TechnologyDefinition technology = technologies.get(normalize(id));
        if (technology == null || !hasUnlockedTechnology(player, technology.id())) return false;
        Optional<String> eraId = dataStore.getPlayerEra(player);
        if (eraId.isEmpty()) return false;
        EraDefinition current = eras.get(eraId.get()), required = eras.get(technology.requiredEraId());
        return current != null && required != null && current.order() >= required.order();
    }
    @Override public void registerModule(String id) { modules.add(normalize(id)); }
    @Override public Set<String> getRegisteredModules() { return Collections.unmodifiableSet(new TreeSet<>(modules)); }

    @Override public synchronized boolean registerCategory(ContentCategory value) {
        Objects.requireNonNull(value);
        ContentCategory old = categories.get(value.id());
        if (old != null) {
            if (!old.equals(value)) return false;
            trackContent(MechaniumContentStore.CATEGORY, value.id());
            return true;
        }
        persist(() -> contentStore.saveCategory(value));
        categories.put(value.id(), value);
        trackContent(MechaniumContentStore.CATEGORY, value.id());
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumCategoryRegisterEvent(value)));
        return true;
    }
    @Override public Optional<ContentCategory> getCategory(String id) { return Optional.ofNullable(categories.get(normalize(id))); }
    @Override public Collection<ContentCategory> getCategories() { return categories.values().stream().sorted(Comparator.comparing(ContentCategory::id)).toList(); }

    @Override public synchronized boolean registerItem(ItemDefinition value) {
        Objects.requireNonNull(value);
        if (!categories.containsKey(value.categoryId())) throw new IllegalArgumentException("Unknown item category: " + value.categoryId());
        validateTechnology(value.requiredTechnologyId());
        ItemDefinition old = items.get(value.id());
        if (old != null) {
            if (!old.equals(value)) return false;
            trackContent(MechaniumContentStore.ITEM, value.id());
            return true;
        }
        persist(() -> contentStore.saveItem(value));
        items.put(value.id(), value);
        trackContent(MechaniumContentStore.ITEM, value.id());
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumItemRegisterEvent(value)));
        return true;
    }
    @Override public Optional<ItemDefinition> getItem(String id) { return Optional.ofNullable(items.get(normalize(id))); }
    @Override public Collection<ItemDefinition> getItems() { return items.values().stream().sorted(Comparator.comparing(ItemDefinition::id)).toList(); }
    @Override public Optional<ItemStack> createItemStack(String id, int amount) {
        ItemDefinition definition = items.get(normalize(id));
        if (definition == null || amount < 1 || amount > definition.maxStackSize()) return Optional.empty();
        Material material = Material.valueOf(definition.material());
        if (material.isAir() || (Bukkit.getServer() != null && !material.isItem())) return Optional.empty();
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey("mechanium", "item_id"), PersistentDataType.STRING, definition.id());
        meta.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(definition.displayName()));
        if (!definition.lore().isEmpty()) meta.lore(definition.lore().stream()
                .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line)).toList());
        if (definition.customModelData() > 0) {
            CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of((float) definition.customModelData()));
            meta.setCustomModelDataComponent(modelData);
        }
        meta.setMaxStackSize(definition.maxStackSize());
        stack.setItemMeta(meta);
        return Optional.of(stack);
    }
    @Override public boolean canUseItem(UUID player, String id) {
        ItemDefinition item = items.get(normalize(id));
        return item != null && (item.requiredTechnologyId() == null || canUseTechnology(player, item.requiredTechnologyId()));
    }

    @Override public synchronized boolean registerRecipe(RecipeDefinition value) {
        Objects.requireNonNull(value);
        validateRecipeDefinition(value);
        RecipeDefinition old = recipes.get(value.id());
        if (old != null) {
            if (!old.equals(value)) return false;
            trackContent(MechaniumContentStore.RECIPE, value.id());
            return true;
        }
        persist(() -> contentStore.saveRecipe(value));
        recipes.put(value.id(), value);
        trackContent(MechaniumContentStore.RECIPE, value.id());
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumRecipeRegisterEvent(value)));
        return true;
    }

    /**
     * Replaces a module/addon recipe for the current runtime. The caller owns persistence of this
     * administrative override so module registration can continue to reconcile its canonical data.
     */
    @Override public synchronized boolean overrideRecipe(RecipeDefinition value) {
        Objects.requireNonNull(value);
        if (!recipes.containsKey(value.id())) return false;
        validateRecipeDefinition(value);
        recipes.put(value.id(), value);
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumRecipeRegisterEvent(value)));
        return true;
    }

    private void validateRecipeDefinition(RecipeDefinition value) {
        ItemDefinition resultItem = items.get(value.resultItemId());
        if (resultItem == null) {
            throw new IllegalArgumentException("Recipe result item is not registered: " + value.resultItemId());
        }
        if (value.resultAmount() > resultItem.maxStackSize()) {
            throw new IllegalArgumentException("Recipe result amount exceeds max stack size for " + resultItem.id());
        }
        if (value.requiredTechnologyId() != null) validateTechnology(value.requiredTechnologyId());
        for (String ingredient : value.ingredients().values()) {
            if (ingredient.startsWith("mechanium:") && !items.containsKey(ingredient.substring(10))) {
                throw new IllegalArgumentException("Recipe references unregistered custom ingredient: " + ingredient);
            }
            if (!ingredient.startsWith("mechanium:")) {
                Material material = Material.matchMaterial(ingredient);
                if (material == null || (Bukkit.getServer() != null && !material.isItem())) {
                    throw new IllegalArgumentException("Recipe ingredient is not a valid item material: " + ingredient);
                }
            }
        }
    }
    @Override public Optional<RecipeDefinition> getRecipe(String id) { return Optional.ofNullable(recipes.get(normalize(id))); }
    @Override public Collection<RecipeDefinition> getRecipes() { return recipes.values().stream().sorted(Comparator.comparing(RecipeDefinition::id)).toList(); }

    @Override public synchronized boolean registerMobDrop(MobDropDefinition value) {
        Objects.requireNonNull(value, "drop");
        ItemDefinition resultItem = items.get(value.resultItemId());
        if (resultItem == null) throw new IllegalArgumentException(
                "Mob drop references unregistered result item: " + value.resultItemId());
        if (value.maxAmount() > resultItem.maxStackSize()) throw new IllegalArgumentException(
                "Mob drop maximum amount exceeds item max stack size for " + resultItem.id());
        if (value.requiredTechnologyId() != null) validateTechnology(value.requiredTechnologyId());
        MobDropDefinition old = mobDrops.get(value.id());
        if (old != null) {
            if (!old.equals(value)) return false;
            trackContent(MechaniumContentStore.MOB_DROP, value.id());
            return true;
        }
        persist(() -> contentStore.saveMobDrop(value));
        mobDrops.put(value.id(), value);
        trackContent(MechaniumContentStore.MOB_DROP, value.id());
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumMobDropRegisterEvent(value)));
        return true;
    }

    @Override public Optional<MobDropDefinition> getMobDrop(String id) {
        return Optional.ofNullable(mobDrops.get(normalize(id)));
    }

    @Override public Collection<MobDropDefinition> getMobDrops() {
        return mobDrops.values().stream().sorted(Comparator.comparing(MobDropDefinition::id)).toList();
    }

    @Override public synchronized boolean registerMachine(MachineDefinition value) {
        Objects.requireNonNull(value, "machine");
        if (!SUPPORTED_MACHINE_BLOCKS.contains(value.blockMaterial())) {
            throw new IllegalArgumentException("Unsupported machine block material: " + value.blockMaterial()
                    + ". The current runtime supports DISPENSER only.");
        }
        ItemDefinition machineItem = items.get(value.machineItemId());
        if (machineItem == null) throw new IllegalArgumentException(
                "Machine references unregistered machine item: " + value.machineItemId());
        if (!machineItem.material().equals(value.blockMaterial()) || machineItem.maxStackSize() != 1) {
            throw new IllegalArgumentException("Machine item must use block material " + value.blockMaterial()
                    + " and maxStackSize 1: " + machineItem.id());
        }
        ItemDefinition resultItem = items.get(value.outputItemId());
        if (resultItem == null) throw new IllegalArgumentException(
                "Machine references unregistered output item: " + value.outputItemId());
        if (value.outputAmount() > resultItem.maxStackSize()) {
            throw new IllegalArgumentException("Machine output exceeds max stack size for " + resultItem.id());
        }
        if (value.inputId().startsWith("mechanium:")) {
            ItemDefinition inputItem = items.get(value.inputId().substring(10));
            if (inputItem == null) throw new IllegalArgumentException(
                    "Machine references unregistered input item: " + value.inputId());
            if (value.inputAmount() > inputItem.maxStackSize()) {
                throw new IllegalArgumentException("Machine input amount exceeds max stack size for " + inputItem.id());
            }
        }
        validateTechnology(value.requiredTechnologyId());
        MachineDefinition old = machines.get(value.id());
        if (old != null) {
            if (!old.equals(value)) return false;
            trackContent(MechaniumContentStore.MACHINE, value.id());
            return true;
        }
        persist(() -> contentStore.saveMachine(value));
        machines.put(value.id(), value);
        trackContent(MechaniumContentStore.MACHINE, value.id());
        publishContentEvent(() -> Bukkit.getPluginManager().callEvent(new MechaniumMachineRegisterEvent(value)));
        return true;
    }

    @Override public Optional<MachineDefinition> getMachine(String id) {
        return Optional.ofNullable(machines.get(normalize(id)));
    }

    @Override public Collection<MachineDefinition> getMachines() {
        return machines.values().stream().sorted(Comparator.comparing(MachineDefinition::id)).toList();
    }
    @Override public boolean canCraft(UUID player, String id) {
        RecipeDefinition recipe = recipes.get(normalize(id));
        if (recipe == null) return false;
        ItemDefinition result = items.get(recipe.resultItemId());
        if (result == null || !canUseItem(player, result.id())) return false;
        if (recipe.requiredTechnologyId() != null && !canUseTechnology(player, recipe.requiredTechnologyId())) return false;
        return recipe.ingredients().values().stream()
                .filter(ingredient -> ingredient.startsWith("mechanium:"))
                .map(ingredient -> ingredient.substring(10))
                .allMatch(itemId -> canUseItem(player, itemId));
    }
    @Override public List<String> validateContent() {
        List<String> errors = new ArrayList<>();
        for (ItemDefinition item : items.values()) {
            if (!categories.containsKey(item.categoryId())) errors.add("Item " + item.id() + " references missing category " + item.categoryId());
            if (item.requiredTechnologyId() != null && !technologies.containsKey(item.requiredTechnologyId()))
                errors.add("Item " + item.id() + " references missing technology " + item.requiredTechnologyId());
        }
        for (MachineDefinition machine : machines.values()) {
            if (!SUPPORTED_MACHINE_BLOCKS.contains(machine.blockMaterial())) {
                errors.add("Machine " + machine.id() + " uses unsupported runtime block " + machine.blockMaterial());
            }
            ItemDefinition machineItem = items.get(machine.machineItemId());
            if (machineItem == null) errors.add("Machine " + machine.id() + " references missing machine item " + machine.machineItemId());
            else if (!machineItem.material().equals(machine.blockMaterial()) || machineItem.maxStackSize() != 1)
                errors.add("Machine " + machine.id() + " has an invalid machine item definition");
            ItemDefinition output = items.get(machine.outputItemId());
            if (output == null) errors.add("Machine " + machine.id() + " references missing output item " + machine.outputItemId());
            else if (machine.outputAmount() > output.maxStackSize()) errors.add("Machine " + machine.id() + " output exceeds item max stack size");
            if (machine.inputId().startsWith("mechanium:")) {
                ItemDefinition input = items.get(machine.inputId().substring(10));
                if (input == null) errors.add("Machine " + machine.id() + " references missing custom input " + machine.inputId());
                else if (machine.inputAmount() > input.maxStackSize()) errors.add("Machine " + machine.id() + " input exceeds item max stack size");
            }
            if (!technologies.containsKey(machine.requiredTechnologyId()))
                errors.add("Machine " + machine.id() + " references missing technology " + machine.requiredTechnologyId());
        }
        for (MobDropDefinition drop : mobDrops.values()) {
            ItemDefinition result = items.get(drop.resultItemId());
            if (result == null) errors.add("Mob drop " + drop.id() + " references missing result item " + drop.resultItemId());
            else if (drop.maxAmount() > result.maxStackSize()) errors.add("Mob drop " + drop.id() + " exceeds item max stack size");
            if (drop.requiredTechnologyId() != null && !technologies.containsKey(drop.requiredTechnologyId()))
                errors.add("Mob drop " + drop.id() + " references missing technology " + drop.requiredTechnologyId());
        }
        for (RecipeDefinition recipe : recipes.values()) {
            if (!items.containsKey(recipe.resultItemId())) errors.add("Recipe " + recipe.id() + " has missing result item " + recipe.resultItemId());
            if (recipe.requiredTechnologyId() != null && !technologies.containsKey(recipe.requiredTechnologyId()))
                errors.add("Recipe " + recipe.id() + " references missing technology " + recipe.requiredTechnologyId());
            ItemDefinition result = items.get(recipe.resultItemId());
            if (result != null && recipe.resultAmount() > result.maxStackSize())
                errors.add("Recipe " + recipe.id() + " result amount exceeds item max stack size (" + result.maxStackSize() + ")");
            for (String ingredient : recipe.ingredients().values()) {
                if (ingredient.startsWith("mechanium:") && !items.containsKey(ingredient.substring(10)))
                    errors.add("Recipe " + recipe.id() + " references missing custom ingredient " + ingredient);
            }
        }
        return List.copyOf(errors);
    }


    private final class ContentScope implements MechaniumContentRegistrationScope {
        private final String ownerId;
        private final Map<String, Set<String>> activeContent = new HashMap<>();
        private final List<Runnable> pendingEvents = new ArrayList<>();
        private final Map<String, EraDefinition> eraSnapshot = new HashMap<>(eras);
        private final Map<String, TechnologyDefinition> technologySnapshot = new HashMap<>(technologies);
        private final Map<String, ContentCategory> categorySnapshot = new HashMap<>(categories);
        private final Map<String, ItemDefinition> itemSnapshot = new HashMap<>(items);
        private final Map<String, RecipeDefinition> recipeSnapshot = new HashMap<>(recipes);
        private final Map<String, MobDropDefinition> mobDropSnapshot = new HashMap<>(mobDrops);
        private final Map<String, MachineDefinition> machineSnapshot = new HashMap<>(machines);
        private final Set<String> moduleSnapshot = new HashSet<>(modules);
        private boolean transactionOpen;
        private boolean completed;

        private ContentScope(String ownerId) {
            this.ownerId = ownerId;
            if (contentStore != null) {
                contentStore.beginContentTransaction();
                transactionOpen = true;
            }
        }

        private void track(String type, String id) {
            if (completed) throw new IllegalStateException("Content registration scope is already closed.");
            activeContent.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(id);
        }

        @Override
        public void commit() {
            synchronized (DefaultMechaniumApi.this) {
                ensureActive();
                Map<String, Set<String>> removed = Map.of();
                if (contentStore != null) {
                    Map<String, Set<String>> snapshot = new HashMap<>();
                    activeContent.forEach((type, ids) -> snapshot.put(type, Set.copyOf(ids)));
                    removed = contentStore.reconcileContentOwner(ownerId, Map.copyOf(snapshot));
                    contentStore.commitContentTransaction();
                    transactionOpen = false;
                }
                removeContent(removed);
                completed = true;
                activeContentScope = null;
                // Publish registration events only after the database and in-memory registry commit.
                for (Runnable event : pendingEvents) {
                    try {
                        event.run();
                    } catch (RuntimeException exception) {
                        Bukkit.getLogger().warning("Falha ao publicar evento de conteúdo MECHANIUM: "
                                + exception.getMessage());
                    }
                }
                pendingEvents.clear();
            }
        }

        @Override
        public void close() {
            synchronized (DefaultMechaniumApi.this) {
                if (completed) return;
                RuntimeException rollbackFailure = null;
                if (contentStore != null && transactionOpen) {
                    try { contentStore.rollbackContentTransaction(); }
                    catch (RuntimeException exception) { rollbackFailure = exception; }
                }
                restore(eras, eraSnapshot);
                restore(technologies, technologySnapshot);
                restore(categories, categorySnapshot);
                restore(items, itemSnapshot);
                restore(recipes, recipeSnapshot);
                restore(mobDrops, mobDropSnapshot);
                restore(machines, machineSnapshot);
                modules.clear();
                modules.addAll(moduleSnapshot);
                completed = true;
                activeContentScope = null;
                if (rollbackFailure != null) throw rollbackFailure;
            }
        }

        private void ensureActive() {
            if (completed || activeContentScope != this) {
                throw new IllegalStateException("Content registration scope is not active.");
            }
        }
    }

    private static <T> void restore(Map<String, T> target, Map<String, T> snapshot) {
        target.clear();
        target.putAll(snapshot);
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

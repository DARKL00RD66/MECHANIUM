package dev.mechanium.storage;

import dev.mechanium.api.MechaniumDataStore;
import dev.mechanium.api.MechaniumContentStore;
import dev.mechanium.api.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite persistence using prepared statements, WAL and an atomic research debit.
 * A single connection is synchronized; larger deployments can replace this adapter.
 */
public final class SqliteDataStore implements MechaniumDataStore, MechaniumContentStore {
    private final Connection connection;
    private final Logger logger;

    public SqliteDataStore(Path databaseFile, Logger logger) throws SQLException {
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception exception) {
            throw new SQLException("Could not create MECHANIUM data directory", exception);
        }
        this.logger = logger;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        configure();
        migrate();
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_players (
                    uuid TEXT PRIMARY KEY,
                    era_id TEXT,
                    research_points INTEGER NOT NULL DEFAULT 0 CHECK (research_points >= 0)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_unlocked_technologies (
                    player_uuid TEXT NOT NULL,
                    technology_id TEXT NOT NULL,
                    unlocked_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, technology_id),
                    FOREIGN KEY (player_uuid) REFERENCES mechanium_players(uuid) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (1)");
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_categories(id TEXT PRIMARY KEY,display_name TEXT NOT NULL,description TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_items(id TEXT PRIMARY KEY,display_name TEXT NOT NULL,material TEXT NOT NULL,lore TEXT NOT NULL,custom_model_data INTEGER NOT NULL,max_stack_size INTEGER NOT NULL,category_id TEXT NOT NULL,required_technology_id TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_recipes(id TEXT PRIMARY KEY,type TEXT NOT NULL,result_item_id TEXT NOT NULL,result_amount INTEGER NOT NULL,shape TEXT NOT NULL,ingredients TEXT NOT NULL,required_technology_id TEXT)");
            statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (2)");
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_mob_drops(id TEXT PRIMARY KEY,entity_type TEXT NOT NULL,result_item_id TEXT NOT NULL,min_amount INTEGER NOT NULL,max_amount INTEGER NOT NULL,chance REAL NOT NULL,required_technology_id TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_machines(id TEXT PRIMARY KEY,display_name TEXT NOT NULL,machine_item_id TEXT NOT NULL,block_material TEXT NOT NULL,input_id TEXT NOT NULL,input_amount INTEGER NOT NULL,output_item_id TEXT NOT NULL,output_amount INTEGER NOT NULL,processing_ticks INTEGER NOT NULL,required_technology_id TEXT NOT NULL)");
            statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (3)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_content_owners (
                    content_type TEXT NOT NULL CHECK (content_type IN ('category','item','recipe','mob_drop','machine')),
                    content_id TEXT NOT NULL,
                    owner_id TEXT NOT NULL,
                    PRIMARY KEY (content_type, content_id)
                )
                """);
            int currentVersion = 0;
            try (ResultSet versionRows = statement.executeQuery(
                    "SELECT COALESCE(MAX(version), 0) FROM mechanium_schema_version")) {
                if (versionRows.next()) currentVersion = versionRows.getInt(1);
            }
            if (currentVersion < 4) {
                seedLegacyContentOwnership();
                statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (4)");
            }
            statement.execute("CREATE TABLE IF NOT EXISTS mechanium_server_settings (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            if (currentVersion < 5) {
                boolean hasOverrideFlag = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(mechanium_players)")) {
                    while (columns.next()) {
                        if ("era_override_enabled".equalsIgnoreCase(columns.getString("name"))) {
                            hasOverrideFlag = true;
                            break;
                        }
                    }
                }
                if (!hasOverrideFlag) {
                    statement.execute("ALTER TABLE mechanium_players ADD COLUMN era_override_enabled INTEGER NOT NULL DEFAULT 0");
                }
                statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (5)");
            }
            if (currentVersion < 6) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mechanium_active_researches (
                        player_uuid TEXT NOT NULL,
                        technology_id TEXT NOT NULL,
                        started_at_ms INTEGER NOT NULL,
                        completes_at_ms INTEGER NOT NULL CHECK (completes_at_ms > started_at_ms),
                        PRIMARY KEY (player_uuid, technology_id),
                        FOREIGN KEY (player_uuid) REFERENCES mechanium_players(uuid) ON DELETE CASCADE
                    )
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_mechanium_active_researches_due ON mechanium_active_researches(player_uuid, completes_at_ms)");
                statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (6)");
            }
            if (currentVersion < 7) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mechanium_machine_inputs (
                        machine_id TEXT NOT NULL,
                        ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
                        item_id TEXT NOT NULL,
                        amount INTEGER NOT NULL CHECK (amount BETWEEN 1 AND 99),
                        PRIMARY KEY (machine_id, ordinal),
                        UNIQUE (machine_id, item_id),
                        FOREIGN KEY (machine_id) REFERENCES mechanium_machines(id) ON DELETE CASCADE
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mechanium_machine_outputs (
                        machine_id TEXT NOT NULL,
                        ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
                        item_id TEXT NOT NULL,
                        amount INTEGER NOT NULL CHECK (amount BETWEEN 1 AND 99),
                        PRIMARY KEY (machine_id, ordinal),
                        UNIQUE (machine_id, item_id),
                        FOREIGN KEY (machine_id) REFERENCES mechanium_machines(id) ON DELETE CASCADE
                    )
                    """);
                statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (7)");
            }
        }
        logger.info("MECHANIUM SQLite schema até a versão 7 pronta.");
    }


    /** Adopts content created by the original built-in modules before ownership tracking existed. */
    private void seedLegacyContentOwnership() throws SQLException {
        String[][] legacy = {
                {"category", "industrial_components", "module:industry", "mechanium_categories"},
                {"category", "agricultural_goods", "module:agriculture", "mechanium_categories"},
                {"category", "geology", "module:geology", "mechanium_categories"},
                {"category", "private_storage", "module:storage", "mechanium_categories"},
                {"item", "mechanical_parts", "module:industry", "mechanium_items"},
                {"item", "copper_coil", "module:industry", "mechanium_items"},
                {"item", "mechanical_press", "module:industry", "mechanium_items"},
                {"item", "animal_fat", "module:agriculture", "mechanium_items"},
                {"item", "compost", "module:agriculture", "mechanium_items"},
                {"item", "industrial_fertilizer", "module:agriculture", "mechanium_items"},
                {"item", "tallow_torch", "module:agriculture", "mechanium_items"},
                {"item", "geological_survey_kit", "module:geology", "mechanium_items"},
                {"item", "private_storage_crate", "module:storage", "mechanium_items"},
                {"recipe", "mechanical_parts_recipe", "module:industry", "mechanium_recipes"},
                {"recipe", "copper_coil_recipe", "module:industry", "mechanium_recipes"},
                {"recipe", "mechanical_press_recipe", "module:industry", "mechanium_recipes"},
                {"recipe", "compost_recipe", "module:agriculture", "mechanium_recipes"},
                {"recipe", "industrial_fertilizer_recipe", "module:agriculture", "mechanium_recipes"},
                {"recipe", "tallow_torch_recipe", "module:agriculture", "mechanium_recipes"},
                {"recipe", "geological_survey_kit_recipe", "module:geology", "mechanium_recipes"},
                {"recipe", "private_storage_crate_recipe", "module:storage", "mechanium_recipes"},
                {"mob_drop", "cow_animal_fat", "module:agriculture", "mechanium_mob_drops"},
                {"mob_drop", "pig_animal_fat", "module:agriculture", "mechanium_mob_drops"},
                {"mob_drop", "sheep_animal_fat", "module:agriculture", "mechanium_mob_drops"},
                {"machine", "mechanical_press", "module:industry", "mechanium_machines"}
        };
        for (String[] row : legacy) {
            // The table name is selected exclusively from the hard-coded migration map above.
            String sql = "INSERT OR IGNORE INTO mechanium_content_owners(content_type,content_id,owner_id) "
                    + "SELECT ?, ?, ? WHERE EXISTS (SELECT 1 FROM " + row[3] + " WHERE id = ?)";
            try (PreparedStatement insert = connection.prepareStatement(sql)) {
                insert.setString(1, row[0]);
                insert.setString(2, row[1]);
                insert.setString(3, row[2]);
                insert.setString(4, row[1]);
                insert.executeUpdate();
            }
        }
    }

    private void ensurePlayer(UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO mechanium_players(uuid) VALUES (?)")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized Optional<String> getPlayerEra(UUID playerId) {
        return getPlayerEraOverride(playerId);
    }

    @Override
    public synchronized Optional<String> getPlayerEraOverride(UUID playerId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT era_id FROM mechanium_players WHERE uuid = ? AND era_override_enabled = 1")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw failure("read player era override", exception);
        }
    }

    @Override
    public synchronized Optional<String> getServerEra() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM mechanium_server_settings WHERE setting_key = 'server-era'");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
        } catch (SQLException exception) {
            throw failure("read server era", exception);
        }
    }

    @Override
    public synchronized void setServerEra(String eraId) {
        Objects.requireNonNull(eraId, "eraId");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO mechanium_server_settings(setting_key, setting_value)
                VALUES ('server-era', ?)
                ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                """)) {
            statement.setString(1, eraId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("set server era", exception);
        }
    }

    @Override
    public synchronized void clearPlayerEraOverride(UUID playerId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET era_override_enabled = 0 WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("clear player era override", exception);
        }
    }

    @Override
    public synchronized void setPlayerEra(UUID playerId, String eraId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET era_id = ?, era_override_enabled = 1 WHERE uuid = ?")) {
                statement.setString(1, eraId);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("set player era", exception);
        }
    }

    @Override
    public synchronized long getResearchPoints(UUID playerId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT research_points FROM mechanium_players WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getLong(1) : 0L;
                }
            }
        } catch (SQLException exception) {
            throw failure("read research points", exception);
        }
    }

    @Override
    public synchronized void addResearchPoints(UUID playerId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Research point grants must be non-negative");
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET research_points = research_points + ? WHERE uuid = ? AND research_points <= ?")) {
                statement.setLong(1, amount);
                statement.setString(2, playerId.toString());
                statement.setLong(3, Long.MAX_VALUE - amount);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Research point balance would exceed the maximum supported value");
                }
            }
        } catch (SQLException exception) {
            throw failure("add research points", exception);
        }
    }

    @Override
    public synchronized boolean hasUnlocked(UUID playerId, String technologyId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM mechanium_unlocked_technologies WHERE player_uuid = ? AND technology_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, technologyId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        } catch (SQLException exception) {
            throw failure("read unlocked technology", exception);
        }
    }


    @Override
    public synchronized Collection<ResearchProgress> getResearches(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        List<ResearchProgress> researches = new ArrayList<>();
        try {
            ensurePlayer(playerId);
            try (PreparedStatement query = connection.prepareStatement("""
                    SELECT technology_id, started_at_ms, completes_at_ms
                    FROM mechanium_active_researches
                    WHERE player_uuid = ?
                    ORDER BY completes_at_ms, technology_id
                    """)) {
                query.setString(1, playerId.toString());
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) {
                        researches.add(new ResearchProgress(rows.getString(1), rows.getLong(2), rows.getLong(3)));
                    }
                }
            }
            return List.copyOf(researches);
        } catch (SQLException exception) {
            throw failure("read active research jobs", exception);
        }
    }

    @Override
    public synchronized boolean beginResearch(UUID playerId, String technologyId, long cost,
                                               long startedAtEpochMillis, long completesAtEpochMillis) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(technologyId, "technologyId");
        if (cost < 0L) throw new IllegalArgumentException("Research cost cannot be negative.");
        if (startedAtEpochMillis < 0L || completesAtEpochMillis <= startedAtEpochMillis) {
            throw new IllegalArgumentException("Research timestamps are invalid.");
        }

        try {
            ensurePlayer(playerId);
            connection.setAutoCommit(false);
            try {
                if (hasUnlocked(playerId, technologyId)) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement existing = connection.prepareStatement("""
                        SELECT 1 FROM mechanium_active_researches
                        WHERE player_uuid = ? AND technology_id = ?
                        """)) {
                    existing.setString(1, playerId.toString());
                    existing.setString(2, technologyId);
                    try (ResultSet rows = existing.executeQuery()) {
                        if (rows.next()) {
                            connection.commit();
                            return false;
                        }
                    }
                }

                try (PreparedStatement debit = connection.prepareStatement("""
                        UPDATE mechanium_players
                        SET research_points = research_points - ?
                        WHERE uuid = ? AND research_points >= ?
                        """)) {
                    debit.setLong(1, cost);
                    debit.setString(2, playerId.toString());
                    debit.setLong(3, cost);
                    if (debit.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }

                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO mechanium_active_researches(
                            player_uuid, technology_id, started_at_ms, completes_at_ms
                        ) VALUES (?, ?, ?, ?)
                        """)) {
                    insert.setString(1, playerId.toString());
                    insert.setString(2, technologyId);
                    insert.setLong(3, startedAtEpochMillis);
                    insert.setLong(4, completesAtEpochMillis);
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("start timed research", exception);
        }
    }

    @Override
    public synchronized Collection<String> completeDueResearches(UUID playerId, long nowEpochMillis) {
        Objects.requireNonNull(playerId, "playerId");
        List<String> completed = new ArrayList<>();
        try {
            ensurePlayer(playerId);
            connection.setAutoCommit(false);
            try {
                List<String> due = new ArrayList<>();
                try (PreparedStatement query = connection.prepareStatement("""
                        SELECT technology_id
                        FROM mechanium_active_researches
                        WHERE player_uuid = ? AND completes_at_ms <= ?
                        ORDER BY completes_at_ms, technology_id
                        """)) {
                    query.setString(1, playerId.toString());
                    query.setLong(2, nowEpochMillis);
                    try (ResultSet rows = query.executeQuery()) {
                        while (rows.next()) due.add(rows.getString(1));
                    }
                }

                try (PreparedStatement unlock = connection.prepareStatement("""
                        INSERT OR IGNORE INTO mechanium_unlocked_technologies(player_uuid, technology_id)
                        VALUES (?, ?)
                        """)) {
                    for (String technologyId : due) {
                        unlock.setString(1, playerId.toString());
                        unlock.setString(2, technologyId);
                        if (unlock.executeUpdate() == 1) completed.add(technologyId);
                    }
                }

                try (PreparedStatement delete = connection.prepareStatement("""
                        DELETE FROM mechanium_active_researches
                        WHERE player_uuid = ? AND completes_at_ms <= ?
                        """)) {
                    delete.setString(1, playerId.toString());
                    delete.setLong(2, nowEpochMillis);
                    delete.executeUpdate();
                }
                connection.commit();
                return List.copyOf(completed);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("complete timed research", exception);
        }
    }

    @Override
    public synchronized boolean spendAndUnlock(UUID playerId, String technologyId, long cost) {
        if (cost < 0) throw new IllegalArgumentException("Technology cost must be non-negative");
        try {
            ensurePlayer(playerId);
            connection.setAutoCommit(false);
            try {
                if (hasUnlocked(playerId, technologyId)) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement debit = connection.prepareStatement("""
                    UPDATE mechanium_players
                    SET research_points = research_points - ?
                    WHERE uuid = ? AND research_points >= ?
                    """)) {
                    debit.setLong(1, cost);
                    debit.setString(2, playerId.toString());
                    debit.setLong(3, cost);
                    if (debit.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO mechanium_unlocked_technologies(player_uuid, technology_id)
                    VALUES (?, ?)
                    """)) {
                    insert.setString(1, playerId.toString());
                    insert.setString(2, technologyId);
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("spend research points and unlock technology", exception);
        }
    }


    private static String enc(String value) { return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }
    private static String encList(Collection<String> values) { return values.stream().map(SqliteDataStore::enc).collect(java.util.stream.Collectors.joining(".")); }
    private static List<String> decList(String value) { return value == null || value.isEmpty() ? List.of() : Arrays.stream(value.split("\\.", -1)).map(SqliteDataStore::dec).toList(); }
    private static String encIngredients(Map<Character,String> values) { return values.entrySet().stream().map(e -> enc(e.getKey().toString()) + ":" + enc(e.getValue())).collect(java.util.stream.Collectors.joining(".")); }
    private static Map<Character,String> decIngredients(String value) {
        Map<Character,String> out = new TreeMap<>();
        if (value != null && !value.isEmpty()) for (String pair : value.split("\\.")) { String[] p=pair.split(":",2); out.put(dec(p[0]).charAt(0),dec(p[1])); }
        return out;
    }
    @Override public synchronized void saveCategory(ContentCategory v) {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO mechanium_categories VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET display_name=excluded.display_name,description=excluded.description")){p.setString(1,v.id());p.setString(2,v.displayName());p.setString(3,v.description());p.executeUpdate();}
        catch(SQLException e){throw failure("save category",e);}
    }
    @Override public synchronized Collection<ContentCategory> loadCategories() {
        List<ContentCategory> out=new ArrayList<>();
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT id,display_name,description FROM mechanium_categories ORDER BY id")){while(r.next())out.add(new ContentCategory(r.getString(1),r.getString(2),r.getString(3)));return List.copyOf(out);}
        catch(SQLException e){throw failure("load categories",e);}
    }
    @Override public synchronized void saveItem(ItemDefinition v) {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO mechanium_items VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET display_name=excluded.display_name,material=excluded.material,lore=excluded.lore,custom_model_data=excluded.custom_model_data,max_stack_size=excluded.max_stack_size,category_id=excluded.category_id,required_technology_id=excluded.required_technology_id")){
            p.setString(1,v.id());p.setString(2,v.displayName());p.setString(3,v.material());p.setString(4,encList(v.lore()));p.setInt(5,v.customModelData());p.setInt(6,v.maxStackSize());p.setString(7,v.categoryId());p.setString(8,v.requiredTechnologyId());p.executeUpdate();
        }catch(SQLException e){throw failure("save item",e);}
    }
    @Override public synchronized Collection<ItemDefinition> loadItems() {
        List<ItemDefinition> out=new ArrayList<>();
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT id,display_name,material,lore,custom_model_data,max_stack_size,category_id,required_technology_id FROM mechanium_items ORDER BY id")){
            while(r.next())out.add(new ItemDefinition(r.getString(1),r.getString(2),r.getString(3),decList(r.getString(4)),r.getInt(5),r.getInt(6),r.getString(7),r.getString(8)));return List.copyOf(out);
        }catch(SQLException e){throw failure("load items",e);}
    }
    @Override public synchronized void saveRecipe(RecipeDefinition v) {
        try(PreparedStatement p=connection.prepareStatement("INSERT INTO mechanium_recipes VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET type=excluded.type,result_item_id=excluded.result_item_id,result_amount=excluded.result_amount,shape=excluded.shape,ingredients=excluded.ingredients,required_technology_id=excluded.required_technology_id")){
            p.setString(1,v.id());p.setString(2,v.type().name());p.setString(3,v.resultItemId());p.setInt(4,v.resultAmount());p.setString(5,encList(v.shape()));p.setString(6,encIngredients(v.ingredients()));p.setString(7,v.requiredTechnologyId());p.executeUpdate();
        }catch(SQLException e){throw failure("save recipe",e);}
    }
    @Override public synchronized Collection<RecipeDefinition> loadRecipes() {
        List<RecipeDefinition> out=new ArrayList<>();
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT id,type,result_item_id,result_amount,shape,ingredients,required_technology_id FROM mechanium_recipes ORDER BY id")){
            while(r.next())out.add(new RecipeDefinition(r.getString(1),RecipeDefinition.Type.valueOf(r.getString(2)),r.getString(3),r.getInt(4),decList(r.getString(5)),decIngredients(r.getString(6)),r.getString(7)));return List.copyOf(out);
        }catch(SQLException e){throw failure("load recipes",e);}
    }

    @Override public synchronized void saveMobDrop(MobDropDefinition v) {
        try (PreparedStatement p = connection.prepareStatement("""
                INSERT INTO mechanium_mob_drops VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET entity_type=excluded.entity_type,
                result_item_id=excluded.result_item_id,min_amount=excluded.min_amount,
                max_amount=excluded.max_amount,chance=excluded.chance,
                required_technology_id=excluded.required_technology_id
                """)) {
            p.setString(1, v.id());
            p.setString(2, v.entityType());
            p.setString(3, v.resultItemId());
            p.setInt(4, v.minAmount());
            p.setInt(5, v.maxAmount());
            p.setDouble(6, v.chance());
            p.setString(7, v.requiredTechnologyId());
            p.executeUpdate();
        } catch (SQLException e) {
            throw failure("save mob drop", e);
        }
    }

    @Override public synchronized Collection<MobDropDefinition> loadMobDrops() {
        List<MobDropDefinition> out = new ArrayList<>();
        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery("SELECT id,entity_type,result_item_id,min_amount,max_amount,chance,required_technology_id FROM mechanium_mob_drops ORDER BY id")) {
            while (r.next()) {
                out.add(new MobDropDefinition(r.getString(1), r.getString(2), r.getString(3),
                        r.getInt(4), r.getInt(5), r.getDouble(6), r.getString(7)));
            }
            return List.copyOf(out);
        } catch (SQLException e) {
            throw failure("load mob drops", e);
        }
    }

    @Override public synchronized void saveMachine(MachineDefinition v) {
        try (PreparedStatement p = connection.prepareStatement("""
                INSERT INTO mechanium_machines VALUES(?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET display_name=excluded.display_name,
                machine_item_id=excluded.machine_item_id,block_material=excluded.block_material,
                input_id=excluded.input_id,input_amount=excluded.input_amount,
                output_item_id=excluded.output_item_id,output_amount=excluded.output_amount,
                processing_ticks=excluded.processing_ticks,
                required_technology_id=excluded.required_technology_id
                """)) {
            // Keep the legacy columns populated so older readers can still inspect
            // the primary input and output while new readers use the normalized tables.
            p.setString(1, v.id());
            p.setString(2, v.displayName());
            p.setString(3, v.machineItemId());
            p.setString(4, v.blockMaterial());
            p.setString(5, v.inputId());
            p.setInt(6, v.inputAmount());
            p.setString(7, v.outputItemId());
            p.setInt(8, v.outputAmount());
            p.setInt(9, v.processingTicks());
            p.setString(10, v.requiredTechnologyId());
            p.executeUpdate();

            try (PreparedStatement deleteInputs = connection.prepareStatement(
                    "DELETE FROM mechanium_machine_inputs WHERE machine_id=?");
                 PreparedStatement deleteOutputs = connection.prepareStatement(
                    "DELETE FROM mechanium_machine_outputs WHERE machine_id=?")) {
                deleteInputs.setString(1, v.id());
                deleteInputs.executeUpdate();
                deleteOutputs.setString(1, v.id());
                deleteOutputs.executeUpdate();
            }
            try (PreparedStatement inputInsert = connection.prepareStatement(
                    "INSERT INTO mechanium_machine_inputs(machine_id,ordinal,item_id,amount) VALUES(?,?,?,?)");
                 PreparedStatement outputInsert = connection.prepareStatement(
                    "INSERT INTO mechanium_machine_outputs(machine_id,ordinal,item_id,amount) VALUES(?,?,?,?)")) {
                for (int i = 0; i < v.inputs().size(); i++) {
                    MachineIngredient input = v.inputs().get(i);
                    inputInsert.setString(1, v.id());
                    inputInsert.setInt(2, i);
                    inputInsert.setString(3, input.itemId());
                    inputInsert.setInt(4, input.amount());
                    inputInsert.addBatch();
                }
                for (int i = 0; i < v.outputs().size(); i++) {
                    MachineProduct output = v.outputs().get(i);
                    outputInsert.setString(1, v.id());
                    outputInsert.setInt(2, i);
                    outputInsert.setString(3, output.itemId());
                    outputInsert.setInt(4, output.amount());
                    outputInsert.addBatch();
                }
                inputInsert.executeBatch();
                outputInsert.executeBatch();
            }
        } catch (SQLException e) {
            throw failure("save machine", e);
        }
    }

    @Override public synchronized Collection<MachineDefinition> loadMachines() {
        List<MachineDefinition> legacyRows = new ArrayList<>();
        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery("SELECT id,display_name,machine_item_id,block_material,input_id,input_amount,output_item_id,output_amount,processing_ticks,required_technology_id FROM mechanium_machines ORDER BY id")) {
            while (r.next()) {
                legacyRows.add(new MachineDefinition(r.getString(1), r.getString(2), r.getString(3),
                        r.getString(4), r.getString(5), r.getInt(6), r.getString(7),
                        r.getInt(8), r.getInt(9), r.getString(10)));
            }
        } catch (SQLException e) {
            throw failure("load machines", e);
        }

        List<MachineDefinition> out = new ArrayList<>(legacyRows.size());
        for (MachineDefinition legacy : legacyRows) {
            List<MachineIngredient> inputs = loadMachineInputs(legacy.id());
            List<MachineProduct> outputs = loadMachineOutputs(legacy.id());
            out.add(new MachineDefinition(legacy.id(), legacy.displayName(), legacy.machineItemId(),
                    legacy.blockMaterial(),
                    inputs.isEmpty() ? legacy.inputs() : inputs,
                    outputs.isEmpty() ? legacy.outputs() : outputs,
                    legacy.processingTicks(), legacy.requiredTechnologyId()));
        }
        return List.copyOf(out);
    }

    private List<MachineIngredient> loadMachineInputs(String machineId) {
        List<MachineIngredient> inputs = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(
                "SELECT item_id,amount FROM mechanium_machine_inputs WHERE machine_id=? ORDER BY ordinal")) {
            p.setString(1, machineId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) inputs.add(new MachineIngredient(r.getString(1), r.getInt(2)));
            }
            return List.copyOf(inputs);
        } catch (SQLException e) {
            throw failure("load machine inputs", e);
        }
    }

    private List<MachineProduct> loadMachineOutputs(String machineId) {
        List<MachineProduct> outputs = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement(
                "SELECT item_id,amount FROM mechanium_machine_outputs WHERE machine_id=? ORDER BY ordinal")) {
            p.setString(1, machineId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) outputs.add(new MachineProduct(r.getString(1), r.getInt(2)));
            }
            return List.copyOf(outputs);
        } catch (SQLException e) {
            throw failure("load machine outputs", e);
        }
    }


    @Override public synchronized void beginContentTransaction() {
        try {
            if (!connection.getAutoCommit()) throw new IllegalStateException("A MECHANIUM content transaction is already active.");
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            throw failure("begin content transaction", exception);
        }
    }

    @Override public synchronized void migrateContentOwner(String previousOwnerId, String newOwnerId) {
        if (connectionAutoCommit()) throw new IllegalStateException("No active MECHANIUM content transaction.");
        if (previousOwnerId == null || previousOwnerId.isBlank()
                || newOwnerId == null || newOwnerId.isBlank()) {
            throw new IllegalArgumentException("Content owner IDs cannot be blank.");
        }
        if (previousOwnerId.equals(newOwnerId)) return;
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE mechanium_content_owners SET owner_id=? WHERE owner_id=?")) {
            update.setString(1, newOwnerId);
            update.setString(2, previousOwnerId);
            update.executeUpdate();
        } catch (SQLException exception) {
            throw failure("migrate content owner", exception);
        }
    }

    @Override public synchronized Map<String, Set<String>> reconcileContentOwner(
            String ownerId, Map<String, Set<String>> activeContent) {
        if (connectionAutoCommit()) throw new IllegalStateException("No active MECHANIUM content transaction.");
        if (ownerId == null || ownerId.isBlank()) throw new IllegalArgumentException("Content owner cannot be blank.");
        Map<String, Set<String>> current = activeContent == null ? Map.of() : activeContent;
        Map<String, Set<String>> stale = new LinkedHashMap<>();
        String[][] orderedTypes = {
                {RECIPE, "mechanium_recipes"},
                {MACHINE, "mechanium_machines"},
                {MOB_DROP, "mechanium_mob_drops"},
                {ITEM, "mechanium_items"},
                {CATEGORY, "mechanium_categories"}
        };
        try {
            List<String[]> previous = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT content_type,content_id FROM mechanium_content_owners WHERE owner_id=?")) {
                query.setString(1, ownerId);
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) previous.add(new String[]{rows.getString(1), rows.getString(2)});
                }
            }
            for (String[] row : previous) {
                String type = row[0], id = row[1];
                if (current.getOrDefault(type, Set.of()).contains(id)) continue;
                String table = tableForContentType(type);
                try (PreparedStatement deleteContent = connection.prepareStatement("DELETE FROM " + table + " WHERE id=?");
                     PreparedStatement deleteOwner = connection.prepareStatement(
                             "DELETE FROM mechanium_content_owners WHERE content_type=? AND content_id=? AND owner_id=?")) {
                    deleteContent.setString(1, id);
                    deleteContent.executeUpdate();
                    deleteOwner.setString(1, type);
                    deleteOwner.setString(2, id);
                    deleteOwner.setString(3, ownerId);
                    deleteOwner.executeUpdate();
                }
                stale.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(id);
            }

            for (String[] typeRow : orderedTypes) {
                String type = typeRow[0];
                for (String id : current.getOrDefault(type, Set.of())) {
                    if (id == null || id.isBlank()) throw new IllegalArgumentException("Content ID cannot be blank.");
                    String existingOwner = null;
                    try (PreparedStatement query = connection.prepareStatement(
                            "SELECT owner_id FROM mechanium_content_owners WHERE content_type=? AND content_id=?")) {
                        query.setString(1, type);
                        query.setString(2, id);
                        try (ResultSet row = query.executeQuery()) {
                            if (row.next()) existingOwner = row.getString(1);
                        }
                    }
                    if (existingOwner != null && !existingOwner.equals(ownerId)) {
                        throw new IllegalStateException("Content " + type + ":" + id
                                + " already belongs to " + existingOwner + " and cannot be claimed by " + ownerId);
                    }
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT OR IGNORE INTO mechanium_content_owners(content_type,content_id,owner_id) VALUES(?,?,?)")) {
                        insert.setString(1, type);
                        insert.setString(2, id);
                        insert.setString(3, ownerId);
                        insert.executeUpdate();
                    }
                }
            }
            Map<String, Set<String>> result = new LinkedHashMap<>();
            stale.forEach((type, ids) -> result.put(type, Set.copyOf(ids)));
            return Map.copyOf(result);
        } catch (SQLException exception) {
            throw failure("reconcile content owner " + ownerId, exception);
        }
    }

    @Override public synchronized Map<String, Set<String>> pruneInactiveContentOwners(Set<String> activeOwnerIds) {
        if (connectionAutoCommit()) throw new IllegalStateException("No active MECHANIUM content transaction.");
        Set<String> active = activeOwnerIds == null ? Set.of() : Set.copyOf(activeOwnerIds);
        Set<String> owners = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT DISTINCT owner_id FROM mechanium_content_owners")) {
            while (rows.next()) owners.add(rows.getString(1));
        } catch (SQLException exception) {
            throw failure("list content owners", exception);
        }
        Map<String, Set<String>> removed = new LinkedHashMap<>();
        for (String owner : owners) {
            if (active.contains(owner)) continue;
            Map<String, Set<String>> stale = reconcileContentOwner(owner, Map.of());
            stale.forEach((type, ids) -> removed.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).addAll(ids));
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        removed.forEach((type, ids) -> result.put(type, Set.copyOf(ids)));
        return Map.copyOf(result);
    }

    @Override public synchronized void commitContentTransaction() {
        try {
            if (connection.getAutoCommit()) throw new IllegalStateException("No active MECHANIUM content transaction.");
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            try { connection.setAutoCommit(true); } catch (SQLException reset) { exception.addSuppressed(reset); }
            throw failure("commit content transaction", exception);
        }
    }

    @Override public synchronized void rollbackContentTransaction() {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw failure("rollback content transaction", exception);
        }
    }

    private boolean connectionAutoCommit() {
        try { return connection.getAutoCommit(); }
        catch (SQLException exception) { throw failure("inspect transaction state", exception); }
    }

    private String tableForContentType(String contentType) {
        return switch (contentType) {
            case CATEGORY -> "mechanium_categories";
            case ITEM -> "mechanium_items";
            case RECIPE -> "mechanium_recipes";
            case MOB_DROP -> "mechanium_mob_drops";
            case MACHINE -> "mechanium_machines";
            default -> throw new IllegalArgumentException("Unknown content type: " + contentType);
        };
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + " in MECHANIUM database", exception);
    }

    @Override
    public synchronized void close() {
        try {
            if (!connection.isClosed()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                }
                connection.close();
            }
        } catch (SQLException exception) {
            logger.warning("Could not close MECHANIUM SQLite cleanly: " + exception.getMessage());
        }
    }
}

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
        }
        logger.info("MECHANIUM SQLite schema v1 pronta.");
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
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT era_id FROM mechanium_players WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw failure("read player era", exception);
        }
    }

    @Override
    public synchronized void setPlayerEra(UUID playerId, String eraId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET era_id = ? WHERE uuid = ?")) {
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

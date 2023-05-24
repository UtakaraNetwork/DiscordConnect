package work.novablog.mcplugin.discordconnect.account.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.account.AccountManager;
import work.novablog.mcplugin.discordconnect.account.LinkedAccount;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SQLiteAccountManager extends AccountManager {

    private final DatabaseConfig config;
    private final File dataDir;
    private @Nullable HikariDataSource hikari;

    public SQLiteAccountManager(File dataDir, DatabaseConfig config) {
        this.dataDir = dataDir;
        this.config = config;
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    @Override
    public void connect() throws IOException {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + new File(dataDir, this.config.getFile()));
        config.setConnectionInitSql("SELECT 1");
        config.setAutoCommit(true);
        this.config.getProperties().forEach(config::addDataSourceProperty);
        hikari = new HikariDataSource(config);
        initDatabase();
    }

    @Override
    public void close() {
        if (hikari != null) {
            hikari.close();
        }
        hikari = null;
    }

    public void initDatabase() throws IOException {
        if (hikari == null)
            throw new IOException("database closed");

        try (Connection conn = hikari.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS linked (mc_uuid VARCHAR(36) UNIQUE, discord_id BIGINT, link_time BIGINT)";
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }


    @Override
    public CompletableFuture<@NotNull Boolean> isLinkedDiscord(@NotNull UUID minecraftId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT 1 FROM `linked` WHERE `mc_uuid` = ? LIMIT 1";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, minecraftId.toString().toLowerCase(Locale.ROOT));
                ResultSet rs = stmt.executeQuery();
                return rs.next();

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<@Nullable Long> getLinkedDiscordId(@NotNull UUID minecraftId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT `discord_id` FROM `linked` WHERE `mc_uuid` = ? LIMIT 1";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, minecraftId.toString().toLowerCase(Locale.ROOT));
                ResultSet rs = stmt.executeQuery();

                if (rs.next())
                    return rs.getLong("discord_id");
                return null;

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> linkDiscordId(@NotNull UUID minecraftId, long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            try (Connection conn = hikari.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM `linked` WHERE `discord_id` = ?")) {
                    stmt.setLong(1, discordId);
                    stmt.execute();
                }

                try (PreparedStatement stmt = conn.prepareStatement("REPLACE INTO `linked` VALUES (?, ?, ?)")) {
                    stmt.setString(1, minecraftId.toString().toLowerCase(Locale.ROOT));
                    stmt.setLong(2, discordId);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.executeUpdate();
                }

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<@NotNull Boolean> isLinkedMinecraft(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT 1 FROM `linked` WHERE `discord_id` = ? LIMIT 1";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, discordId);
                ResultSet rs = stmt.executeQuery();
                return rs.next();

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<@Nullable UUID> getLinkedMinecraftId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT `mc_uuid` FROM `linked` WHERE `discord_id` = ? LIMIT 1";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, discordId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next())
                    return UUID.fromString(rs.getString("mc_uuid"));
                return null;

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unlinkByMinecraftId(@NotNull UUID minecraftId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "DELETE FROM `linked` WHERE `mc_uuid` = ?";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, minecraftId.toString().toLowerCase(Locale.ROOT));
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> unlinkByDiscordId(long discordId) {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "DELETE FROM `linked` WHERE `discord_id` = ?";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, discordId);
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<LinkedAccount[]> getLinkedAccountAll() {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT `mc_uuid`, `discord_id` FROM `linked`";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<LinkedAccount> data = Lists.newArrayList();
                while (rs.next())
                    data.add(new LinkedAccount(UUID.fromString(rs.getString(1)), rs.getLong(2)));

                return data.toArray(new LinkedAccount[0]);

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getLinkedAccountCount() {
        return CompletableFuture.supplyAsync(() -> {
            if (hikari == null)
                throw new CompletionException(new IOException("database closed"));

            String sql = "SELECT COUNT(*) AS `total` FROM `linked`";
            try (Connection conn = hikari.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next())
                    return rs.getInt("total");
                return 0;

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }


    public static class DatabaseConfig extends work.novablog.mcplugin.discordconnect.account.db.DatabaseConfig {

        public DatabaseConfig(ConfigurationSection config) {
            super(config);
        }

        @Override
        public String getType() {
            return "sqlite";
        }

        public String getFile() {
            return config.getString("file", "./accounts.yml");
        }

        public Map<String, Object> getProperties() {
            ConfigurationSection props = config.getConfigurationSection("properties");
            if (props == null)
                return Collections.emptyMap();

            Map<String, Object> values = Maps.newHashMap();
            for (String key : props.getKeys(false)) {
                values.put(key, props.get(key));
            }

            return values;
        }

    }

}

package work.novablog.mcplugin.discordconnect.account;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.account.db.DatabaseConfig;
import work.novablog.mcplugin.discordconnect.account.db.MySQLAccountManager;
import work.novablog.mcplugin.discordconnect.account.db.SQLiteAccountManager;
import work.novablog.mcplugin.discordconnect.account.db.YamlAccountManager;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AccountManager {
    private final Map<String, UUID> linkingCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();


    public abstract void connect() throws IOException;

    public abstract void close() throws IOException;


    public final String generateCode(UUID playerUuid, String playerName) {
        names.put(playerUuid, playerName);
        String codeString;
        do {
            int code = ThreadLocalRandom.current().nextInt(10000);
            codeString = String.format("%04d", code);

        } while (linkingCodes.putIfAbsent(codeString, playerUuid) != null);
        return codeString;
    }

    public @Nullable String getLinkingPlayerName(UUID playerId) {
        return names.get(playerId);
    }

    public final @Nullable UUID removeMinecraftIdByLinkCode(@NotNull String code) {
        return linkingCodes.remove(code);
    }

    public final Map<String, UUID> linkingCodes() {
        return linkingCodes;
    }


    public abstract CompletableFuture<@NotNull Boolean> isLinkedDiscord(@NotNull UUID minecraftId);

    public abstract CompletableFuture<@Nullable Long> getLinkedDiscordId(@NotNull UUID minecraftId);

    public abstract CompletableFuture<Void> linkDiscordId(@NotNull UUID minecraftId, long discordId);

    public abstract CompletableFuture<@NotNull Boolean> isLinkedMinecraft(long discordId);

    public abstract CompletableFuture<@Nullable UUID> getLinkedMinecraftId(long discordId);

    public abstract CompletableFuture<Void> unlinkByMinecraftId(@NotNull UUID minecraftId);

    public abstract CompletableFuture<Void> unlinkByDiscordId(long discordId);


    public abstract CompletableFuture<LinkedAccount[]> getLinkedAccountAll();

    public abstract CompletableFuture<Integer> getLinkedAccountCount();


    public static DatabaseConfig createDatabaseConfig(String dbType, ConfigurationSection config) {
        switch (dbType.toLowerCase(Locale.ROOT)) {
            case "yaml":
                return new YamlAccountManager.DatabaseConfig(config);
            case "sqlite":
                return new SQLiteAccountManager.DatabaseConfig(config);
            case "mysql":
                return new MySQLAccountManager.DatabaseConfig(config);
        }
        throw new IllegalArgumentException("Unknown database type: " + dbType);
    }

    public static AccountManager createManager(DatabaseConfig config, Plugin plugin) {
        if (config instanceof YamlAccountManager.DatabaseConfig) {
            return new YamlAccountManager(plugin.getDataFolder(), ((YamlAccountManager.DatabaseConfig) config));
        } else if (config instanceof SQLiteAccountManager.DatabaseConfig) {
            return new SQLiteAccountManager(plugin.getDataFolder(), ((SQLiteAccountManager.DatabaseConfig) config));
        } else if (config instanceof MySQLAccountManager.DatabaseConfig) {
            return new MySQLAccountManager(((MySQLAccountManager.DatabaseConfig) config));
        }
        throw new IllegalArgumentException("Unknown database config: " + config.getClass().getSimpleName());
    }

}

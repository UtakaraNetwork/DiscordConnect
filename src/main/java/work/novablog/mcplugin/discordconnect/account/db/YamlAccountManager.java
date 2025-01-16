package work.novablog.mcplugin.discordconnect.account.db;

import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.account.AccountManager;
import work.novablog.mcplugin.discordconnect.account.LinkedAccount;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class YamlAccountManager extends AccountManager {
    private final Map<UUID, Long> discordAccounts = Maps.newConcurrentMap();
    private final File dataFilePath;
    private final DatabaseConfig config;

    public YamlAccountManager(File dataDir, DatabaseConfig config) {
        this.config = config;
        this.dataFilePath = new File(dataDir, config.getFile());
    }


    public DatabaseConfig getConfig() {
        return config;
    }

    public void loadFile() {
        discordAccounts.clear();
        if (dataFilePath.isFile()) {
            ConfigurationSection ids = YamlConfiguration.loadConfiguration(dataFilePath).getConfigurationSection("ids");
            if (ids != null) {
                for (String key : ids.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(key);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    long discordId = ids.getLong(key);
                    discordAccounts.put(uuid, discordId);
                }
            }
        }
    }

    public void saveFile() throws IOException {
        YamlConfiguration config;
        if (dataFilePath.isFile()) {
            config = YamlConfiguration.loadConfiguration(dataFilePath);
        } else {
            config = new YamlConfiguration();
        }

        config.set("ids", null);
        discordAccounts.forEach((uuid, accountId) ->
                config.set("ids." + uuid.toString(), accountId));

        config.save(dataFilePath);
    }


    @Override
    public void connect() {
        loadFile();
    }

    @Override
    public void close() throws IOException {
        saveFile();
        discordAccounts.clear();
    }


    @Override
    public CompletableFuture<@NotNull Boolean> isLinkedDiscord(@NotNull UUID minecraftId) {
        return runCurrent(() -> discordAccounts.containsKey(minecraftId));
    }

    @Override
    public CompletableFuture<@Nullable Long> getLinkedDiscordId(@NotNull UUID minecraftId) {
        return runCurrent(() -> discordAccounts.get(minecraftId));
    }

    @Override
    public CompletableFuture<Void> linkDiscordId(@NotNull UUID minecraftId, long discordId) {
        return runCurrent(() -> {
            discordAccounts.put(minecraftId, discordId);
            saveFile();
        });
    }

    @Override
    public CompletableFuture<@NotNull Boolean> isLinkedMinecraft(long discordId) {
        return runCurrent(() -> discordAccounts.containsValue(discordId));
    }

    @Override
    public CompletableFuture<@Nullable UUID> getLinkedMinecraftId(long discordId) {
        return runCurrent(() -> discordAccounts.entrySet().stream()
                .filter(e -> e.getValue() == discordId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null));
    }

    @Override
    public CompletableFuture<Void> unlinkByMinecraftId(@NotNull UUID minecraftId) {
        return runCurrent(() -> {
            if (discordAccounts.remove(minecraftId) != null)
                saveFile();
        });
    }

    @Override
    public CompletableFuture<Void> unlinkByDiscordId(long discordId) {
        return runCurrent(() -> {
            if (discordAccounts.values().removeIf(dId -> dId == discordId))
                saveFile();
        });
    }


    @Override
    public CompletableFuture<LinkedAccount[]> getLinkedAccountAll() {
        return runCurrent(() -> discordAccounts.entrySet().stream()
                .map(e -> new LinkedAccount(e.getKey(), e.getValue()))
                .toArray(LinkedAccount[]::new));
    }

    @Override
    public CompletableFuture<Integer> getLinkedAccountCount() {
        return CompletableFuture.completedFuture(discordAccounts.size());
    }


    private <T> CompletableFuture<T> runCurrent(Supplier<T> runnable) {
        try {
            return CompletableFuture.completedFuture(runnable.get());
        } catch (Throwable e) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    private CompletableFuture<Void> runCurrent(ThrowRunnable runnable) {
        try {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        } catch (Throwable e) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    private interface ThrowRunnable {
        void run() throws Throwable;
    }


    public static class DatabaseConfig extends work.novablog.mcplugin.discordconnect.account.db.DatabaseConfig {
        public DatabaseConfig(ConfigurationSection config) {
            super(config);
        }

        @Override
        public String getType() {
            return "yaml";
        }

        public String getFile() {
            return config.getString("file", "./accounts.yml");
        }

    }

}

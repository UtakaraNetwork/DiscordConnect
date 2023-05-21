package work.novablog.mcplugin.discordconnect.util;

import com.google.common.collect.Maps;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AccountManager {
    private final Map<UUID, Long> discordAccounts = Maps.newHashMap();
    private final Map<String, UUID> linkingCodes = new ConcurrentHashMap<>();
    private final File dataFilePath;

    public AccountManager(File dataFilePath) {
        this.dataFilePath = dataFilePath;
    }


    public String generateCode(UUID playerUuid) {
        String codeString;
        do {
            int code = ThreadLocalRandom.current().nextInt(10000);
            codeString = String.format("%04d", code);

        } while (linkingCodes.putIfAbsent(codeString, playerUuid) != null);
        return codeString;
    }

    public @Nullable UUID removeMinecraftIdByLinkCode(String code) {
        return linkingCodes.remove(code);
    }


    public File getFilePath() {
        return dataFilePath;
    }

    public boolean saveFile() {
        YamlConfiguration config;
        if (dataFilePath.isFile()) {
            config = YamlConfiguration.loadConfiguration(dataFilePath);
        } else {
            config = new YamlConfiguration();
        }

        config.set("ids", null);
        discordAccounts.forEach((uuid, accountId) ->
                config.set("ids." + uuid.toString(), accountId));

        try {
            config.save(dataFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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


    public boolean isLinkedDiscord(UUID minecraftId) {
        return discordAccounts.containsKey(minecraftId);
    }

    public @Nullable Long getLinkedDiscordId(UUID minecraftId) {
        return discordAccounts.get(minecraftId);
    }

    public void setLinkedDiscordId(UUID minecraftId, long discordId) {
        discordAccounts.put(minecraftId, discordId);
    }

    public boolean isLinkedMinecraft(long discordId) {
        return discordAccounts.containsValue(discordId);
    }

    public @Nullable UUID getLinkedMinecraftId(long discordId) {
        return discordAccounts.entrySet().stream()
                .filter(e -> e.getValue() == discordId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void unlinkByMinecraftId(UUID minecraftId) {
        discordAccounts.remove(minecraftId);
    }

    public void unlinkByDiscordId(long discordId) {
        discordAccounts.values().removeIf(dId -> dId == discordId);
    }


    public Map<UUID, Long> discordAccounts() {
        return discordAccounts;
    }

    public Map<UUID, Long> getDiscordAccounts() {
        return Collections.unmodifiableMap(discordAccounts);
    }

}

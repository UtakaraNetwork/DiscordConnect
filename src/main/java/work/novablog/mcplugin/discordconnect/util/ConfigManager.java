package work.novablog.mcplugin.discordconnect.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.account.db.DatabaseConfig;
import work.novablog.mcplugin.discordconnect.account.db.SQLiteAccountManager;
import work.novablog.mcplugin.discordconnect.account.db.YamlAccountManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ConfigManager {
    private static final int CONFIG_LATEST = 3;

    private static YamlConfiguration langData;
    private final DatabaseConfig accountsDatabaseConfig;

    public String botToken;
    public List<String> botWebhookURLs;
    public List<Long> botChatChannelIds;
    public String botPlayingGameName;

    public Boolean isEnableConsoleChannel;
    public Long consoleChannelId;
    public Boolean allowDispatchCommandFromConsoleChannel;

    public String botCommandPrefix;
    public String adminRole;
    public boolean doUpdateCheck;

    public String fromDiscordToMinecraftFormat;
    public String fromMinecraftToDiscordName;
    public String fromDiscordToDiscordName;

    public List<String> hiddenServers;
    public String dummyServerName;
    public String lunaChatJapanizeFormat;

    public String linkedToConsoleCommand;

    /**
     * configの読み出し、保持を行うインスタンスを生成します
     * @param plugin プラグインのメインクラス
     * @throws IOException 読み出し中にエラーが発生した場合にthrowされます
     */
    public ConfigManager(@NotNull Plugin plugin) throws IOException {
        //設定フォルダの作成
        if(!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdir()) {
            throw new IOException();
        }

        //バージョンが古ければ古いファイルをバックアップ
        if (getConfigData(plugin).getInt("configVersion", 0) < CONFIG_LATEST) {
            backupOldFile(plugin, "config.yml");
            backupOldFile(plugin, "message.yml");
        }

        //configとlangの取得
        YamlConfiguration pluginConfig = getConfigData(plugin);
        langData = getLangData(plugin);

        //configの読み出し
        botToken = pluginConfig.getString("token");
        botWebhookURLs = pluginConfig.getStringList("webhookURLs");
        botChatChannelIds = pluginConfig.getLongList("chatChannelIDs");
        botPlayingGameName = pluginConfig.getString("playingGameName");

        isEnableConsoleChannel = pluginConfig.getBoolean("consoleChannel.enable");
        consoleChannelId = pluginConfig.getLong("consoleChannel.channelId");
        allowDispatchCommandFromConsoleChannel = pluginConfig.getBoolean("consoleChannel.allowDispatchCommand");

        botCommandPrefix = pluginConfig.getString("prefix");
        adminRole = pluginConfig.getString("adminRole");
        doUpdateCheck = pluginConfig.getBoolean("updateCheck");

        fromDiscordToMinecraftFormat = pluginConfig.getString("fromDiscordToMinecraftFormat");
        fromMinecraftToDiscordName = pluginConfig.getString("fromMinecraftToDiscordName");
        fromDiscordToDiscordName = pluginConfig.getString("fromDiscordToDiscordName");

        hiddenServers = pluginConfig.getStringList("hiddenServers");
        dummyServerName = pluginConfig.getString("dummyServerName");
        lunaChatJapanizeFormat = pluginConfig.getString("japanizeFormat");

        linkedToConsoleCommand = pluginConfig.getString("linkedToConsoleCommand");

        String dbType = Optional.ofNullable(pluginConfig.getString("accounts.dbType")).orElse("yaml");
        ConfigurationSection dbSection = pluginConfig.getConfigurationSection("accounts.database." + dbType);
        if (dbSection == null)
            dbSection = new YamlConfiguration();

        if (dbType.equalsIgnoreCase("yaml")) {
            accountsDatabaseConfig = new YamlAccountManager.DatabaseConfig(dbSection);
        } else if (dbType.equalsIgnoreCase("sqlite")) {
            accountsDatabaseConfig = new SQLiteAccountManager.DatabaseConfig(dbSection);
        } else {
            throw new IllegalArgumentException("Unknown database type: " + dbType);
        }
    }

    private YamlConfiguration getConfigData(Plugin plugin) throws IOException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            //存在しなければコピー
            InputStream src = plugin.getResource("config.yml");
            Files.copy(src, configFile.toPath());
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    private YamlConfiguration getLangData(Plugin plugin) throws IOException {
        File langFile = new File(plugin.getDataFolder(), "message.yml");
        if (!langFile.exists()) {
            //存在しなければコピー
            InputStream src = plugin.getResource(Locale.getDefault().toString() + ".yml");
            if(src == null) src = plugin.getResource("ja_JP.yml");
            Files.copy(src, langFile.toPath());
        }

        return YamlConfiguration.loadConfiguration(langFile);
    }

    public DatabaseConfig getAccountsDatabaseConfig() {
        return accountsDatabaseConfig;
    }

    private void backupOldFile(Plugin plugin, String targetFileName) throws IOException {
        File oldFile = new File(plugin.getDataFolder(), targetFileName + "_old");
        Files.deleteIfExists(oldFile.toPath());
        if(!(new File(plugin.getDataFolder(), targetFileName).renameTo(oldFile))) throw new IOException();
    }

    /**
     * 多言語対応メッセージ
     */
    public enum Message {
        invalidToken,
        invalidWebhookURL,
        mainChannelNotFound,
        consoleChannelNotFound,
        shutdownDueToError,
        normalShutdown,
        botIsReady,
        botRestarted,
        configReloaded,
        configReloadFailed,
        configPropertyIsNull,
        dispatchedCommand,
        bungeeCommandPlayerOnly,

        updateNotice,
        updateDownloadLink,
        updateCheckFailed,
        pluginIsLatest,

        accountLinkLinkedToDiscord,
        accountLinkLinked,
        accountLinkShowCode,

        bungeeCommandDenied,
        bungeeCommandNotFound,
        bungeeCommandSyntaxError,

        bungeeCommandHelpLine1,
        bungeeCommandHelpHelpcmd,
        bungeeCommandHelpReloadcmd,
        bungeeCommandHelpLinkcmd,

        discordCommandDenied,
        discordCommandNotFound,
        discordCommandSyntaxError,

        discordCommandHelp,

        discordCommandPlayersListDescription,
        discordCommandPlayerList,
        discordCommandPlayerCount,
        discordCommandNoPlayersFound,

        discordCommandReloadDescription,
        discordCommandReload,
        discordCommandReloading,

        userActivity,
        serverActivity,
        command,

        proxyStarted,
        proxyStopped,
        joined,
        left,
        serverSwitched;

        /**
         * yamlファイルからメッセージを取ってきます
         * @return 多言語対応メッセージ
         */
        @Override
        public String toString() {
            return langData.getString(name());
        }
    }
}

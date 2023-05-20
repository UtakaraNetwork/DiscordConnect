package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatBukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import work.novablog.mcplugin.discordconnect.command.BukkitMinecraftCommand;
import work.novablog.mcplugin.discordconnect.listener.BukkitListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class DiscordConnect extends JavaPlugin {
    private static final int CONFIG_LATEST = 4;
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private Properties langData;
    private BukkitListener bukkitListener;

    private LunaChatBukkit lunaChat;
    private LunaChatListener lunaChatListener;

    /**
     * インスタンスを返す
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * Botマネージャーを返す
     * @return botマネージャー
     */
    public BotManager getBotManager() {
        return botManager;
    }

    /**
     * 言語データを返す
     * @return 言語データ
     */
    public Properties getLangData() {
        return langData;
    }

    /**
     * BungeeListenerを返す
     * @return BungeeListener
     */
    public BukkitListener getBungeeListener() {
        return bukkitListener;
    }

    /**
     * LunaChatを返す
     * @return lunaChat
     */
    public LunaChatBukkit getLunaChat() {
        return lunaChat;
    }

    /**
     * LunaChatListenerを返す
     * @return lunaChatListener
     */
    public LunaChatListener getLunaChatListener() {
        return lunaChatListener;
    }

    @Override
    public void onEnable() {
        instance = this;

        //bstats
//        new Metrics(this, 7990);

        //LunaChatと連携
        Plugin temp = getServer().getPluginManager().getPlugin("LunaChat");
        if(temp instanceof LunaChatBukkit) {
            lunaChat = (LunaChatBukkit) temp;
            lunaChatListener = new LunaChatListener();
        }

        //configの読み込み
        loadConfig();

        //コマンドの追加
        Optional.ofNullable(getCommand("discordconnect"))
                .ifPresent(cmd -> cmd.setExecutor(new BukkitMinecraftCommand()));
    }

    public void loadConfig() {
        if(botManager != null) {
            botManager.botShutdown(true);
            botManager = null;
        }

        //設定フォルダ
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        //言語ファイル
        File languageFile = new File(getDataFolder(), "message.yml");
        if (!languageFile.exists()) {
            //存在しなければコピー
            InputStream src = getResource(Locale.getDefault().toString() + ".properties");
            if(src == null) src = getResource("ja_JP.properties");

            try {
                Files.copy(src, languageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Messageの準備
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(inputStreamReader));
            langData = new Properties();
            langData.load(bufferedReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //configファイル
        File pluginConfig = new File(getDataFolder(), "config.yml");
        if (!pluginConfig.exists()) {
            //存在しなければコピー
            InputStream src = getResource("config.yml");

            try {
                Files.copy(src, pluginConfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //config取得・bot起動
        YamlConfiguration pluginConfiguration = YamlConfiguration.loadConfiguration(pluginConfig);

        int configVersion = pluginConfiguration.getInt("configVersion", 0);
        //configが古ければ新しいconfigをコピー
        if(configVersion < CONFIG_LATEST) {
            try {
                //古いconfigをリネーム
                File old_config = new File(getDataFolder(), "config_old.yml");
                Files.deleteIfExists(old_config.toPath());
                pluginConfig.renameTo(old_config);

                //新しいconfigをコピー
                pluginConfig = new File(getDataFolder(), "config.yml");
                InputStream src = getResource("config.yml");
                Files.copy(src, pluginConfig.toPath());
                pluginConfiguration = YamlConfiguration.loadConfiguration(pluginConfig);

                //古いlangファイルをリネーム
                File old_lang = new File(getDataFolder(), "message_old.yml");
                Files.deleteIfExists(old_lang.toPath());
                languageFile.renameTo(old_lang);

                //新しいlangファイルをコピー
                languageFile = new File(getDataFolder(), "message.yml");
                src = getResource(Locale.getDefault().toString() + ".properties");
                if(src == null) src = getResource("ja_JP.properties");
                Files.copy(src, languageFile.toPath());
                InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(inputStreamReader));
                langData = new Properties();
                langData.load(bufferedReader);

                DiscordConnect.getInstance().getLogger().info(Message.configIsOld.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String token = pluginConfiguration.getString("token");
        List<Long> chatChannelIds = pluginConfiguration.getLongList("chatChannelIDs");
        String playingGameName = pluginConfiguration.getString("playingGameName");
        String prefix = pluginConfiguration.getString("prefix");
        String toMinecraftFormat = pluginConfiguration.getString("toMinecraftFormat");
        String toDiscordFormat = pluginConfiguration.getString("toDiscordFormat");
        String japanizeFormat = pluginConfiguration.getString("japanizeFormat");
        bukkitListener = new BukkitListener(toDiscordFormat);
        if(lunaChatListener != null) {
            lunaChatListener.setToDiscordFormat(toDiscordFormat);
            lunaChatListener.setJapanizeFormat(japanizeFormat);
        }
        botManager = new BotManager(token, chatChannelIds, playingGameName, prefix, toMinecraftFormat);

        // アップデートチェック
        boolean updateCheck = pluginConfiguration.getBoolean("updateCheck");
        String currentVer = getDescription().getVersion();
        String latestVer = GithubAPI.getLatestVersionNum();
        if(updateCheck) {
            if (latestVer == null) {
                // チェックに失敗
                getLogger().info(
                        Message.updateCheckFailed.toString()
                );
            } else if (currentVer.equals(latestVer)) {
                // すでに最新
                getLogger().info(
                        Message.pluginIsLatest.toString()
                                .replace("{current}", currentVer)
                );
            }else{
                // 新しいバージョンがある
                getLogger().info(
                        Message.updateNotice.toString()
                                .replace("{current}", currentVer)
                                .replace("{latest}", latestVer)
                );
                getLogger().info(
                        Message.updateDownloadLink.toString()
                                .replace("{link}",pluginDownloadLink)
                );
            }
        }
    }

    @Override
    public void onDisable() {
        botManager.botShutdown(false);
    }
}

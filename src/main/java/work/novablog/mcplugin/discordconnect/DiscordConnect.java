package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.github.ucchyocean.lc3.UUIDCacheData;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.account.AccountManager;
import work.novablog.mcplugin.discordconnect.account.db.DatabaseConfig;
import work.novablog.mcplugin.discordconnect.command.BukkitCommand;
import work.novablog.mcplugin.discordconnect.command.DiscordCommandExecutor;
import work.novablog.mcplugin.discordconnect.command.DiscordStandardCommand;
import work.novablog.mcplugin.discordconnect.listener.BukkitListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public final class DiscordConnect extends JavaPlugin {
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private ArrayList<DiscordWebhookSender> discordWebhookSenders;
    private DiscordCommandExecutor discordCommandExecutor;
    private BukkitListener bukkitListener;

    private LunaChatAPI lunaChatAPI;
    private UUIDCacheData uuidCacheData;
    private LunaChatListener lunaChatListener;

    private @Nullable AccountManager accountManager;

    /**
     * インスタンスを返します
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * Botマネージャーを返します
     * @return botマネージャー
     */
    public @Nullable BotManager getBotManager() {
        return botManager;
    }

    public @Nullable AccountManager getAccountManager() {
        return accountManager;
    }

    /**
     * Webhook送信インスタンスの配列を返します
     * @return webhook送信インスタンス
     */
    public @NotNull
    ArrayList<DiscordWebhookSender> getDiscordWebhookSenders() {
        return discordWebhookSenders;
    }

    /**
     * LunaChatAPIを返します
     * @return lunaChatAPI
     */
    public @Nullable LunaChatAPI getLunaChatAPI() {
        return lunaChatAPI;
    }

    /**
     * botが使用可能か返します
     * @return trueの場合使用可能
     */
    public boolean canBotBeUsed() {
        return botManager != null && botManager.isActive();
    }

    @Override
    public void onEnable() {
        instance = this;

        //bstats
//        new Metrics(this, 7990);

        //LunaChatと連携
        Plugin temp = getServer().getPluginManager().getPlugin("LunaChat");
        if(temp instanceof LunaChatBukkit) {
            uuidCacheData = ((LunaChatBukkit) temp).getUUIDCacheData();
            lunaChatAPI = ((LunaChatBukkit) temp).getLunaChatAPI();
        }

        try {
            init();
        } catch (Throwable e) {
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        //コマンドの追加
        Optional.ofNullable(getCommand("discordconnect"))
                        .ifPresent(cmd -> cmd.setExecutor(new BukkitCommand()));
        discordCommandExecutor = new DiscordCommandExecutor();
        discordCommandExecutor.registerCommand(new DiscordStandardCommand());
    }

    /**
     * プラグインの初期設定をします
     * <p>
     *     複数回呼び出し可能です
     *     複数回呼び出した場合、新しいconfigデータが読み出されます
     * </p>
     */
    public void init() throws IOException {
        if(botManager != null) botManager.botShutdown(true);
        if(discordWebhookSenders != null) discordWebhookSenders.forEach(DiscordWebhookSender::shutdown);
        if(bukkitListener != null) HandlerList.unregisterAll(bukkitListener);
        if(lunaChatListener != null) HandlerList.unregisterAll(lunaChatListener);

        ConfigManager configManager = new ConfigManager(this);

        DatabaseConfig dbConfig = configManager.getAccountsDatabaseConfig();
        accountManager = AccountManager.createManager(dbConfig, this);
        accountManager.connect();

        discordCommandExecutor.setAdminRole(configManager.adminRole);

        try {
            botManager = new BotManager(
                    configManager.botToken,
                    configManager.botChatChannelIds,
                    configManager.botPlayingGameName,
                    configManager.botCommandPrefix,
                    configManager.fromDiscordToMinecraftFormat,
                    configManager.fromDiscordToDiscordName,
                    discordCommandExecutor,
                    configManager.isEnableConsoleChannel,
                    configManager.consoleChannelId,
                    configManager.allowDispatchCommandFromConsoleChannel,
                    configManager.linkedToConsoleCommand
            );
        } catch (LoginException e) {
            getLogger().severe(ConfigManager.Message.invalidToken.toString());
        }

        discordWebhookSenders = new ArrayList<>();
        try {
            configManager.botWebhookURLs.forEach(url ->
                    discordWebhookSenders.add(new DiscordWebhookSender(url))
            );
        } catch(IllegalArgumentException e) {
            getLogger().severe(ConfigManager.Message.invalidWebhookURL.toString());
        }

        //BungeecordイベントのListenerを登録
        bukkitListener = new BukkitListener(configManager.fromMinecraftToDiscordName);
        getServer().getPluginManager().registerEvents(bukkitListener, this);
        if(lunaChatAPI != null) {
            lunaChatListener = new LunaChatListener(
                    configManager.fromMinecraftToDiscordName,
                    configManager.lunaChatJapanizeFormat,
                    uuidCacheData
            );
            getServer().getPluginManager().registerEvents(lunaChatListener, this);
        }

        //アップデートのチェック
        if(configManager.doUpdateCheck) {
            ConfigManager.Message updateStatus;

            String latestVer = GithubAPI.getLatestVersionNum();
            String currentVer = getDescription().getVersion();
            if (latestVer.isEmpty()) {
                updateStatus = ConfigManager.Message.updateCheckFailed;
            } else if (currentVer.equals(latestVer)) {
                updateStatus = ConfigManager.Message.pluginIsLatest;
            }else{
                updateStatus = ConfigManager.Message.updateNotice;
            }

            getLogger().info(
                    updateStatus.toString()
                            .replace("{current}", currentVer)
                            .replace("{latest}", latestVer)
            );
            if(updateStatus == ConfigManager.Message.updateNotice) {
                getLogger().info(
                        ConfigManager.Message.updateDownloadLink.toString()
                                .replace("{link}", pluginDownloadLink)
                );
            }
        }
    }

    @Override
    public void onDisable() {
        if(botManager != null) botManager.botShutdown(false);
        if(discordWebhookSenders != null) discordWebhookSenders.forEach(DiscordWebhookSender::shutdown);

        if (accountManager != null) {
            try {
                accountManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

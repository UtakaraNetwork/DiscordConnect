package work.novablog.mcplugin.discordconnect.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.account.AccountManager;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BukkitListener implements Listener {
    private final ConfigManager config;
    private final String fromMinecraftToDiscordName;

    /**
     * bungeecordのイベントを受け取るインスタンスを生成します
     * @param config プラグイン設定
     */
    public BukkitListener(@NotNull ConfigManager config) {
        this.config = config;
        this.fromMinecraftToDiscordName = config.fromMinecraftToDiscordName;
    }

    /**
     * チャットが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        LunaChatAPI lunaChatAPI = DiscordConnect.getInstance().getLunaChatAPI();
        if (lunaChatAPI == null) {
            // 連携プラグインが無効の場合
            String name = fromMinecraftToDiscordName
                    .replace("{name}", event.getPlayer().getName())
                    .replace("{displayName}", event.getPlayer().getDisplayName())
                    .replace("{server}", "");

            String avatarURL = ConvertUtil.getMinecraftAvatarURL(event.getPlayer().getUniqueId());

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getMessage(), '&');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);

            discordWebhookSenders.forEach(sender -> sender.sendMessage(
                    name,
                    avatarURL,
                    convertedMessage
            ));
        }
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.isAccountLinkRequired() || !AsyncPlayerPreLoginEvent.Result.ALLOWED.equals(event.getLoginResult()))
            return;

        DiscordConnect plugin = DiscordConnect.getInstance();
        UUID playerId = event.getUniqueId();
        String playerName = event.getName();
        AccountManager accountManager = plugin.getAccountManager();

        try {
            Objects.requireNonNull(accountManager, "Account Manager not loaded");
            Boolean linked = accountManager.isLinkedDiscord(playerId).get();

            if (Boolean.TRUE.equals(linked)) {
                return;  // linked
            }

            // create code
            BotManager botManager = Objects.requireNonNull(plugin.getBotManager(), "Bot Manager not loaded");
            String botName = botManager.getBotUser().getName();

            String code = accountManager.linkingCodes().entrySet().stream()
                    .filter(e -> playerId.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .findAny()
                    .orElseGet(() -> accountManager.generateCode(playerId, playerName));

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ConfigManager.Message.accountLinkRequired.toString()
                    .replaceAll("\\{bot}", botName)
                    .replaceAll("\\{code}", code));


        } catch (Throwable e) {
            e.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ConfigManager.Message.accountLinkProcessError.toString());
        }
    }

    /**
     * プレイヤーがログインしたら実行されます
     * @param e ログイン情報
     */
    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        String name = fromMinecraftToDiscordName
                .replace("{name}", e.getPlayer().getName())
                .replace("{displayName}", e.getPlayer().getDisplayName())
                .replace("{server}", "");
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(e.getPlayer().getName(), avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.GREEN.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.joined.toString().replace("{name}", e.getPlayer().getName())
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                webhookEmbedBuilder.build()
        ));

        if(DiscordConnect.getInstance().canBotBeUsed()) {
            assert DiscordConnect.getInstance().getBotManager() != null;
            updatePlayerCount(DiscordConnect.getInstance().getBotManager());
        }
    }

    /**
     * プレイヤーが切断したら実行されます
     * @param e 切断情報
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        String name = fromMinecraftToDiscordName
                .replace("{name}", e.getPlayer().getName())
                .replace("{displayName}", e.getPlayer().getDisplayName())
                .replace("{server}", "");

        String avatarURL = ConvertUtil.getMinecraftAvatarURL(e.getPlayer().getUniqueId());

        WebhookEmbedBuilder webhookEmbedBuilder = new WebhookEmbedBuilder();
        webhookEmbedBuilder.setAuthor(
                new WebhookEmbed.EmbedAuthor(e.getPlayer().getName(), avatarURL, null)
        );
        webhookEmbedBuilder.setColor(Color.RED.getRGB());
        webhookEmbedBuilder.setTitle(
                new WebhookEmbed.EmbedTitle(
                        ConfigManager.Message.userActivity.toString(),
                        null
                )
        );
        webhookEmbedBuilder.setDescription(
                ConfigManager.Message.left.toString().replace("{name}", e.getPlayer().getName())
        );

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                webhookEmbedBuilder.build()
        ));

        if(DiscordConnect.getInstance().canBotBeUsed()) {
            assert DiscordConnect.getInstance().getBotManager() != null;
            updatePlayerCount(DiscordConnect.getInstance().getBotManager());
        }
    }

    /**
     * プレイヤー数情報を更新します
     * @param botManager アクティブなbotマネージャーのインスタンス
     */
    private void updatePlayerCount(@NotNull BotManager botManager) {
        DiscordConnect.getInstance().getServer().getScheduler().runTaskLater(DiscordConnect.getInstance(), () ->
                botManager.updateGameName(
                        DiscordConnect.getInstance().getServer().getOnlinePlayers().size(),
                        DiscordConnect.getInstance().getServer().getMaxPlayers()
        ),20);
    }

}

package work.novablog.mcplugin.discordconnect.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import java.awt.*;
import java.util.ArrayList;

public class BukkitListener implements Listener {
    private final String fromMinecraftToDiscordName;

    /**
     * bungeecordのイベントを受け取るインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     */
    public BukkitListener(@NotNull String fromMinecraftToDiscordName) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
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

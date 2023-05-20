package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.util.ArrayList;

public class BukkitListener implements Listener {
    private static final String AVATAR_IMG_URL = "https://crafatar.com/avatars/{uuid}?size=512&default=MHF_Steve&overlay";
    private final String toDiscordFormat;

    public BukkitListener(String toDiscordFormat) {
        this.toDiscordFormat = toDiscordFormat;
    }

    /**
     * チャットが送信されたら実行
     * @param event チャット情報
     */
    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        LunaChatBukkit lunaChat = DiscordConnect.getInstance().getLunaChat();
        if (lunaChat == null) {
            // 連携プラグインが無効の場合
            Player sender = event.getPlayer();
            String message = event.getMessage();

            MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
            String convertedMessage = MarkdownConverter.toDiscordMessage(components);
            DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                    toDiscordFormat.replace("{server}", "")
                            .replace("{sender}", sender.getName())
                            .replace("{original}", convertedMessage)
            );
        }
    }

    /**
     * ログインされたら
     * @param e ログイン情報
     */
    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                Message.userActivity.toString(),
                null,
                Message.joined.toString().replace("{name}", e.getPlayer().getName()),
                Color.GREEN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace("{uuid}", e.getPlayer().getUniqueId().toString().replace("-", ""))
        );

        updatePlayerCount();
    }

    /**
     * 切断されたら
     * @param e 切断情報
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        DiscordConnect.getInstance().getBotManager().sendMessageToChatChannel(
                Message.userActivity.toString(),
                null,
                Message.left.toString().replace("{name}", e.getPlayer().getName()),
                Color.RED,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace("{uuid}", e.getPlayer().getUniqueId().toString().replace("-", ""))
        );

        updatePlayerCount();
    }

    /**
     * プレイヤー数情報を更新
     */
    private void updatePlayerCount() {
        DiscordConnect.getInstance().getServer().getScheduler().runTaskLater(DiscordConnect.getInstance(), () ->
                DiscordConnect.getInstance().getBotManager().updateGameName(
                        DiscordConnect.getInstance().getServer().getOnlinePlayers().size(),
                        DiscordConnect.getInstance().getServer().getMaxPlayers()
        ),20);
    }
}

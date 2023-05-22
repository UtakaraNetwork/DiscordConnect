package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc3.UUIDCacheData;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitChannelChatEvent;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitPostJapanizeEvent;
import com.github.ucchyocean.lc3.util.Utility;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class LunaChatListener implements Listener {
    private final String fromMinecraftToDiscordName;
    private final String japanizeFormat;
    private final UUIDCacheData uuidCacheData;

    /**
     * LunaChatのイベントをリッスンするインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     * @param japanizeFormat japanizeメッセージをDiscordへ送信するときのフォーマット
     * @param uuidCacheData LunaChatから取得した、UUIDのキャッシュ
     */
    public LunaChatListener(@NotNull String fromMinecraftToDiscordName, @NotNull String japanizeFormat, @NotNull UUIDCacheData uuidCacheData) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
        this.japanizeFormat = japanizeFormat;
        this.uuidCacheData = uuidCacheData;
    }

    /**
     * LunaChatのチャンネルにJapanizeメッセージが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onJapanizeChat(LunaChatBukkitPostJapanizeEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;
        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        MarkComponent[] japanizeComponents = MarkdownConverter.fromMinecraftMessage(event.getJapanized(), '&');
        String japanizeMessage = MarkdownConverter.toDiscordMessage(japanizeComponents);

        MarkComponent[] originalComponents = MarkdownConverter.fromMinecraftMessage(event.getOriginal(), '&');
        String originalMessage = MarkdownConverter.toDiscordMessage(originalComponents);

        String name = fromMinecraftToDiscordName
                .replace("{name}", event.getMember().getName())
                .replace("{displayName}", event.getMember().getDisplayName())
                .replace("{server}", event.getMember().getServerName());
        UUID playerUuid = UUID.fromString(uuidCacheData.getUUIDFromName(event.getMember().getName()));
        String avatarUrl = ConvertUtil.getMinecraftAvatarURL(playerUuid);
        String message = japanizeFormat
                .replace("{japanized}", japanizeMessage)
                .replace("{original}", originalMessage);

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarUrl,
                message
        ));
    }

    /**
     * LunaChatのチャンネルにメッセージが送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onChat(LunaChatBukkitChannelChatEvent event) {
        if(!event.getChannel().isGlobalChannel()) return;

        if (willJapanize(event.getPreReplaceMessage())) return;

        ArrayList<DiscordWebhookSender> discordWebhookSenders = DiscordConnect.getInstance().getDiscordWebhookSenders();

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getNgMaskedMessage(), '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        String name = fromMinecraftToDiscordName
                .replace("{name}", event.getMember().getName())
                .replace("{displayName}", event.getMember().getDisplayName())
                .replace("{server}", event.getMember().getServerName());
        UUID playerUuid = UUID.fromString(uuidCacheData.getUUIDFromName(event.getMember().getName()));
        String avatarUrl = ConvertUtil.getMinecraftAvatarURL(playerUuid);

        discordWebhookSenders.forEach(sender -> sender.sendMessage(
                name,
                avatarUrl,
                convertedMessage
        ));
    }

    /**
     * 与えられた文字列がLunaChatによって日本語化されるかを判定します
     * @param message 文字列
     */
    private boolean willJapanize(String message) {
        final String strippedMessage = Utility.stripColorCode(message);
        return strippedMessage.getBytes(StandardCharsets.UTF_8).length <= strippedMessage.length() &&
                !strippedMessage.matches("[\\uFF61-\\uFF9F]+");
    }

}

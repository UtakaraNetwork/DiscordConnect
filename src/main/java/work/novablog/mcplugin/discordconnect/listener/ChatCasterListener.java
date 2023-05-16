package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.AdminChatEvent;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.events.GlobalChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConvertUtil;
import work.novablog.mcplugin.discordconnect.util.discord.DiscordWebhookSender;

public class ChatCasterListener implements Listener {
    private final String fromMinecraftToDiscordName;
    private final @Nullable String fromMinecraftToDiscordNameAdminChat;

    /**
     * ChatCasterのイベントをリッスンするインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     */
    public ChatCasterListener(@NotNull String fromMinecraftToDiscordName) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
        this.fromMinecraftToDiscordNameAdminChat = null;
    }

    /**
     * ChatCasterのイベントをリッスンするインスタンスを生成します
     * @param fromMinecraftToDiscordName マイクラからDiscordへ転送するときの名前欄のフォーマット
     * @param fromMinecraftToDiscordNameAdminChat マイクラからDiscordへ転送するときの名前欄のフォーマット (管理者チャット)
     */
    public ChatCasterListener(@NotNull String fromMinecraftToDiscordName, @Nullable String fromMinecraftToDiscordNameAdminChat) {
        this.fromMinecraftToDiscordName = fromMinecraftToDiscordName;
        this.fromMinecraftToDiscordNameAdminChat = fromMinecraftToDiscordNameAdminChat;
    }

    /**
     * グローバルチャットに送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onGlobalChat(GlobalChatEvent event) {
        if(!DiscordConnect.getInstance().canBotBeUsed() || event.isCancelled()) return;
        assert DiscordConnect.getInstance().getChatCasterAPI() != null;
        assert DiscordConnect.getInstance().getBotManager() != null;

        String message = DiscordConnect.getInstance().getChatCasterAPI().formatMessageForDiscord(event);
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        String name = fromMinecraftToDiscordName
                .replace("{name}", event.getSender().getName())
                .replace("{displayName}", event.getSender().getDisplayName())
                .replace("{server}", event.getSender().getServer().getInfo().getName());
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(event.getSender().getUniqueId());

        DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender -> sender.sendMessage(
                name,
                avatarURL,
                convertedMessage
        ));
    }

    /**
     * 管理者チャットに送信されたら実行されます
     * @param event チャット情報
     */
    @EventHandler
    public void onAdminChat(AdminChatEvent event) {
        DiscordWebhookSender webhookSender = DiscordConnect.getInstance().getN8CCAdminChatWebhookSender();
        if (webhookSender == null) return;
        if (fromMinecraftToDiscordNameAdminChat == null) return;
        if(!DiscordConnect.getInstance().canBotBeUsed() || event.isCancelled()) return;
        assert DiscordConnect.getInstance().getChatCasterAPI() != null;
        assert DiscordConnect.getInstance().getBotManager() != null;

        String message = event.getMessage();
        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        String name = fromMinecraftToDiscordNameAdminChat
                .replace("{name}", event.getSender().getName())
                .replace("{displayName}", event.getSender().getDisplayName())
                .replace("{server}", event.getSender().getServer().getInfo().getName());
        String avatarURL = ConvertUtil.getMinecraftAvatarURL(event.getSender().getUniqueId());

        webhookSender.sendMessage(name, avatarURL, convertedMessage);
    }

}

package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.command.DiscordCommandExecutor;
import work.novablog.mcplugin.discordconnect.util.AccountManager;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;
import work.novablog.mcplugin.discordconnect.util.discord.BotManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordListener extends ListenerAdapter {
    private final String prefix;
    private final String toMinecraftFormat;
    private final String fromDiscordToDiscordName;
    private final DiscordCommandExecutor discordCommandExecutor;
    private final Long consoleChannelId;
    private final Boolean allowDispatchCommandFromConsoleChannel;

    /**
     * Discordのイベントをリッスンするインスタンスを生成します
     *
     * @param prefix                   コマンドのprefix
     * @param toMinecraftFormat        DiscordのメッセージをBungeecordへ転送するときのフォーマット
     * @param fromDiscordToDiscordName Discordのメッセージを再送するときの名前欄のフォーマット
     * @param discordCommandExecutor   discordのコマンドの解析や実行を行うインスタンス
     * @param consoleChannelId         コンソールチャンネルのID
     * @param allowDispatchCommandFromConsoleChannel コンソールチャンネルからのコマンド実行を許可するか否か
     */
    public DiscordListener(
            @NotNull String prefix,
            @NotNull String toMinecraftFormat,
            @NotNull String fromDiscordToDiscordName,
            @NotNull DiscordCommandExecutor discordCommandExecutor,
            @Nullable Long consoleChannelId,
            @Nullable Boolean allowDispatchCommandFromConsoleChannel
    ) {
        this.prefix = prefix;
        this.toMinecraftFormat = toMinecraftFormat;
        this.fromDiscordToDiscordName = fromDiscordToDiscordName;
        this.discordCommandExecutor = discordCommandExecutor;
        this.consoleChannelId = consoleChannelId;
        this.allowDispatchCommandFromConsoleChannel = allowDispatchCommandFromConsoleChannel;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent receivedMessage) {
        if (receivedMessage.getAuthor().isBot()) return;
        BotManager botManager = DiscordConnect.getInstance().getBotManager();
        assert botManager != null;

        if (ChannelType.PRIVATE.equals(receivedMessage.getChannel().getType())) {
            if (processLinkCodeMessage(receivedMessage, receivedMessage.getAuthor().getIdLong()))
                return;
        }

        if (botManager.getChatChannelSenders().stream()
                .noneMatch(sender -> sender.getChannelID() == receivedMessage.getChannel().getIdLong()) && !(consoleChannelId == receivedMessage.getChannel().getIdLong()))
            return;

        if (receivedMessage.getMessage().getContentRaw().startsWith(prefix)) {
            //コマンド
            String alias = receivedMessage.getMessage().getContentRaw().replace(prefix, "").split("\\s+")[0];
            String[] args = receivedMessage.getMessage().getContentRaw()
                    .replaceAll(Pattern.quote(prefix + alias) + "\\s*", "").split("\\s+");
            if (args[0].equals("")) {
                args = new String[0];
            }

            discordCommandExecutor.parse(receivedMessage, alias, args);
        } else if (consoleChannelId == receivedMessage.getChannel().getIdLong()) {
            if(Boolean.TRUE.equals(allowDispatchCommandFromConsoleChannel)) {
                String commandLine = receivedMessage.getMessage().getContentRaw();

                DiscordConnect.getInstance().getServer().dispatchCommand(
                        DiscordConnect.getInstance().getServer().getConsoleSender(),
                        commandLine
                );

                // 誰が何を実行したか分かりづらいのログを出力する
                DiscordConnect.getInstance().getLogger().info(
                        ConfigManager.Message.dispatchedCommand.toString()
                                .replace("{authorId}", receivedMessage.getAuthor().getId())
                                .replace("{commandLine}", commandLine)
                );
            }
        } else {
            String name = receivedMessage.getAuthor().getName();
            String nickname = Objects.requireNonNull(receivedMessage.getMember()).getNickname() == null ?
                    name : Objects.requireNonNull(receivedMessage.getMember()).getNickname();

            String formattedForMinecraft = toMinecraftFormat
                    .replace("{name}", receivedMessage.getAuthor().getName())
                    .replace("{nickName}", name)
                    .replace("{tag}", receivedMessage.getAuthor().getAsTag())
                    .replace("{server_name}", receivedMessage.getGuild().getName())
                    .replace("{channel_name}", receivedMessage.getChannel().getName());

            //マイクラに送信
            if (!receivedMessage.getMessage().getContentRaw().equals("")) {
                MarkComponent[] components =
                        MarkdownConverter.fromDiscordMessage(receivedMessage.getMessage().getContentRaw());
                List<BaseComponent> convertedMessage = Arrays.asList(MarkdownConverter.toMinecraftMessage(components));

                TextComponent message = new TextComponent(TextComponent.fromLegacyText(formattedForMinecraft));
                List<BaseComponent> extra = message.getExtra();
                extra.addAll(convertedMessage);
                message.setExtra(extra);

                Bukkit.spigot().broadcast(message);
                Bukkit.getConsoleSender().spigot().sendMessage(message);
            }

            receivedMessage.getMessage().getAttachments().forEach((attachment) -> {
                TextComponent url = new TextComponent(TextComponent.fromLegacyText(attachment.getUrl()));
                url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
                BaseComponent[] message = new ComponentBuilder("")
                        .append(formattedForMinecraft)
                        .append(url)
                        .create();

                Bukkit.spigot().broadcast(message);
                Bukkit.getConsoleSender().spigot().sendMessage(message);
            });

            //Discordに再送
            String nameField = fromDiscordToDiscordName
                    .replace("{name}", name)
                    .replace("{nickName}", Objects.requireNonNull(nickname))
                    .replace("{tag}", receivedMessage.getAuthor().getAsTag())
                    .replace("{server_name}", receivedMessage.getGuild().getName())
                    .replace("{channel_name}", receivedMessage.getChannel().getName());
            String message = receivedMessage.getMessage().getContentRaw();
            StringJoiner sj = new StringJoiner("\n");
            receivedMessage.getMessage().getAttachments().forEach(attachment -> sj.add(attachment.getUrl()));
            String finalMessage = message + "\n" + sj;
            if (!finalMessage.equals("\n")) {
                //空白でなければ送信
                DiscordConnect.getInstance().getDiscordWebhookSenders().forEach(sender ->
                        sender.sendMessage(
                                nameField,
                                receivedMessage.getAuthor().getAvatarUrl(),
                                finalMessage
                        )
                );
            }

            //メッセージを削除
            receivedMessage.getMessage().delete().queue();
        }
    }

    private boolean processLinkCodeMessage(MessageReceivedEvent event, long userId) {
        String content = event.getMessage().getContentStripped();
        AccountManager mgr = DiscordConnect.getInstance().getAccountManager();

        Matcher matcher = Pattern.compile("(\\d+)").matcher(content);
        while (matcher.find()) {
            String code = matcher.group(1);
            UUID uuid = mgr.removeMinecraftIdByLinkCode(code);
            if (uuid == null)
                continue;

            mgr.setLinkedDiscordId(uuid, userId);
            mgr.saveFile();

            Optional<Player> player = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getPlayer());
            String mcid = player.map(HumanEntity::getName).orElse("?");

            try {
                player.ifPresent(p -> p.sendMessage(ConfigManager.Message.accountLinkLinked.toString()
                        .replaceAll("\\{user}", event.getAuthor().getName())));
                event.getChannel().sendMessage(ConfigManager.Message.accountLinkLinkedToDiscord.toString()
                                .replaceAll("\\{mcid}", mcid))
                        .queue();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}

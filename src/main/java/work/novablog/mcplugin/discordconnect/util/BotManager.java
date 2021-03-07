package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private JDA bot;
    private List<Long> chatChannelIds;
    private List<DiscordSender> chatChannelSenders;

    private boolean isActive;

    public BotManager(String token, List<Long> chatChannelIds, String playingGameName, String prefix, String toMinecraftFormat) {
        //ログインする
        try {
            bot = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing(playingGameName))
                    .addEventListeners(this)
                    .build();
            bot.addEventListener(new DiscordListener(prefix, toMinecraftFormat));
        } catch (LoginException e) {
            DiscordConnect.getInstance().getLogger().severe(Message.invalidToken.toString());
            bot = null;
            isActive = false;
            return;
        }

        this.chatChannelIds = chatChannelIds;
        isActive = true;
    }

    /**
     * botをシャットダウンする
     */
    public void botShutdown() {
        if(isActive) {
            //メインチャンネルスレッドの停止
            DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(DiscordConnect.getInstance().getBungeeListener());
            DiscordConnect.getInstance().getLogger().info(Message.normalShutdown.toString());
            if(chatChannelSenders != null) {
                chatChannelSenders.forEach(DiscordSender::threadStop);
                chatChannelSenders.forEach(sender -> {
                    try {
                        sender.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                chatChannelSenders = null;
            }

            //botのシャットダウン
            bot.shutdown();
            bot = null;
            chatChannelIds = null;
            isActive = false;
        }
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ReadyEvent) {
            //Botのログインが完了

            //チャットチャンネルを探す
            chatChannelSenders = chatChannelIds.stream()
                    .map(id -> {
                        TextChannel chatChannel = bot.getTextChannelById(id);
                        if(chatChannel == null) {
                            return null;
                        }else{
                            return new DiscordSender(chatChannel);
                        }
                    })
                    .collect(Collectors.toList());
            if(chatChannelSenders.contains(null)) {
                //無効なチャンネルがあれば
                DiscordConnect.getInstance().getLogger().severe(Message.mainChannelNotFound.toString());
                DiscordConnect.getInstance().getLogger().severe(Message.shutdownDueToError.toString());
                chatChannelSenders = null;
                bot.shutdown();
                return;
            }
            chatChannelSenders.forEach(Thread::start);
            DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), DiscordConnect.getInstance().getBungeeListener());

            DiscordConnect.getInstance().getLogger().info(Message.botIsReady.toString());
        }
    }

    /**
     * チャットチャンネルへメッセージを送信
     * @param mes メッセージ
     */
    public void sendMessageToChatChannel(String mes) {
        chatChannelSenders.forEach(sender -> sender.addQueue(mes));
    }

    /**
     * チャットチャンネルへ埋め込みメッセージを送信
     * @param title タイトル
     * @param titleUrl タイトルのリンクURL
     * @param desc 説明
     * @param color 色
     * @param embedFields フィールド
     * @param author 送信者の名前
     * @param authorUrl 送信者のリンクURL
     * @param authorIcon 送信者のアイコン
     * @param footer フッター
     * @param footerIcon フッターのアイコン
     * @param image 画像
     * @param thumbnail サムネイル
     */
    public void sendMessageToChatChannel(String title, String titleUrl, String desc, Color color, List<MessageEmbed.Field> embedFields, String author, String authorUrl, String authorIcon, String footer, String footerIcon, String image, String thumbnail) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(title, titleUrl);
        eb.setColor(color);
        eb.setDescription(desc);
        embedFields.forEach(eb::addField);
        eb.setAuthor(author, authorUrl, authorIcon);
        eb.setFooter(footer, footerIcon);
        eb.setImage(image);
        eb.setThumbnail(thumbnail);

        chatChannelSenders.forEach(sender -> sender.addQueue(eb.build()));
    }

    /**
     * チャットチャンネルのIDリストを取得
     * @return IDリスト
     */
    public List<Long> getChatChannelIds() {
        return chatChannelIds;
    }
}
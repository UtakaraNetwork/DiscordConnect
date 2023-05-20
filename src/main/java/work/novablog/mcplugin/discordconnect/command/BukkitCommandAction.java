package work.novablog.mcplugin.discordconnect.command;


import org.bukkit.command.CommandSender;

public interface BukkitCommandAction {
    /**
     * 実行する処理
     * @param sender コマンドの送信者
     * @param args 引数
     */
    void execute(CommandSender sender, String[] args);
}

package work.novablog.mcplugin.discordconnect.command;


import org.bukkit.command.CommandSender;

/**
 * Minecraftコマンドの原型
 */
public interface BukkitCommandBase {
    /**
     * 実行する処理
     * @param sender 送信者
     * @param args 引数
     */
    void execute(CommandSender sender, String[] args);
}

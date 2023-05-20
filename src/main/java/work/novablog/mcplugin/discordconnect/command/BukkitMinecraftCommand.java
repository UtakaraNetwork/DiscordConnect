package work.novablog.mcplugin.discordconnect.command;

import org.bukkit.command.CommandSender;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;

/**
 * BungeeCordコマンド
 */
public class BukkitMinecraftCommand extends BukkitCommandExecutor {
    public static final String PERM = "discordconnect.command";
    private static final String RELOAD_PERM = "reload";

    /**
     * コンストラクタ
     */
    public BukkitMinecraftCommand() {
        super(PERM);
        addSubCommand(new BungeeSubCommandBuilder("help", this::helpCmd).setDefault(true));
        addSubCommand(new BungeeSubCommandBuilder("reload", RELOAD_PERM, this::reloadCmd));
    }

    public void helpCmd(CommandSender sender, String[] args) {
        sender.sendMessage(Message.bungeeCommandHelpLine1.toString());
        sender.sendMessage(Message.bungeeCommandHelpHelpcmd.toString());
        sender.sendMessage(Message.bungeeCommandHelpReloadcmd.toString());
    }

    public void reloadCmd(CommandSender sender, String[] args) {
        DiscordConnect.getInstance().loadConfig();
        sender.sendMessage(Message.configReloaded.toString());
    }
}

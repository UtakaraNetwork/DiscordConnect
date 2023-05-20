package work.novablog.mcplugin.discordconnect.command;

import org.bukkit.command.CommandSender;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

public class BukkitCommand extends BukkitCommandExecutor {
    public static final String PERM = "discordconnect.command";

    private static final String RELOAD_PERM = "reload";
    private static final String DEBUG_PERM = "debug";

    public BukkitCommand() {
        super(PERM);
        addSubCommand(new BukkitSubCommandSettings("help", null, this::helpCmd).setDefault(true));
        addSubCommand(new BukkitSubCommandSettings("reload", RELOAD_PERM, this::reloadCmd));
        addSubCommand(new BukkitSubCommandSettings("debug", DEBUG_PERM, this::debugCmd));
    }

    public void helpCmd(CommandSender sender, String[] args) {
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpLine1.toString());
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpHelpcmd.toString());
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpReloadcmd.toString());
    }

    public void reloadCmd(CommandSender sender, String[] args) {
        DiscordConnect.getInstance().init();
        sender.sendMessage(ConfigManager.Message.configReloaded.toString());
    }

    public void debugCmd(CommandSender sender, String[] args) {
        sender.sendMessage("*Bot");
        sender.sendMessage("isActive: " + DiscordConnect.getInstance().canBotBeUsed());
    }
}

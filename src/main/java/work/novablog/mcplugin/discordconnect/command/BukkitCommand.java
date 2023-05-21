package work.novablog.mcplugin.discordconnect.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

public class BukkitCommand extends BukkitCommandExecutor {
    public static final String PERM = "discordconnect.command";

    private static final String RELOAD_PERM = "reload";
    private static final String DEBUG_PERM = "debug";
    private static final String LINK_PERM = "link";

    public BukkitCommand() {
        super(PERM);
        addSubCommand(new BukkitSubCommandSettings("help", null, this::helpCmd).setDefault(true));
        addSubCommand(new BukkitSubCommandSettings("link", LINK_PERM, this::linkCmd));
        addSubCommand(new BukkitSubCommandSettings("reload", RELOAD_PERM, this::reloadCmd));
        addSubCommand(new BukkitSubCommandSettings("debug", DEBUG_PERM, this::debugCmd));
    }

    public void helpCmd(CommandSender sender, String[] args) {
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpLine1.toString());
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpHelpcmd.toString());
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpLinkcmd.toString());
        sender.sendMessage(ConfigManager.Message.bungeeCommandHelpReloadcmd.toString());
    }

    public void linkCmd(CommandSender sender, String[] args) {
        DiscordConnect instance = DiscordConnect.getInstance();
        if (instance.getBotManager() == null || instance.getBotManager().getBotUser() == null)
            return;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ConfigManager.Message.bungeeCommandPlayerOnly.toString());
            return;
        }
        Player player = (Player) sender;
        String botName = instance.getBotManager().getBotUser().getName();
        String code = instance.getAccountManager().generateCode(player.getUniqueId());

        sender.sendMessage(ConfigManager.Message.accountLinkShowCode.toString()
                .replaceAll("\\{bot}", botName)
                .replaceAll("\\{code}", code));
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

package work.novablog.mcplugin.discordconnect.account.db;

import org.bukkit.configuration.ConfigurationSection;

public abstract class DatabaseConfig {

    protected final ConfigurationSection config;

    public DatabaseConfig(ConfigurationSection config) {
        this.config = config;
    }

    public abstract String getType();

    public ConfigurationSection getConfig() {
        return config;
    }

}

package work.novablog.mcplugin.discordconnect.account;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LinkedAccount {

    private final @NotNull UUID minecraftId;
    private final long discordId;

    public LinkedAccount(@NotNull UUID minecraftId, long discordId) {
        this.minecraftId = minecraftId;
        this.discordId = discordId;
    }

    public @NotNull UUID getMinecraftId() {
        return minecraftId;
    }

    public long getDiscordId() {
        return discordId;
    }

}

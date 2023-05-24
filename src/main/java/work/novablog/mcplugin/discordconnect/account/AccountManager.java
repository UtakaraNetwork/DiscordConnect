package work.novablog.mcplugin.discordconnect.account;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AccountManager {
    private final Map<String, UUID> linkingCodes = new ConcurrentHashMap<>();


    public abstract void connect() throws IOException;

    public abstract void close() throws IOException;


    public final String generateCode(UUID playerUuid) {
        String codeString;
        do {
            int code = ThreadLocalRandom.current().nextInt(10000);
            codeString = String.format("%04d", code);

        } while (linkingCodes.putIfAbsent(codeString, playerUuid) != null);
        return codeString;
    }

    public final @Nullable UUID removeMinecraftIdByLinkCode(@NotNull String code) {
        return linkingCodes.remove(code);
    }

    public final Map<String, UUID> linkingCodes() {
        return linkingCodes;
    }


    public abstract CompletableFuture<@NotNull Boolean> isLinkedDiscord(@NotNull UUID minecraftId);

    public abstract CompletableFuture<@Nullable Long> getLinkedDiscordId(@NotNull UUID minecraftId);

    public abstract CompletableFuture<Void> linkDiscordId(@NotNull UUID minecraftId, long discordId);

    public abstract CompletableFuture<@NotNull Boolean> isLinkedMinecraft(long discordId);

    public abstract CompletableFuture<@Nullable UUID> getLinkedMinecraftId(long discordId);

    public abstract CompletableFuture<Void> unlinkByMinecraftId(@NotNull UUID minecraftId);

    public abstract CompletableFuture<Void> unlinkByDiscordId(long discordId);


    public abstract CompletableFuture<LinkedAccount[]> getLinkedAccountAll();

    public abstract CompletableFuture<Integer> getLinkedAccountCount();

}

package work.novablog.mcplugin.discordconnect.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ConvertUtil {
    private static final String AVATAR_IMG_URL = "https://mineskin.eu/helm/{uuid}/100.png";

    /**
     * MinecraftプレイヤーのUUIDからアバターのURLを取得します
     * <p>
     *     uuidが無効の場合、代わりにSteveのアバターURLが返されます
     * </p>
     * @param uuid プレイヤーのUUID
     * @return プレイヤーのアバターURL
     * @see <a href="https://mineskin.eu/">Mineskin</a>を利用させていただいています
     */
    public static String getMinecraftAvatarURL(@NotNull UUID uuid) {
        String uuidText = uuid.toString();
        return AVATAR_IMG_URL.replace("{uuid}", uuidText);
    }
}

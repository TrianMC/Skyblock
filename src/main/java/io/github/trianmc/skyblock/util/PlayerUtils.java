package io.github.trianmc.skyblock.util;

import io.github.bluelhf.anemone.gui.ViewContext;
import io.github.trianmc.skyblock.config.Lang;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerUtils {
    public static String getName(UUID uuid) {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(op -> op.getUniqueId().equals(uuid))
                .map(OfflinePlayer::getName)
                .findFirst().orElse("null");
    }

    @Nullable
    public static UUID getUUID(String name) {
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(op -> Objects.equals(op.getName(), name))
                .map(OfflinePlayer::getUniqueId)
                .findFirst().orElse(null);
    }

    public static void with(Object obj, Consumer<Player> playerConsumer) {
        if (obj instanceof Player) {
            playerConsumer.accept((Player) obj);
        } else if (obj instanceof UUID) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID) obj);
            if (offlinePlayer.isOnline()) {
                playerConsumer.accept(offlinePlayer.getPlayer());
            }
        } else if (obj instanceof ViewContext) {
            with(((ViewContext) obj).getViewer(), playerConsumer);
        }
    }
    public static void send(Audience audience, Lang lang, String entry, Object... format) {
        audience.sendMessage(lang.get("generic.prefix").append(Component.text(" ").append(lang.get(entry, format))));
    }
}

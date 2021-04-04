package io.github.trianmc.skyblock.members;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class SkyPlayer {
    private Player player;
    private final UUID uuid;

    protected SkyPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    protected SkyPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.player = player;
    }

    public boolean hasPlayer() {
        return player != null;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void with(Consumer<Player> playerConsumer) {
        if (hasPlayer()) playerConsumer.accept(player);
    }

    public void with(Consumer<Player> playerConsumer, Consumer<UUID> fallback) {
        if (hasPlayer()) {
            playerConsumer.accept(player);
        } else {
            fallback.accept(uuid);
        }
    }

    public Player getPlayer() {
        return player;
    }
}

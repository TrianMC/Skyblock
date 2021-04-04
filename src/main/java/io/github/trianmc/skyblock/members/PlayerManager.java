package io.github.trianmc.skyblock.members;

import io.github.trianmc.skyblock.Skyblock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.UUID;

public class PlayerManager implements AutoCloseable, Listener {
    private final HashMap<UUID, SkyPlayer> playerMap = new HashMap<>();
    private final Skyblock host;

    public PlayerManager(Skyblock host) {
        this.host = host;
    }


    public SkyPlayer getPlayer(UUID uuid) {
        return addPlayer(uuid);
    }

    public SkyPlayer wrap(Player player) {
        return addPlayer(player);
    }


    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        addPlayer(event.getPlayer());
    }

    private void addOnline() {
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
    }

    private SkyPlayer addPlayer(Player player) {
        playerMap.putIfAbsent(player.getUniqueId(), new SkyPlayer(player));
        return playerMap.get(player.getUniqueId());
    }

    private SkyPlayer addPlayer(UUID uuid) {
        playerMap.putIfAbsent(uuid, new SkyPlayer(uuid));
        return playerMap.get(uuid);
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
    }
}

package io.github.trianmc.skyblock.board;

import fr.mrmicky.fastboard.FastBoard;
import io.github.bluelhf.tasks.Task;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.islands.Island;
import io.github.trianmc.skyblock.islands.IslandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;
import static org.bukkit.event.EventPriority.LOWEST;

public class SkyBoard implements AutoCloseable, Listener {

    private final Skyblock host;
    private final Map<Player, FastBoard> boardMap = new HashMap<>();

    public SkyBoard(Skyblock host) {
        this.host = host;

        Bukkit.getOnlinePlayers().forEach(this::add);
        Bukkit.getPluginManager().registerEvents(this, host);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(host, () -> {
            Lang lang = host.getLang();
            IslandManager man = host.getIslandManager();
            for (FastBoard board : boardMap.values()) {
                Island is = man.getIslandAt(board.getPlayer().getLocation());
                Task<Double, Double> levelTask;
                if (is != null) {
                    levelTask = is.calculateLevel();
                } else {
                    levelTask = Task.of(d -> -1D);
                    levelTask.run();
                }

                levelTask.onResult((level) -> {
                    String lvl = level == -1
                            ? gson().serialize(lang.get("generic.none"))
                            : "{\"text\": \"" + host.getEconomy().format(level) + "\"}";
                    List<String> lines = new ArrayList<>();
                    for (Component component : lang.getAll("board.lines",
                            gson().serialize(board.getPlayer().displayName()),
                            lvl,
                            gson().serialize(lang.get(
                                    "currency.plural",
                                    host.getEconomy().format(host.getEconomy().getBalance(board.getPlayer())))
                            ))) {
                        lines.add(LegacyComponentSerializer.legacySection().serialize(component));
                    }
                    board.updateLines(lines);
                });

                board.updateTitle(LegacyComponentSerializer.legacySection().serialize(lang.get("board.title")));
            }
        }, 0, 20);
    }

    @EventHandler(priority = LOWEST, ignoreCancelled = true)
    private void join(PlayerJoinEvent event) { add(event.getPlayer()); }

    @EventHandler(priority = LOWEST, ignoreCancelled = true)
    private void leave(PlayerQuitEvent event) { remove(event.getPlayer()); }

    private void add(Player player) {
        boardMap.putIfAbsent(player, new FastBoard(player));
    }

    private void remove(Player player) {
        boardMap.remove(player);
    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
    }
}

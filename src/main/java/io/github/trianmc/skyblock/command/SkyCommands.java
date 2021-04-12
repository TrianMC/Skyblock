package io.github.trianmc.skyblock.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import io.github.bluelhf.anemone.Anemones;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.gui.ControlAnemone;
import io.github.trianmc.skyblock.islands.Island;
import io.github.trianmc.skyblock.islands.IslandManager;
import io.github.trianmc.skyblock.util.PlayerUtils;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkyCommands implements Cloneable {
    private Skyblock host;
    private CommandAPICommand islandCommand;

    public SkyCommands(Skyblock host) {
        this.host = host;
        init();
    }

    protected void setHost(Skyblock newHost) {
        this.host = newHost;
    }

    private void init() {
        Lang lang = host.getLang();
        IslandManager manager = host.getIslandManager();

        islandCommand = new CommandAPICommand("island")
                .withAliases("is", "islands", "i")
                .executesPlayer((player, args) -> {
                    Anemones.open(player, ControlAnemone.class);
                })
                .withSubcommand(new CommandAPICommand("debug")
                        .executes((executor, args) -> {
                            host.getLang().debug();
                            executor.sendMessage("Debug state set to " + host.getLang().isDebug());
                        }))
                .withSubcommand(new CommandAPICommand("create")
                        .executesPlayer((player, args) -> {
                            manager.createIsland(player);
                            PlayerUtils.send(player, lang, "island.created");
                        }))
                .withSubcommand(new CommandAPICommand("peer")
                        .withArguments(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args[0];
                            Island island = manager.getIslandAt(player.getLocation());
                            if (island == null) {
                                PlayerUtils.send(player, lang, "player.not_on_island");
                                return;
                            }
                            if (target == player) {
                                PlayerUtils.send(player, lang, "invites.cannot_invite_self");
                                return;
                            }

                            // TODO: Send invite
                        }))
                .withSubcommand(new CommandAPICommand("level")
                        .executesPlayer((player, args) -> {
                            Island island = manager.getIslandAt(player.getLocation());
                            if (island == null) {
                                PlayerUtils.send(player, lang, "player.not_on_island");
                                return;
                            }

                            PlayerUtils.send(player, lang, "island.level.calculating");
                            long start = System.currentTimeMillis();
                            AtomicBoolean hasNotified = new AtomicBoolean(false);
                            DecimalFormat df = new DecimalFormat("##.#%");
                            island.calculateLevel()
                                    .whileRunning((progress) -> {
                                        if (System.currentTimeMillis() - start > 10000) {
                                            if (!hasNotified.get()) {
                                                PlayerUtils.send(player, lang, "island.level.long_calculation");
                                                hasNotified.set(true);
                                            }
                                            PlayerUtils.send(player, lang, "island.level.calculation_progress",
                                                    (int) Math.round(progress * island.getChunkCount()),
                                                    (int) island.getChunkCount(),
                                                    df.format(progress)
                                            );
                                        }
                                    }, Duration.ofMillis(5000))
                                    .onResult(result -> PlayerUtils.send(player, lang, "island.level.calculated",
                                            result.intValue(),
                                            System.currentTimeMillis() - start));
                        }));
    }

    public void unregister() {
        CommandAPI.unregister(islandCommand.getName());
    }

    public void register() {
        islandCommand.register();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

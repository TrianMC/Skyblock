package io.github.trianmc.skyblock.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.bluelhf.anemone.Anemones;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.gui.ControlAnemone;
import io.github.trianmc.skyblock.islands.Island;
import io.github.trianmc.skyblock.islands.IslandManager;
import io.github.trianmc.skyblock.util.PlayerUtils;
import io.github.trianmc.skyblock.util.sfx.Ambience;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson;

public class SkyCommands implements Cloneable {
    private Skyblock host;
    private CommandAPICommand islandCommand;
    private CommandAPICommand tokensCommand;
    private CommandAPICommand skyblockCommand;

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

        skyblockCommand = new CommandAPICommand("skyblock")
                .withAliases("sb")
                .withSubcommand(new CommandAPICommand("reload")
                        .executesPlayer((player, args) -> {
                            long start = System.currentTimeMillis();
                            host.reload();
                            long end = System.currentTimeMillis();
                            player.sendMessage(host.getLang().get("generic.reloaded", end - start));
                        }));

        tokensCommand = new CommandAPICommand("token")
                .withAliases("tk", "tokens", "tks")
                .executesPlayer((player, args) -> {
                    player.sendMessage(host.getLang().get(
                                    "tokens.info",
                                    host.getEconomy().getBalance(player)
                            ));
                })
                .withSubcommand(new CommandAPICommand("pay")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount", 1))
                        .executesPlayer((player, args) -> {
                            Economy economy = host.getEconomy();
                            if (!economy.has(player, (int) args[1])) {
                                player.sendMessage(host.getLang().get(
                                        "tokens.not_enough_tokens",
                                        ((int) (args[1]) - (int) host.getEconomy().getBalance(player))
                                ));
                                Ambience.error(player);
                                return;
                            }
                            economy.withdrawPlayer(player, (int) args[1]);
                            economy.depositPlayer((Player) args[0], (int) args[1]);
                            player.sendMessage(host.getLang().get(
                                    "tokens.transferred",
                                    gson().serialize(lang.get("currency.plural", economy.format((int) args[1]))),
                                    gson().serialize(((Player) args[0]).displayName())
                            ));
                        })
                )
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new PlayerArgument("target"), new IntegerArgument("amount", 1))
                        .executesPlayer((player, args) -> {
                            Economy economy = host.getEconomy();
                            economy.depositPlayer((Player) args[0], (int) args[1]);
                            player.sendMessage(host.getLang().get(
                                    "tokens.transferred",
                                    gson().serialize(lang.get("currency.plural", economy.format((int) args[1]))),
                                    gson().serialize(((Player) args[0]).displayName())
                            ));
                        }));

        islandCommand = new CommandAPICommand("island")
                .withAliases("is", "islands", "i")
                .executesPlayer((player, args) -> {
                    Anemones.open(player, ControlAnemone.class);
                })
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
                                Ambience.error(player);
                                return;
                            }
                            if (target == player) {
                                PlayerUtils.send(player, lang, "invites.cannot_invite_self");
                                Ambience.error(player);
                                return;
                            }

                            // TODO: Send invite
                        }))
                .withSubcommand(new CommandAPICommand("level")
                        .executesPlayer((player, args) -> {
                            Island island = manager.getIslandAt(player.getLocation());
                            if (island == null) {
                                PlayerUtils.send(player, lang, "player.not_on_island");
                                Ambience.error(player);
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
        CommandAPI.unregister(tokensCommand.getName());
        CommandAPI.unregister(skyblockCommand.getName());
    }

    public void register() {
        islandCommand.register();
        tokensCommand.register();
        skyblockCommand.register();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

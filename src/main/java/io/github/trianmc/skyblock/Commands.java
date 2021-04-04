package io.github.trianmc.skyblock;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.trianmc.skyblock.islands.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

public class Commands implements Cloneable {
    private Skyblock host;
    private CommandAPICommand islandCommand;
    private ArrayList<UUID> levelCalculators = new ArrayList<>();

    public Commands(Skyblock host) {
        this.host = host;
        init();
    }

    protected void setHost(Skyblock newHost) {
        this.host = newHost;
    }

    private void init() {
         islandCommand = new CommandAPICommand("island")
                .withAliases("is", "islands", "i")
                .withSubcommand(new CommandAPICommand("create")
                        .executesPlayer((player, args) -> {
                            host.getIslandManager().createIsland(
                                    host.getPlayerManager().wrap(player)
                            );
                        }))
                 .withSubcommand(new CommandAPICommand("level")
                         .executesPlayer((player, args) -> {
                             if (levelCalculators.contains(player.getUniqueId())) {
                                 player.sendMessage(Component.text("You're already calculating an island level!").color(NamedTextColor.RED));
                                 return;
                             }
                             Island island = host.getIslandManager().getIslandAt(player.getLocation());
                             if (island == null) {
                                 player.sendMessage(Component.text("You're not on an island!").color(NamedTextColor.RED));
                                 return;
                             }
                             levelCalculators.add(player.getUniqueId());
                             player.sendMessage(Component.text("Calculating level...").color(NamedTextColor.YELLOW));
                             island.calculateLevel().onResult(result -> {
                                         player.sendMessage(Component.empty()
                                                 .append(Component.text("Your island level is: "))
                                                 .append(Component.text(result.intValue()).color(NamedTextColor.GREEN)));
                                         levelCalculators.remove(player.getUniqueId());
                                     });
                         }));
    }


    public void register() {
        islandCommand.register();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

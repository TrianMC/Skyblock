package io.github.trianmc.skyblock.gui;

import io.github.bluelhf.anemone.gui.Anemone;
import io.github.bluelhf.anemone.gui.Index;
import io.github.bluelhf.anemone.gui.ViewContext;
import io.github.bluelhf.anemone.util.Items;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.islands.Island;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.util.sfx.Ambience;
import io.github.trianmc.skyblock.util.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static io.github.trianmc.skyblock.util.sfx.Components.unstyle;

public class ControlAnemone extends Anemone {

    private final Skyblock host;

    public ControlAnemone(Skyblock host) {
        this.host = host;
    }

    @Override
    public @NotNull List<String> getTemplate() {
        return Arrays.asList(
                "xxxxxxxxx",
                "xxxxxxxxx",
                "<C######>"
        );
    }

    @Override
    public @NotNull ItemStack itemFor(Index index, ViewContext viewContext) {
        Lang lang = host.getLang();
        switch (index.getChar()) {
            case 'x':
                List<Island> islands = host.getIslandManager()
                        .getIslands(viewContext.getViewer().getUniqueId());

                if (index.getCharIndex() >= islands.size()) return new ItemStack(Material.AIR);
                Island is = islands.get(index.getCharIndex());
                return is.getIcon(index.getCharIndex(), viewContext);
            case '<':
                return Items.of(Material.FEATHER)
                        .modifyMeta(meta -> meta.displayName(unstyle(lang.get("gui.back"))))
                        .build();
            case '>':
                return Items.of(Material.ARROW)
                        .modifyMeta(meta -> meta.displayName(unstyle(lang.get("gui.next"))))
                        .build();
            case 'C':
                return Items.of(Material.GOLD_BLOCK)
                        .modifyMeta(meta -> meta.displayName(unstyle(lang.get("gui.create"))))
                        .build();
        }
        return new ItemStack(Material.AIR);
    }

    @Override
    protected void onClick(Index index, ViewContext context, InventoryClickEvent event) {
        Lang lang = host.getLang();
        PlayerUtils.with(context, Ambience::click);
        event.setCancelled(true);
        switch (index.getChar()) {
            case 'x':
                List<Island> islands = host.getIslandManager()
                        .getIslands(context.getViewer().getUniqueId());

                if (index.getCharIndex() >= islands.size()) {
                    return;
                }

                Island is = islands.get(index.getCharIndex());

                switch (event.getClick()) {
                    case DROP:
                    case CONTROL_DROP:
                        host.getIslandManager().removeIsland(is);
                        context.update();
                        break;
                    case LEFT:
                    case SHIFT_LEFT:
                    case CREATIVE:
                        PlayerUtils.with(context, player -> player.teleportAsync(is.getLocation()).thenRun(() ->
                                PlayerUtils.send(player, lang, "generic.teleported")));
                }
                break;
            case '<':
                context.previous();
                break;
            case '>':
                context.next();
                break;
            case 'C':
                PlayerUtils.with(context, player -> {
                    host.getIslandManager().createIsland(player);
                    PlayerUtils.send(player, lang, "island.created");
                });
                break;
        }
    }
}

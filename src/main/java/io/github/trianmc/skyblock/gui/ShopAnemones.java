package io.github.trianmc.skyblock.gui;

import io.github.bluelhf.anemone.gui.Anemone;
import io.github.bluelhf.anemone.gui.Index;
import io.github.bluelhf.anemone.gui.ViewContext;
import io.github.bluelhf.anemone.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ShopAnemones {
    public static class Menu extends Anemone {

        @Override
        public @NotNull List<String> getTemplate() {
            return Arrays.asList(
                    "#########",
                    "####x####",
                    "#########",
                    "#########",
                    "#########",
                    "#########"
            );
        }

        @Override
        public @NotNull ItemStack itemFor(Index index, ViewContext viewContext) {
            switch (index.getChar()) {
                case '#':
                    return Items.of(Material.GRAY_STAINED_GLASS_PANE)
                            .modifyMeta(meta -> meta.displayName(Component.empty()))
                            .build();
                case 'x':
                default:
                    return Items.of(Material.AIR).build();
            }
        }
    }
}

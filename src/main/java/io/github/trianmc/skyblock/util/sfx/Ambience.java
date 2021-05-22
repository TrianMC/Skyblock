package io.github.trianmc.skyblock.util.sfx;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Ambience {
    public static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public static void error(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
    }
}

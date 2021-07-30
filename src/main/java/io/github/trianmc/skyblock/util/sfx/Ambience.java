package io.github.trianmc.skyblock.util.sfx;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import static org.bukkit.Sound.UI_BUTTON_CLICK;

public class Ambience {

    public static void sound(Player player, Sound sound, float... details) {
        float pitch, volume;
        switch (details.length) {
            case 1:
                pitch = details[0]; volume = 1;
                break;
            case 2:
                pitch = details[0]; volume = details[1];
                break;
            default:
                pitch = 1; volume = 1;
                break;
        }

        player.playSound(player.getLocation(), sound, pitch, volume);
    }

    public static void click(Player player) {
        sound(player, UI_BUTTON_CLICK);
    }

    public static void error(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
    }
}

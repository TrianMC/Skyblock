package io.github.trianmc.skyblock.nms;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface INms {
    void setBorder(Player player, Location location, double size);
    void resetBorder(Player player);
}

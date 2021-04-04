package io.github.trianmc.skyblock.nms;

import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_16_R3.WorldBorder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class v1_16_R3 implements INms {

    private void setBorder(Player player, Location location, double size, @Nullable Double damageAmount, @Nullable Double damageBuffer, @Nullable Integer warningDistance, @Nullable Integer warningTime) {
        CraftPlayer craftPlayer = (CraftPlayer)player;
        EntityPlayer handle = craftPlayer.getHandle();
        WorldBorder worldBorder = new WorldBorder();
        worldBorder.world = ((CraftWorld)location.getWorld()).getHandle();
        worldBorder.setCenter(location.getX(), location.getZ());
        worldBorder.setSize(size);
        if (damageAmount != null) worldBorder.setDamageAmount(damageAmount);
        if (damageBuffer != null) worldBorder.setDamageBuffer(damageBuffer);
        if (warningDistance != null) worldBorder.setWarningDistance(warningDistance);
        if (warningTime != null) worldBorder.setWarningTime(warningTime);
        handle.playerConnection.sendPacket(new PacketPlayOutWorldBorder(worldBorder, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE));
    }

    @Override
    public void setBorder(Player player, Location location, double size) {
        setBorder(player, location, size, null, null, null, null);
    }

    @Override
    public void resetBorder(Player player) {
        org.bukkit.WorldBorder wb = player.getWorld().getWorldBorder();
        setBorder(
                player,
                wb.getCenter(),
                wb.getSize(),
                wb.getDamageAmount(),
                wb.getDamageBuffer(),
                wb.getWarningDistance(),
                wb.getWarningTime()
        );
    }
}

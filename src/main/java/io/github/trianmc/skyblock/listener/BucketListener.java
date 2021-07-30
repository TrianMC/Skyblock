package io.github.trianmc.skyblock.listener;

import io.github.trianmc.skyblock.util.sfx.Ambience;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class BucketListener implements Listener {

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        Block b;
        EntityEquipment equipment;
        if (
                (b = event.getClickedBlock())                  == null              ||
                (equipment = event.getPlayer().getEquipment()) == null              ||
                equipment.getItemInMainHand().getType()        != Material.BUCKET   ||
                b.getType()                                    != Material.OBSIDIAN
        ) return;

        b.setType(Material.AIR);
        equipment.setItemInMainHand(new ItemStack(Material.LAVA_BUCKET));
        Ambience.sound(event.getPlayer(), Sound.ITEM_BUCKET_FILL_LAVA);
        event.setUseItemInHand(Event.Result.DENY);
    }
}

package io.github.trianmc.skyblock.listener;

import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.islands.Island;
import io.github.trianmc.skyblock.members.Rights;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class ProtectionListener implements Listener {
    private final Skyblock host;
    private HashMap<UUID, Long> cooldownMap = new HashMap<>();

    public ProtectionListener(Skyblock host) {
        this.host = host;
    }
    private void protect(Location location, Player player, Runnable r) {
        withIsland(location, (island) -> {
            if (!isPeer(player, island)) {
                if (System.currentTimeMillis() > cooldownMap.getOrDefault(player.getUniqueId(), 0L)) {
                    player.sendMessage(Component.text("You cannot do that as a visitor!").color(NamedTextColor.RED));
                    cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + 10000L);
                }
                r.run();
            }
        });
    }

    private boolean isPeer(Player player, Island island) {
        return island.getMembers().hasRights(player.getUniqueId(), Rights.PEER);
    }

    private void withIsland(Location location, Consumer<Island> consumer) {
        if (host.getIslandManager().getIslandAt(location) != null) {
            consumer.accept(host.getIslandManager().getIslandAt(location));
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        protect(event.getBed().getLocation(), event.getPlayer(), () -> {
            event.setCancelled(true);
            event.setUseBed(Event.Result.DENY);
        });
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        protect(event.getBlock().getLocation(), event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        protect(event.getBlock().getLocation(), event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler
    public void onCheck(BlockCanBuildEvent event) {
        protect(event.getBlock().getLocation(), event.getPlayer(), () -> event.setBuildable(false));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        protect(event.getBlock().getLocation(), event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler
    public void onTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (event.getClickedBlock() == null) return;
        protect(event.getClickedBlock().getLocation(), event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        protect(event.getEntity().getLocation(), player, () -> event.setCancelled(true));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        protect(event.getClickedBlock().getLocation(), event.getPlayer(), () -> {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        });
    }
}

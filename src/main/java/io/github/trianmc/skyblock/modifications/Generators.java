package io.github.trianmc.skyblock.modifications;

import io.github.trianmc.skyblock.Skyblock;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import java.util.EnumMap;
import java.util.Map;

public class Generators implements Listener {
    private static final EnumMap<Material, Double> GENERABLES = new EnumMap<>(Material.class);
    private static double sum = 0;

    static {
        // TODO: Config
        GENERABLES.put(Material.COBBLESTONE,     50.000);
        GENERABLES.put(Material.GOLD_ORE,        05.000);
        GENERABLES.put(Material.IRON_ORE,        07.000);
        GENERABLES.put(Material.COAL_ORE,        10.000);
        GENERABLES.put(Material.LAPIS_ORE,       03.000);
        GENERABLES.put(Material.DIAMOND_ORE,     00.100);
        GENERABLES.put(Material.REDSTONE_ORE,    02.000);
        GENERABLES.put(Material.EMERALD_ORE,     00.500);
        GENERABLES.put(Material.ANCIENT_DEBRIS,  00.010);
        GENERABLES.put(Material.GOLD_BLOCK,      00.100);
        GENERABLES.put(Material.IRON_BLOCK,      00.200);
        GENERABLES.put(Material.COAL_BLOCK,      02.000);
        GENERABLES.put(Material.LAPIS_BLOCK,     00.300);
        GENERABLES.put(Material.REDSTONE_BLOCK,  00.300);
        GENERABLES.put(Material.DIAMOND_BLOCK,   00.020);
        GENERABLES.put(Material.NETHERITE_BLOCK, 00.005);
        for (double f : GENERABLES.values()) sum += f;
    }

    private final Skyblock host;

    public Generators(Skyblock host) {
        this.host = host;
    }

    @EventHandler
    public void onGenerate(BlockFormEvent event) {
        if (host.getIslandManager().getIslandAt(event.getBlock().getLocation()) == null) return;
        if (event.getNewState().getType() == Material.COBBLESTONE)
            event.getNewState().setType(getRandom());
    }
    
    public static Material getRandom() {
        double random = sum * Math.random();
        for (Map.Entry<Material, Double> entry : GENERABLES.entrySet()) {
            if (random < entry.getValue()) {
                return entry.getKey();
            }
            random -= entry.getValue();
        }
        throw new IllegalStateException("Oh no!");
    }
}

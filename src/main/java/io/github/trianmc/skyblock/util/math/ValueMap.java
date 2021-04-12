package io.github.trianmc.skyblock.util.math;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class ValueMap extends Object2DoubleOpenHashMap<QuickBlockData> {
    private final ReferenceArraySet<Material> materials = new ReferenceArraySet<>();

    public static ValueMap fromSection(ConfigurationSection section) {
        ValueMap vm = new ValueMap();
        vm.defaultReturnValue(section.getDouble("default", 0));
        for (String key : section.getKeys(false)) {
            if (key.equals("default")) continue;
            try {
                QuickBlockData qbd = new QuickBlockData(Bukkit.getServer().createBlockData(key));
                if (vm.defRetValue == section.getDouble(key)) continue;
                vm.put(qbd, section.getDouble(key, vm.defRetValue));
                vm.materials.add(qbd.bd.getMaterial());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return vm;
    }

    @Override
    public double getDouble(Object k) {
        if (!(k instanceof QuickBlockData)) return 0;
        if (!materials.contains(((QuickBlockData)k).bd.getMaterial())) return 0;
        return super.getDouble(k);
    }
}

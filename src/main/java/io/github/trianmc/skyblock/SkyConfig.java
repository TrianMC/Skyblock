package io.github.trianmc.skyblock;

import io.github.trianmc.skyblock.util.ValueMap;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.util.HashMap;

public class SkyConfig {
    private final Skyblock host;
    private final @NotNull FileConfiguration config;
    private ValueMap valueMap;

    public SkyConfig(Skyblock host) {
        this.host = host;
        this.config = host.getConfig();
        if (!Files.exists(host.getDataFolder().toPath().resolve("config.yml"))) {
            host.saveDefaultConfig();
        }

        valueMap = buildValueMap();
    }

    protected ValueMap buildValueMap() {
        HashMap<BlockData, Double> valueMap = new HashMap<>();
        ConfigurationSection values = config.getConfigurationSection("levels");
        if (values != null) {
            for (String key : values.getKeys(false)) {
                if (key.equals("default")) continue;
                try {
                    valueMap.put(Bukkit.getServer().createBlockData(key), values.getDouble(key, values.getDouble("default", 0)));
                } catch (IllegalArgumentException ex) {
                    host.getLogger().warning(key + " is not a valid block data nor default!");
                }
            }
        }

        return this.valueMap = ValueMap.builder()
                .def(values != null ? values.getDouble("default", 0) : 0)
                .valueMap(valueMap)
                .build();
    }

    public ValueMap getValueMap() {
        return valueMap;
    }
}

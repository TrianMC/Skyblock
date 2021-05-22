package io.github.trianmc.skyblock.config;

import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.util.math.ValueMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class SkyConfig {
    private final Skyblock host;
    private final @NotNull FileConfiguration config;
    private final ValueMap valueMap;

    public SkyConfig(Skyblock host) {
        this.host = host;
        this.config = host.getConfig();
        if (!Files.exists(host.getDataFolder().toPath().resolve("config.yml"))) {
            host.saveDefaultConfig();
        }

        valueMap = buildValueMap();
    }

    protected ValueMap buildValueMap() {
        ConfigurationSection values = config.getConfigurationSection("levels");
        if (values != null) {
            return ValueMap.fromSection(values);
        }
        return new ValueMap();
    }

    public Path getLangPath() {
        if (!config.contains("language") || "none".equalsIgnoreCase(config.getString("language"))) return null;
        return host.getDataFolder().toPath()
                .resolve("lang")
                .resolve(config.getString("language", "en_GB") + ".json");
    }

    public ValueMap getValueMap() {
        return valueMap;
    }
}

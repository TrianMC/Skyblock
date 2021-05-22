package io.github.trianmc.skyblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import dev.jorel.commandapi.CommandAPI;
import io.github.bluelhf.anemone.Anemones;
import io.github.trianmc.skyblock.board.SkyBoard;
import io.github.trianmc.skyblock.command.SkyCommands;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.config.SkyConfig;
import io.github.trianmc.skyblock.economy.SkyEconomy;
import io.github.trianmc.skyblock.generator.VoidGenerator;
import io.github.trianmc.skyblock.gui.ControlAnemone;
import io.github.trianmc.skyblock.islands.IslandManager;
import io.github.trianmc.skyblock.listener.ProtectionListener;
import io.github.trianmc.skyblock.modifications.Generators;
import io.github.trianmc.skyblock.util.IOUtils;
import io.github.trianmc.skyblock.util.JsonUtils;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class Skyblock extends JavaPlugin {

    public static final Executor LEVEL_EXECUTOR = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

    private Lang lang;
    private IslandManager islandManager;
    private SkyCommands skyCommands;
    private SkyConfig config;

    private SkyEconomy economy;
    private boolean shouldWrite = true;

    private SkyBoard board;

    @NotNull
    public SkyConfig getSkyConfig() {
        return config;
    }

    @SuppressWarnings("unused")
    public SkyCommands getCommands() {
        return skyCommands;
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(false);
    }

    @Override
    public void onEnable() {
        this.config = new SkyConfig(this);
        this.board = new SkyBoard(this);
        // Explicitly avoid handling IOException, error is critical
        islandManager = new IslandManager(this, getDataFolder().toPath());

        initLang();
        initEconomy();

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new Generators(this), this);

        Anemones.init(this);
        Anemones.register(new ControlAnemone(this));

        generateWorld();

        CommandAPI.onEnable(this);
        skyCommands = new SkyCommands(this);
        skyCommands.register();
    }

    public void reload() {
        initLang();
        initEconomy();
        reloadConfig();
        config = new SkyConfig(this);

        board.close();
        board = new SkyBoard(this);
    }

    @SneakyThrows
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        islandManager.close();
        skyCommands.unregister();
        if (shouldWrite) {
            try (OutputStream stream = IOUtils.write(getDataFolder().toPath().resolve("economy.dat"))) {
                economy.write(stream);
            }

        }
    }

    private void initEconomy() {
        Path econPath = getDataFolder().toPath().resolve("economy.dat");
        if (!Files.exists(econPath) || !Files.isRegularFile(econPath)) {
            economy = new SkyEconomy(this);
        } else try {
            economy = SkyEconomy.read(this, IOUtils.read(econPath));
        } catch (Exception e) {
            getLogger().severe("An error occurred while loading the economy data!");
            getLogger().severe("To prevent data loss, economy data will not be saved!");
            shouldWrite = false;
            economy = new SkyEconomy(this);
        }

        getServer().getServicesManager().register(Economy.class, economy, this, ServicePriority.Highest);
    }

    private void initLang() {
        InputStream resourceStream = Skyblock.class.getResourceAsStream("/en_GB.json");
        if (resourceStream == null) {
            throw new IllegalStateException("Could not find default language file! Is the JAR corrupted?");
        }
        Lang defLang = new Lang(resourceStream);

        Path optionPath = getSkyConfig().getLangPath();
        if (optionPath == null || !Files.exists(optionPath)) {
            if (optionPath != null) getLogger().warning("Could not find language file: " + optionPath.getFileName());
            lang = defLang;
        } else {
            ArrayList<Runnable> runLater = new ArrayList<>();
            try (InputStream optionStream = IOUtils.read(optionPath)) {
                Lang optionLang = new Lang(optionStream);
                boolean old = defLang.getString("lang.version").equals(optionLang.getString("lang.version"));
                HashMap<String, Integer> changeMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : defLang.getJson().entrySet()) {
                    if (!optionLang.getJson().has(entry.getKey())) {
                        changeMap.put(entry.getKey(), 1);
                    } else if (!JsonUtils.equals(optionLang.getJson().get(entry.getKey()), entry.getValue())) {
                        changeMap.put(entry.getKey(), 2);
                    } else {
                        changeMap.put(entry.getKey(), 3);
                    }
                }
                for (Map.Entry<String, JsonElement> entry : optionLang.getJson().entrySet()) {
                    if (!defLang.getJson().has(entry.getKey())) changeMap.put(entry.getKey(), 4);
                }

                Map<String, JsonElement> combined = new HashMap<>();
                for (Map.Entry<String, Integer> change : changeMap.entrySet()) {
                    String key = change.getKey();
                    int type = change.getValue();
                    switch (type) {
                        case 1: // Missing from option language
                            combined.put(key, defLang.getJson().get(key));
                            break;
                        case 2: // Modified in option language
                        case 3:
                            combined.put(key, optionLang.getJson().get(key));
                            break;
                        case 4: // Added in option language
                            if (!old) {
                                getLogger().warning("Language key " + key + " in " + optionPath.getFileName() + " is useless");
                            }
                            getLogger().warning("Removing unused language key " + key);
                    }
                }
                runLater.add(() -> {
                    Gson gson = new GsonBuilder()
                            .enableComplexMapKeySerialization()
                            .setPrettyPrinting()
                            .create();

                    Type gsonType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
                    try (JsonWriter writer = gson.newJsonWriter(IOUtils.writer(optionPath))) {
                        gson.toJson(combined, gsonType, writer);
                    } catch (IOException e) {
                        getLogger().warning("Failed to write new language file! Using language option blindly.");
                        lang = optionLang;
                    }
                });
            } catch (IOException e) {
                getLogger().severe("Failed to read language file! Falling back to stream.");
                lang = defLang;
            }
            runLater.forEach(Runnable::run);
        }
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    @NotNull
    private NamespacedKey getWorldKey() {
        NamespacedKey islandsKey = NamespacedKey.fromString("islands", this);
        if (islandsKey == null) throw new IllegalArgumentException("Invalid world key!");
        return islandsKey;
    }

    public World generateWorld() {
        World w;
        if ((w = Bukkit.getWorld(getWorldKey())) != null) {
            return w;
        }

        return WorldCreator.ofKey(getWorldKey())
                .environment(World.Environment.NORMAL)
                .generator(new VoidGenerator())
                .createWorld();
    }


    public Lang getLang() {
        return lang;
    }

    public Economy getEconomy() {
        return economy;
    }
}

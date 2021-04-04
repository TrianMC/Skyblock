package io.github.trianmc.skyblock;

import dev.jorel.commandapi.CommandAPI;
import io.github.trianmc.skyblock.islands.IslandManager;
import io.github.trianmc.skyblock.listener.ProtectionListener;
import io.github.trianmc.skyblock.members.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class Skyblock extends JavaPlugin {

    public static final Executor LEVEL_EXECUTOR = Executors.newFixedThreadPool(128);
    private PlayerManager playerManager;
    private IslandManager islandManager;
    private Commands commands;
    private SkyConfig config;

    @NotNull
    public SkyConfig getSkyConfig() {
        return config;
    }

    public Commands getCommands() {
        return commands;
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(false);
    }

    @Override
    public void onEnable() {
        this.config = new SkyConfig(this);
        playerManager = new PlayerManager(this);

        // Explicitly avoid handling IOException, error is critical
        islandManager = new IslandManager(this, getDataFolder().toPath());


        CommandAPI.onEnable(this);
        commands = new Commands(this);
        commands.register();

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);

        generateWorld();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        islandManager.close();
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
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


}

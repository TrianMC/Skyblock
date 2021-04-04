package io.github.trianmc.skyblock.islands;

import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.members.Members;
import io.github.trianmc.skyblock.members.Rights;
import io.github.trianmc.skyblock.members.SkyPlayer;
import io.github.trianmc.skyblock.util.IOUtils;
import io.github.trianmc.skyblock.util.MathUtils;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class IslandManager implements AutoCloseable {
    private final ArrayList<Island> islands = new ArrayList<>();
    private final Skyblock host;
    private final Path dataFile;
    private final Path schematicFolder;
    private final int borderTaskID;

    /**
     * Constructs a new IslandManager
     *
     * @throws IOException If reading the file fails
     */
    @SuppressWarnings("JavaDoc")
    @SneakyThrows
    public IslandManager(Skyblock host, Path dataFolder) {
        this.host = host;

        this.dataFile = dataFolder.resolve("islands.dat");
        this.schematicFolder = dataFolder.resolve("templates");
        Files.createDirectories(schematicFolder);
        read();

        borderTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(host, () -> {
            HashMap<Island, ArrayList<Player>> playerMap = new HashMap<>();
            for (Island is : islands) {
                playerMap.putIfAbsent(is, new ArrayList<>());
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                Island is = getIslandAt(player.getLocation());
                if (is != null) {
                    playerMap.get(is).add(player);
                }
            }
            for (Island island : playerMap.keySet()) {
                island.update(playerMap.get(island));
            }
        }, 20, 10);
    }

    @Nullable
    public Island getIslandAt(Location location) {
        for (Island island : islands) {
            if (island.onIsland(location)) return island;
        }

        return null;
    }

    public void createIsland(SkyPlayer owner) {
        int[] pos = MathUtils.spiralAt(islands.size());
        int x = pos[0] * 1024;
        int z = pos[1] * 1024;

        World world = getWorld();
        Location loc = new Location(world,
                x, 128, z);

        owner.with((player) -> player
                        .teleportAsync(loc)
                        .thenRun(() -> pasteIsland(loc)),
                (uuid) -> pasteIsland(loc));

        Island island = new Island.IslandBuilder()
                .host(this)
                .location(loc)
                .members(new Members(owner.getUUID()))
                .size(32)
                .build();


        islands.add(island);
    }

    private Operation pasteIsland(Location location) {
        URIClipboardHolder[] holders = ClipboardFormats.loadAllFromDirectory(schematicFolder.toFile());

        URIClipboardHolder holder = holders[(int) (Math.random() * holders.length)];
        Operation operation = holder.createPaste(BukkitAdapter.adapt(location).getExtent())
                .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .ignoreAirBlocks(true)
                .copyEntities(true)
                .copyBiomes(true)
                .build();
        Operations.complete(operation);
        return operation;
    }


    protected void read() throws IOException {
        if (Files.notExists(dataFile)) return;
        try (InputStream stream = IOUtils.read(dataFile)) {
            int num = IOUtils.readInt(stream);
            for (int i = 0; i < num; i++) {
                Island island = Island.read(this, stream);
                islands.add(island);
            }
        }
    }

    @SneakyThrows
    public int write() {
        int size = 0;
        try (OutputStream stream = IOUtils.write(dataFile)) {
            size += IOUtils.writeInt(stream, islands.size());
            for (Island island : islands) {
                size += island.write(stream);
            }
        }

        return size;
    }

    public Collection<Island> getIslands(UUID uuid) {
        return islands.stream()
                .filter(island -> island.getMembers().hasRights(uuid, Rights.OWNER))
                .collect(Collectors.toUnmodifiableList());
    }

    public World getWorld() {
        return host.generateWorld();
    }

    public Skyblock getHost() {
        return host;
    }

    @Override
    public void close() {
        Bukkit.getScheduler().cancelTask(borderTaskID);
        write();
    }
}

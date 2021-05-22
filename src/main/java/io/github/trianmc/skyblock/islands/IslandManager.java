package io.github.trianmc.skyblock.islands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.members.Members;
import io.github.trianmc.skyblock.members.Rights;
import io.github.trianmc.skyblock.util.IOUtils;
import io.github.trianmc.skyblock.util.math.MathUtils;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IslandManager implements AutoCloseable {
    private final ArrayList<Island> islands = new ArrayList<>();
    private final Skyblock host;
    private final Path dataFile;
    private final Path schematicFolder;

    private final int islandTickTask;
    private long islandTick = 0;

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

        final int period = 10;
        islandTickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(host, () -> {
            islandTick++;
            boolean levels = islandTick % (200 / period) == 0;
            HashMap<Island, ArrayList<Player>> playerMap = new HashMap<>();
            for (Island is : islands) {
                if (levels) is.calculateLevel();
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
        }, 20, period);
    }

    @Nullable
    public Island getIslandAt(Location location) {
        for (Island island : islands) {
            if (island.onIsland(location)) return island;
        }

        return null;
    }

    public void createIsland(Player owner) {
        int[] pos = MathUtils.spiralAt(islands.size());
        int x = pos[0] * 1024;
        int z = pos[1] * 1024;

        World world = getWorld();
        Location loc = new Location(world,
                x, 128, z);

        pasteIsland(loc);
        owner.teleportAsync(loc).thenRun(() -> pasteIsland(loc));

        Island island = new Island.IslandBuilder()
                .host(this)
                .location(loc)
                .members(new Members(owner.getUniqueId()))
                .size(32)
                .build();


        islands.add(island);
    }

    @SneakyThrows
    private Operation pasteIsland(Location location) {
        List<Path> files = Files.list(schematicFolder).collect(Collectors.toList());
        Collections.shuffle(files);

        Path path = null;
        ClipboardFormat format = null;
        for (Path pathCandidate : files) {
            ClipboardFormat formatCandidate = ClipboardFormats.findByFile(pathCandidate.toFile());
            if (formatCandidate != null) {
                path = pathCandidate;
                format = formatCandidate;
                break;
            }
        }
        if (path == null)
            throw new IllegalStateException("There are no valid island schematics in the schematic folder.");

        Clipboard clipboard;
        try (ClipboardReader reader = format.getReader(new FileInputStream(path.toFile()))) {
            clipboard = reader.read();
        }

        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                    .ignoreAirBlocks(true)
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build();

            Operations.complete(operation);
            return operation;
        }
    }

    public void removeIsland(Island island) {
        ArrayList<CompletableFuture<Void>> tpFutures = new ArrayList<>();
        for (Player player : island.getVisitorCache()) {
            tpFutures.add(player.teleportAsync(player.getWorld().getSpawnLocation()).thenAccept(result -> {}));
        }

        CompletableFuture.allOf(tpFutures.toArray(CompletableFuture[]::new)).thenRun(() -> {
            island.update(new ArrayList<>());
            island.clear();
            islands.remove(island);
        });
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

    public List<Island> getIslands(UUID uuid) {
        return islands.stream()
                .filter(island -> island.getMembers().hasRights(uuid, Rights.PEER))
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
        Bukkit.getScheduler().cancelTask(islandTickTask);
        write();
    }
}

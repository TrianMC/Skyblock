package io.github.trianmc.skyblock.islands;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.bluelhf.tasks.Task;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.members.Members;
import io.github.trianmc.skyblock.members.SkyPlayer;
import io.github.trianmc.skyblock.nms.Mediator;
import io.github.trianmc.skyblock.util.IOUtils;
import io.github.trianmc.skyblock.util.ValueMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;


public class Island {
    private IslandManager host;
    private Members members;
    private Location location;
    private int size;

    private ArrayList<Player> pastVisitors = new ArrayList<>();


    private Island(IslandManager host) {
        this.host = host;
    }

    @Builder(access = AccessLevel.PROTECTED)
    protected Island(IslandManager host, Members members, Location location, int size) {
        this.host = host;
        this.members = members;
        this.location = location;
        this.size = size;
    }

    @SneakyThrows(IOException.class)
    public static Island read(IslandManager host, InputStream stream) {
        return builder()
                .host(host)
                .members(Members.read(stream))
                .location(IOUtils.readXYZ(stream, host.getWorld()))
                .size(IOUtils.readInt(stream))
                .build();
    }

    public Task<Double, Double> calculateLevel() {
        World world = location.getWorld();

        ValueMap valueMap = host.getHost().getSkyConfig().getValueMap();
        HashMap<BlockData, Double> localMap = new HashMap<>();

        Task<Double, Double> task = Task.of((Task<Double, Double>.Delegate delegate) -> {
            AtomicLong counter = new AtomicLong(0);
            AtomicDouble level = new AtomicDouble(0);

            int minX = location.getBlockX() - size;
            int maxX = location.getBlockX() + size;
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            int minZ = location.getBlockZ() - size;
            int maxZ = location.getBlockZ() + size;

            long max = (maxX - minX) * (maxY - minY) * (maxZ - minZ);

            double processors = Runtime.getRuntime().availableProcessors();
            int heightBlockSize = (int) Math.ceil((maxY - minY) / processors);

            ArrayList<CompletableFuture<Void>> blockTasks = new ArrayList<>();
            for (int i = 0; i < processors; i++) {
                int cMinY = i * heightBlockSize;
                int cMaxY = Math.min((i + 1) * heightBlockSize, maxY);

                Task<Void, Void> blockTask = Task.of((Task<Void, Void>.Delegate del) -> {
                    double local = 0;

                    for (int x = minX; x <= maxX; x++) {
                        for (int y = cMinY; y <= cMaxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                delegate.setProgress(counter.incrementAndGet() / (double) max);
                                BlockData bd = world.getBlockAt(x, y, z).getBlockData();
                                localMap.putIfAbsent(bd, valueMap.valueOf(bd));
                                local += localMap.get(bd);
                            }
                        }
                    }
                    level.addAndGet(local);
                    return null;
                });

                blockTasks.add(blockTask.runAsync(Skyblock.LEVEL_EXECUTOR));
            }
            CompletableFuture.allOf(blockTasks.toArray(CompletableFuture[]::new)).join();
            return level.get();
        });
        task.runAsync(Skyblock.LEVEL_EXECUTOR);
        return task;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean onIsland(SkyPlayer player) {
        if (player.hasPlayer()) {
            return onIsland(player.getPlayer().getLocation());
        } else {
            return false;
        }
    }

    private void onEnter(SkyPlayer skyPlayer) {
        skyPlayer.with(player -> {
            Mediator.getNMS().setBorder(player, location, size*2);
        });
    }


    private void onExit(SkyPlayer skyPlayer) {
        skyPlayer.with(player -> {
            Mediator.getNMS().resetBorder(player);
        });
    }

    public void update(ArrayList<Player> players) {
        for (Player player : players) {
            SkyPlayer skyPlayer = host.getHost().getPlayerManager().wrap(player);
            if (!pastVisitors.contains(player)) {
                onEnter(skyPlayer);
            }
        }
        for (Player pastVisitor : pastVisitors) {
            if (players.contains(pastVisitor)) continue;
            onExit(host.getHost().getPlayerManager().wrap(pastVisitor));
        }
        pastVisitors.clear();
        pastVisitors.addAll(players);
    }

    public boolean onIsland(Location loc) {
        if (loc.getWorld() != host.getWorld()) return false;
        int minX = location.getBlockX() - size;
        int maxX = location.getBlockX() + size;
        int minZ = location.getBlockZ() - size;
        int maxZ = location.getBlockZ() + size;

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }


    public Members getMembers() {
        return members;
    }

    public Location getLocation() {
        return location;
    }

    @SneakyThrows(IOException.class)
    public int write(OutputStream stream) {
        int size = 0;
        size += members.write(stream);
        size += IOUtils.writeXYZ(stream, location);
        size += IOUtils.writeInt(stream, size);

        return size;
    }
}

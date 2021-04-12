package io.github.trianmc.skyblock.islands;

import com.boydti.fawe.util.EditSessionBuilder;
import com.google.common.util.concurrent.AtomicDouble;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import io.github.bluelhf.anemone.gui.ViewContext;
import io.github.bluelhf.anemone.util.Items;
import io.github.bluelhf.tasks.Task;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.config.Lang;
import io.github.trianmc.skyblock.members.Members;
import io.github.trianmc.skyblock.members.Rights;
import io.github.trianmc.skyblock.nms.Mediator;
import io.github.trianmc.skyblock.util.IOUtils;
import io.github.trianmc.skyblock.util.PlayerUtils;
import io.github.trianmc.skyblock.util.math.MathUtils;
import io.github.trianmc.skyblock.util.math.QuickBlockData;
import io.github.trianmc.skyblock.util.math.ValueMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.trianmc.skyblock.util.sfx.Components.unstyle;
import static java.lang.StrictMath.floorDiv;
import static java.lang.StrictMath.floorMod;


@SuppressWarnings("DuplicatedCode")
public class Island {
    private final IslandManager host;
    private final ArrayList<Player> pastVisitors = new ArrayList<>();
    private Members members;
    private Location location;
    private int size;

    private double levelCache = -1;
    private Task<Double, Double> calculationTask = null;

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

    public ItemStack getIcon(int num, ViewContext context) {
        Lang lang = host.getHost().getLang();
        UUID uuid = context.getViewer().getUniqueId();
        if (levelCache == -1) {
            calculateLevel().onResult(level -> context.update());
        }
        return Items.of(Material.GRASS_BLOCK)
                .modifyMeta(meta ->
                        meta.displayName(unstyle(lang.get("gui.island_icon.text", num + 1))))
                .modifyMeta(meta -> meta.lore(lang.getAll("gui.island_icon.lore",
                        size,
                        GsonComponentSerializer.gson().serialize(unstyle(
                                        levelCache != -1
                                                ? Component.text(MathUtils.compact(levelCache)).color(NamedTextColor.YELLOW)
                                                : lang.get("generic.none"))),
                        GsonComponentSerializer.gson().serialize(
                                unstyle(members.getRights(uuid).forLang(lang))),

                        members.hasRights(uuid, Rights.OWNER) ? ""
                                : unstyle(Rights.OWNER.forLang(lang)) + ": " + PlayerUtils.getName(members.getOwner()))
                ))
                .build();
    }

    public ArrayList<Player> getVisitorCache() {
        return pastVisitors;
    }

    public long getVolume() {
        World world = location.getWorld();
        int minX = location.getBlockX() - size;
        int maxX = location.getBlockX() + size;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int minZ = location.getBlockZ() - size;
        int maxZ = location.getBlockZ() + size;
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    public Location getCorner(int c) {
        int delta = (int) (Math.signum(c) * size);
        return location.clone().add(delta, delta, delta);
    }

    public Task<Double, Double> calculateLevel() {
        if (calculationTask != null) return calculationTask;
        ValueMap valueMap = host.getHost().getSkyConfig().getValueMap();
        Task<Double, Double> task = Task.of((Task<Double, Double>.Delegate delegate) -> {
            AtomicDouble total = new AtomicDouble();

            World world = location.getWorld();
            int minX = location.getBlockX() - size;
            int maxX = location.getBlockX() + size;
            int minY = world.getMinHeight();
            int minZ = location.getBlockZ() - size;
            int maxZ = location.getBlockZ() + size;

            int minCX = floorDiv(minX, 16);
            int minCZ = floorDiv(minZ, 16);
            int maxCX = floorDiv(maxX, 16);
            int maxCZ = floorDiv(maxZ, 16);

            int minXC = floorMod(minX, 16);
            int maxXC = floorMod(maxX, 16);
            int minZC = floorMod(minZ, 16);
            int maxZC = floorMod(maxZ, 16);

            double max = Math.abs((maxCX - minCX) * (maxCZ - minCZ));
            AtomicLong counter = new AtomicLong();
            ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int cx = minCX; cx < maxCX; cx++) {
                for (int cz = minCZ; cz < maxCZ; cz++) {
                    int finalCx = cx;
                    int finalCz = cz;
                    Bukkit.getScheduler().runTask(host.getHost(), () -> world.addPluginChunkTicket(finalCx, finalCz, host.getHost()));
                    futures.add(world.getChunkAtAsync(cx, cz).thenAcceptAsync(chunk -> {
                        try {
                            ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                            int sX = snapshot.getX();
                            int sZ = snapshot.getZ();

                            int lmx = sX == minCX ? minXC : 0;
                            int mmx = sX == maxCX ? maxXC + 1 : 15;
                            int lmz = sZ == minCZ ? minZC : 0;
                            int mmz = sZ == maxCZ ? maxZC + 1 : 15;


                            for (int x = lmx; x <= mmx; x++) {
                                for (int z = lmz; z <= mmz; z++) {
                                    int maxY = snapshot.getHighestBlockYAt(x, z);
                                    for (int y = minY; y < maxY; y++) {
                                        QuickBlockData data = new QuickBlockData(snapshot.getBlockData(x, y, z));
                                        if (data.bd.getMaterial() == Material.AIR) continue;
                                        total.addAndGet(valueMap.getDouble(data));
                                    }
                                }
                            }
                            delegate.setProgress(counter.getAndIncrement() / max);
                            Bukkit.getScheduler().runTask(host.getHost(), () -> world.removePluginChunkTicket(finalCx, finalCz, host.getHost()));

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }));
                }
            }

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            levelCache = total.get();
            return total.get();
        });
        calculationTask = task;
        calculationTask.onResult((result) -> calculationTask = null);
        task.runAsync(Skyblock.LEVEL_EXECUTOR);
        return task;
    }

    public CompletableFuture<Void> clear() {
        Region region = new CuboidRegion(
                BukkitAdapter.adapt(getCorner(-1)).toBlockPoint(),
                BukkitAdapter.adapt(getCorner(1)).toBlockPoint()
        );
        return CompletableFuture.runAsync(() -> {
            try (EditSession session = new EditSessionBuilder(BukkitAdapter.adapt(location.getWorld()))
                    .fastmode(true)
                    .build()) {
                session.disableHistory();

                BlockData bd = Bukkit.createBlockData(Material.AIR);
                BlockState type = BukkitAdapter.adapt(bd);
                session.replaceBlocks(region, Masks.negate(type.toMask()), type);
                session.flushQueue();
            }
        });
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }


    public boolean onIsland(Player player) {
        return onIsland(player.getLocation());
    }

    private void onEnter(Player player) {
        Mediator.getNMS().setBorder(player, location, size * 2);
    }


    private void onExit(Player player) {
        Mediator.getNMS().resetBorder(player);
    }

    public void update(ArrayList<Player> players) {
        for (Player player : players) {
            if (!pastVisitors.contains(player)) {
                onEnter(player);
            }
        }
        for (Player pastVisitor : pastVisitors) {
            if (players.contains(pastVisitor)) continue;
            onExit(pastVisitor);
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
        size += IOUtils.writeInt(stream, this.size);

        return size;
    }

    public double getChunkCount() {
        int minX = location.getBlockX() - size;
        int maxX = location.getBlockX() + size;
        int minZ = location.getBlockZ() - size;
        int maxZ = location.getBlockZ() + size;

        int minCX = floorDiv(minX, 16);
        int minCZ = floorDiv(minZ, 16);
        int maxCX = floorDiv(maxX, 16);
        int maxCZ = floorDiv(maxZ, 16);

        return Math.abs((maxCX - minCX) * (maxCZ - minCZ));
    }
}

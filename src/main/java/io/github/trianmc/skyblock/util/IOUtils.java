package io.github.trianmc.skyblock.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;

public class IOUtils {

    public static OutputStream write(Path path) throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(path.toFile()));
    }

    public static OutputStreamWriter writer(Path path) throws FileNotFoundException {
        return new OutputStreamWriter(write(path));
    }

    public static InputStream read(Path path) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(path.toFile()));
    }

    public static InputStreamReader reader(Path path) throws FileNotFoundException {
        return new InputStreamReader(read(path));
    }

    public static int writeUUID(OutputStream stream, UUID uuid) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 2);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        stream.write(buf.array());
        return Long.BYTES * 2;
    }

    public static UUID readUUID(InputStream stream) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(stream.readNBytes(Long.BYTES * 2));
        return new UUID(buf.getLong(), buf.getLong());
    }

    public static int writeInt(OutputStream stream, int num) throws IOException {
        stream.write(ByteBuffer.allocate(Integer.BYTES).putInt(num).array());
        return Integer.BYTES;
    }

    public static int readInt(InputStream stream) throws IOException {
        return ByteBuffer.wrap(stream.readNBytes(Integer.BYTES)).getInt();
    }

    public static int writeDouble(OutputStream stream, double num) throws IOException {
        stream.write(ByteBuffer.allocate(Double.BYTES).putDouble(num).array());
        return Double.BYTES;
    }

    public static double readDouble(InputStream stream) throws IOException {
        return ByteBuffer.wrap(stream.readNBytes(Double.BYTES)).getDouble();
    }

    public static int writeXYZ(OutputStream stream, Location location) throws IOException {
        int size = 0;
        size += writeInt(stream, location.getBlockX());
        size += writeInt(stream, location.getBlockY());
        size += writeInt(stream, location.getBlockZ());
        return size;
    }

    public static Location readXYZ(InputStream stream, World world) throws IOException {
        int x = readInt(stream),
            y = readInt(stream),
            z = readInt(stream);

        return new Location(world, x, y, z);
    }
}

package io.github.trianmc.skyblock.nms;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class Mediator {

    private static INms nms;
    static {
        switch (getNMSVersion()) {
            case "v1_16_R3":
                nms = new v1_16_R3();
        }
    }

    public static String getNMSVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    @NotNull
    public static INms getNMS() {
        if (nms == null) {
            throw new IllegalStateException("No NMS implementation available for this server version.");
        }
        return nms;
    }
}

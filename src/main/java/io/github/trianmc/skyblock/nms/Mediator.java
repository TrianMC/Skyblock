package io.github.trianmc.skyblock.nms;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class Mediator {

    private static INms nms;
    static {
        //noinspection SwitchStatementWithTooFewBranches we'll add branches later
        switch (getNMSVersion()) {
            case "v1_17_R1":
                nms = new v1_17_R1();
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

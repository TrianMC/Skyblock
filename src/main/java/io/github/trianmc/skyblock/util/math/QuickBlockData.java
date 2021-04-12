package io.github.trianmc.skyblock.util.math;

import io.github.trianmc.skyblock.nms.Mediator;
import org.bukkit.block.data.BlockData;

public class QuickBlockData {
    public final BlockData bd;
    public QuickBlockData(BlockData blockData) {
        bd = blockData;
    }

    @Override
    public int hashCode() {
        return bd.getMaterial().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickBlockData that = (QuickBlockData) o;
        return Mediator.getNMS().implicitMatch(this.bd, that.bd);
    }
}

package io.github.trianmc.skyblock.util;

import lombok.Builder;
import lombok.Singular;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Map;

@Builder
public class ValueMap {
    private final double def;
    @Singular("put")
    private final Map<BlockData, Double> valueMap;

    public double valueOf(BlockData bd) {
        return valueMap.keySet().stream().filter(other -> bd.matches(other) || other.matches(bd))
                .findFirst().map(valueMap::get).orElse(def);
    }
}

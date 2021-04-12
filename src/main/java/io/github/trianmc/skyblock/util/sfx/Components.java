package io.github.trianmc.skyblock.util.sfx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Components {
    public static Component unstyle(Component input) {
        if (input.decorations().getOrDefault(TextDecoration.ITALIC, TextDecoration.State.NOT_SET) == TextDecoration.State.NOT_SET) {
            input = input.decoration(TextDecoration.ITALIC, false);
        }
        input.style().colorIfAbsent(NamedTextColor.WHITE);
        return input;
    }
}

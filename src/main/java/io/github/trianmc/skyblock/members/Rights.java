package io.github.trianmc.skyblock.members;

import io.github.trianmc.skyblock.config.Lang;
import net.kyori.adventure.text.Component;

public enum Rights {
    OWNER,
    PEER,
    VISITOR;


    public Component forLang(Lang lang) {
        return lang.get("rights." + toString().toLowerCase());
    }
}

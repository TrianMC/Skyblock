package io.github.trianmc.skyblock.util;

import com.google.gson.JsonElement;

public class JsonUtils {
    public static boolean equals(JsonElement one, JsonElement other) {
        if (one.isJsonObject()) {
            return other.isJsonObject() && one.getAsJsonObject().equals(other.getAsJsonObject());
        } else if (one.isJsonArray()) {
            return other.isJsonArray() && one.getAsJsonArray().equals(other.getAsJsonArray());
        } else if (one.isJsonPrimitive()) {
            return other.isJsonPrimitive() && one.getAsJsonPrimitive().equals(other.getAsJsonPrimitive());
        } else return other.isJsonNull();
    }
}

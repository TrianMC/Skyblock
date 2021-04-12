package io.github.trianmc.skyblock.config;

import com.google.gson.*;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.lang.LocaleUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

public class Lang {
    private static final Gson gson = new Gson();
    private static final Pattern RAW = Pattern.compile("\\{\"raw\":[ ]*\"(.*?)\"}");
    private final JsonObject lang;

    private boolean debug = false;

    @SneakyThrows(IOException.class)
    public Lang(InputStream stream) throws JsonParseException {
        InputStreamReader reader = new InputStreamReader(stream);
        JsonObject parsed = gson.fromJson(reader, JsonObject.class);
        lang = parsed.getAsJsonObject();
        reader.close();
    }

    public void debug(boolean shouldDebug) {
        debug = shouldDebug;
    }

    public void debug() {
        debug = !debug;
    }

    public JsonObject getJson() {
        return lang;
    }

    public Component get(String path, Object... format) {
        JsonElement entry = lang.get(path);
        if (entry == null) {
            if (debug) System.err.println("No entry with path " + path + " exists.");
            return Component.text("no.lang.entry");
        }
        if (entry.isJsonObject()) {
            return get(entry.getAsJsonObject(), format);
        } else if (entry.isJsonArray()) {
            JsonArray jsonArray = entry.getAsJsonArray();
            ArrayList<Component> components = get(jsonArray, format);
            return Component.join(Component.empty(), components.toArray(ComponentLike[]::new));
        } else {
            if (debug) {
                System.err.println("Don't know how to get " + entry.getClass().getSimpleName() + "entry at " + path + " with args " + Arrays.toString(format));
            }
            return Component.text("invalid.lang.entry");
        }
    }

    private Component get(JsonObject jsonObject, Object... format) {
        String jsonStr = gson.toJson(jsonObject);
        jsonStr = treatRaw(jsonStr);
        jsonStr = String.format(jsonStr, format);

        try {
            return GsonComponentSerializer.gson().deserialize(jsonStr);
        } catch (JsonParseException exception) {
            if (debug) {
                System.err.println("Got exception while parsing " + jsonStr + " with args " + Arrays.toString(format));
                exception.printStackTrace(System.err);
            }
            return Component.text("invalid.lang.entry");
        }
    }

    private ArrayList<Component> get(JsonArray jsonArray, Object... format) {
        ArrayList<Component> components = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            if (element.isJsonArray()) {
                components.addAll(get(element.getAsJsonArray(), format));
            } else if (element.isJsonObject()) {
                components.add(get(element.getAsJsonObject(), format));
            }
        }
        return components;
    }

    private String treatRaw(String s) {
        return RAW.matcher(s).replaceAll("$1");
    }

    public List<Component> getAll(String path, Object... format) {
        JsonElement entry = lang.get(path);
        if (entry == null) return Collections.singletonList(Component.text("no.lang.entry"));
        if (entry.isJsonObject()) return Collections.singletonList(get(path, format));
        if (entry.isJsonArray()) {
            return get(entry.getAsJsonArray(), format);
        }

        return Collections.singletonList(Component.text("invalid.lang.entry"));
    }

    public String getString(String path) {
        return lang.get(path) != null ? lang.get(path).getAsString() : null;
    }

    public String getLanguageName() {
        return getString("language.name");
    }

    public String getLanguageRegion() {
        return getString("language.region");
    }

    public String getLanguageCode() {
        return getString("language.code");
    }

    public Locale getLocale() {
        return LocaleUtils.toLocale(getLanguageCode());
    }

    public boolean isDebug() {
        return debug;
    }
}

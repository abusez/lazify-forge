package com.lazify.api;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;

public class JsonWrapper {

    private final JsonElement element;

    public JsonWrapper(JsonElement element) {
        this.element = element;
    }

    public static JsonWrapper parse(String json) {
        if (json == null || json.isEmpty()) return new JsonWrapper(null);
        try {
            JsonParser parser = new JsonParser();
            return new JsonWrapper(parser.parse(json));
        } catch (JsonParseException e) {
            return new JsonWrapper(null);
        }
    }

    public boolean exists() {
        return element != null && !element.isJsonNull();
    }

    public String string() {
        if (!exists()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        return element.toString();
    }

    public JsonWrapper get(String key) {
        if (!exists() || !element.isJsonObject()) return new JsonWrapper(null);
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has(key)) return new JsonWrapper(null);
        return new JsonWrapper(obj.get(key));
    }

    public String get(String key, String def) {
        JsonWrapper w = get(key);
        if (!w.exists()) return def;
        return w.string();
    }

    public JsonWrapper object() {
        if (!exists() || !element.isJsonObject()) return new JsonWrapper(null);
        return this;
    }

    public JsonWrapper object(String key) {
        return get(key).object();
    }

    public List<JsonWrapper> array() {
        List<JsonWrapper> result = new ArrayList<>();
        if (!exists() || !element.isJsonArray()) return result;
        for (JsonElement e : element.getAsJsonArray()) {
            result.add(new JsonWrapper(e));
        }
        return result;
    }

    public List<JsonWrapper> array(String key) {
        return get(key).array();
    }

    public JsonElement getRaw() {
        return element;
    }

    public int asInt(int def) {
        if (!exists()) return def;
        try { return element.getAsInt(); } catch (Exception e) { return def; }
    }

    public long asLong(long def) {
        if (!exists()) return def;
        try { return element.getAsLong(); } catch (Exception e) { return def; }
    }

    public double asDouble(double def) {
        if (!exists()) return def;
        try { return element.getAsDouble(); } catch (Exception e) { return def; }
    }

    public boolean asBoolean(boolean def) {
        if (!exists()) return def;
        try { return element.getAsBoolean(); } catch (Exception e) { return def; }
    }
}

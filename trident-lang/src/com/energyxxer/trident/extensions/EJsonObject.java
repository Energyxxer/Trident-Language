package com.energyxxer.trident.extensions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EJsonObject {

    public static JsonElement getByPath(JsonObject thiz, String path) {
        String[] segments = path.split("\\.", 2);
        if(segments.length == 1) {
            return thiz.get(segments[0]);
        } else {
            JsonObject next = thiz.getAsJsonObject(segments[0]);
            if(next != null) {
                return getByPath(thiz, segments[1]);
            }
            return null;
        }
    }

    public static boolean getAsBoolean(JsonObject thiz, String key, boolean def) {
        JsonElement elem = thiz.get(key);
        return elem != null ? elem.getAsBoolean() : def;
    }
}
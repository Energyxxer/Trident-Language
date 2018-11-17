package Trident.extensions.com.google.gson.JsonObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

@Extension
public class EJsonObject {

    public static JsonElement getOrRun(@This JsonObject thiz, String path, Runnable action) {
        JsonElement elem = thiz.get(path);
        if(elem == null) action.run();
        return elem;
    }

    public static boolean getBooleanByPath(@This JsonObject thiz, String path) {
        JsonElement elem = thiz.getByPath(path);
        return elem != null && elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isBoolean() && elem.getAsBoolean();
    }

    public static String getStringByPath(@This JsonObject thiz, String path) {
        JsonElement elem = thiz.getByPath(path);
        return (elem != null && elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) ? elem.getAsString() : null;
    }

    public static JsonObject getJsonObjectByPath(@This JsonObject thiz, String path) {
        JsonElement elem = thiz.getByPath(path);
        return (elem != null && elem.isJsonObject()) ? elem.getAsJsonObject() : null;
    }

    public static JsonElement getByPath(@This JsonObject thiz, String path) {
        String[] segments = path.split("\\.", 2);
        if(segments.length == 1) {
            return thiz.get(segments[0]);
        } else {
            JsonObject next = thiz.getAsJsonObject(segments[0]);
            if(next != null) {
                return thiz.getByPath(segments[1]);
            }
            return null;
        }
    }
}
package Trident.extensions.com.google.gson.JsonElement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

@Extension
public class EJsonElement {
    public static String getAsStringOrRun(@This JsonElement thiz, Runnable action) {
        if(thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isString()) {
            return thiz.getAsString();
        } else {
            action.run();
            return null;
        }
    }

    public static String getAsStringOrNull(@This JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isString()) ?
                thiz.getAsString() :
                null;
    }

    public static Boolean getAsBooleanOrNull(@This JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isBoolean()) ?
                thiz.getAsBoolean() :
                null;
    }

    public static JsonObject getAsJsonObjectOrNull(@This JsonElement thiz) {
        return (thiz != null && thiz.isJsonObject()) ?
                thiz.getAsJsonObject() :
                null;
    }

    public static JsonArray getAsJsonArrayOrNull(@This JsonElement thiz) {
        return (thiz != null && thiz.isJsonArray()) ?
                thiz.getAsJsonArray() :
                null;
    }
}
package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;

import java.util.Map;
import java.util.function.Function;

public class JSON {

    public static Object parse(String raw, ISymbolContext ctx) throws MalformedJsonException {
        if(raw.isEmpty()) return null;
        JsonElement asJson = new Gson().fromJson(raw, JsonElement.class);
        if(asJson.isJsonPrimitive() && asJson.getAsJsonPrimitive().isString() && !(raw.startsWith("\"") || raw.startsWith("'"))) throw new MalformedJsonException("Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1 path $");
        return parseJson(asJson, ctx);
    }

    public static Object stringify(String obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    public static Object stringify(java.lang.Integer obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    public static Object stringify(Double obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    public static Object stringify(Boolean obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    public static Object stringify(ListObject obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    public static Object stringify(DictionaryObject obj, @NativeFunctionAnnotations.NullableArg Boolean prettyPrinting, ISymbolContext ctx) {
        return stringify((Object) obj, prettyPrinting, ctx);
    }

    private static Object stringify(Object param, Boolean prettyPrinting, ISymbolContext ctx) {
        if(prettyPrinting == null) prettyPrinting = false;
        GsonBuilder gb = new GsonBuilder().disableHtmlEscaping();
        if(prettyPrinting) gb.setPrettyPrinting();
        return gb.create().toJson(toJson(param, null, true, ctx));
    }

    private static Object parseJson(JsonElement elem, ISymbolContext ctx) {
        if(elem.isJsonPrimitive()) {
            JsonPrimitive prim = elem.getAsJsonPrimitive();
            if(prim.isString()) {
                return prim.getAsString();
            } else if(prim.isBoolean()) {
                return prim.getAsBoolean();
            } else if(prim.isNumber()) {
                Number num = prim.getAsNumber();
                if(num.intValue() == num.doubleValue()) {
                    return num.intValue();
                } else return num.doubleValue();
            } else {
                throw new IllegalArgumentException("Unknown primitive type: " + elem);
            }
        } else if(elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());
            for(Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                dict.put(entry.getKey(), parseJson(entry.getValue(), ctx));
            }
            return dict;
        } else if(elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            ListObject list = new ListObject(ctx.getTypeSystem());
            for(JsonElement entry : arr) {
                list.add(parseJson(entry, ctx));
            }
            return list;
        } else throw new IllegalArgumentException("Unknown json type: " + elem);
    }

    @DefaultLibraryProvider.HideFromCustomClass
    public static JsonElement toJson(Object obj, Function<Object, JsonElement> filter, boolean skipUnknownTypes, ISymbolContext ctx) {
        if(obj instanceof String || obj instanceof ResourceLocation || obj instanceof TagCompound) return new JsonPrimitive(obj.toString());
        if(obj instanceof Number) return new JsonPrimitive(((Number) obj));
        if(obj instanceof Boolean) return new JsonPrimitive((Boolean) obj);
        if(obj instanceof ListObject) {
            JsonArray arr = new JsonArray();
            for(Object elem : ((ListObject) obj)) {
                JsonElement result = toJson(elem, filter, skipUnknownTypes, ctx);
                if(result != null) arr.add(result);
            }
            return arr;
        }
        if(obj instanceof DictionaryObject) {
            JsonObject jObj = new JsonObject();
            for(Map.Entry<String, Symbol> elem : ((DictionaryObject) obj).entrySet()) {
                JsonElement result = toJson(elem.getValue().getValue(null, null), filter, skipUnknownTypes, ctx);
                if(result != null) jObj.add(elem.getKey(), result);
            }
            return jObj;
        }
        JsonElement applied = filter != null ? filter.apply(obj) : null;
        if(applied == null && !skipUnknownTypes) throw new IllegalArgumentException("Cannot convert object of type '" + ctx.getTypeSystem().getTypeIdentifierForObject(obj) + "' to a JSON element");
        return applied;
    }

    @DefaultLibraryProvider.HideFromCustomClass
    public static class WrapperJsonElement<T> extends JsonElement {
        private T wrapped;
        private Class<T> cls;

        public WrapperJsonElement(T wrapped, Class<T> cls) {
            this.wrapped = wrapped;
            this.cls = cls;
        }

        @Override
        public JsonElement deepCopy() {
            return new WrapperJsonElement<>(wrapped, cls);
        }

        @Override
        public String toString() {
            return String.valueOf(wrapped);
        }

        @Override
        public boolean isJsonPrimitive() {
            return true;
        }

        @Override
        public JsonPrimitive getAsJsonPrimitive() {
            return new JsonPrimitive(String.valueOf(wrapped));
        }

        @Override
        public String getAsString() {
            return String.valueOf(wrapped);
        }

        public T getWrapped() {
            return wrapped;
        }

        public Class<T> getContentClass() {
            return cls;
        }
    }
}

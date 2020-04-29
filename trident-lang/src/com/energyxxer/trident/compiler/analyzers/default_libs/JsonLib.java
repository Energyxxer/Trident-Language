package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;

import java.util.Map;
import java.util.function.Function;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeMethodBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "JSON")
public class JsonLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass jsonLib = new CustomClass("JSON", "trident-util:native", globalCtx);
        jsonLib.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("JSON", Symbol.SymbolVisibility.GLOBAL, jsonLib));

        try {
            jsonLib.putStaticFunction(nativeMethodsToFunction(jsonLib.getInnerContext(), JsonLib.class.getMethod("parse", String.class)));
            jsonLib.putStaticFunction(nativeMethodsToFunction(
                    jsonLib.getInnerContext(),
                    JsonLib.class.getMethod("stringify", String.class),
                    JsonLib.class.getMethod("stringify", Boolean.class),
                    JsonLib.class.getMethod("stringify", Integer.class),
                    JsonLib.class.getMethod("stringify", Double.class),
                    JsonLib.class.getMethod("stringify", ListObject.class),
                    JsonLib.class.getMethod("stringify", DictionaryObject.class),
                    JsonLib.class.getMethod("stringify", String.class, boolean.class),
                    JsonLib.class.getMethod("stringify", Boolean.class, boolean.class),
                    JsonLib.class.getMethod("stringify", Integer.class, boolean.class),
                    JsonLib.class.getMethod("stringify", Double.class, boolean.class),
                    JsonLib.class.getMethod("stringify", ListObject.class, boolean.class),
                    JsonLib.class.getMethod("stringify", DictionaryObject.class, boolean.class)
            ));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object parse(String raw) throws MalformedJsonException {
        if(raw.isEmpty()) return null;
        JsonElement asJson = new Gson().fromJson(raw, JsonElement.class);
        if(asJson.isJsonPrimitive() && asJson.getAsJsonPrimitive().isString() && !(raw.startsWith("\"") || raw.startsWith("'"))) throw new MalformedJsonException("Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1 path $");
        return parseJson(asJson);
    }

    public static Object stringify(String obj) {
        return stringify(obj, false);
    }

    public static Object stringify(String obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(Integer obj) {
        return stringify(obj, false);
    }

    public static Object stringify(Integer obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(Double obj) {
        return stringify(obj, false);
    }

    public static Object stringify(Double obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(Boolean obj) {
        return stringify(obj, false);
    }

    public static Object stringify(Boolean obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(ListObject obj) {
        return stringify(obj, false);
    }

    public static Object stringify(ListObject obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(DictionaryObject obj) {
        return stringify(obj, false);
    }

    public static Object stringify(DictionaryObject obj, boolean prettyPrinting) {
        return stringify((Object) obj, prettyPrinting);
    }

    public static Object stringify(Object param, boolean prettyPrinting) {
        GsonBuilder gb = new GsonBuilder();
        if(prettyPrinting) gb.setPrettyPrinting();
        return gb.create().toJson(toJson(param, null, true));
    }

    public static Object parseJson(JsonElement elem) {
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
            DictionaryObject dict = new DictionaryObject();
            for(Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                dict.put(entry.getKey(), parseJson(entry.getValue()));
            }
            return dict;
        } else if(elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            ListObject list = new ListObject();
            for(JsonElement entry : arr) {
                list.add(parseJson(entry));
            }
            return list;
        } else throw new IllegalArgumentException("Unknown json type: " + elem);
    }

    public static JsonElement toJson(Object obj) {
        return toJson(obj, null, false);
    }

    public static JsonElement toJson(Object obj, Function<Object, JsonElement> filter, boolean skipUnknownTypes) {
        if(obj instanceof String || obj instanceof TridentUtil.ResourceLocation || obj instanceof TagCompound) return new JsonPrimitive(obj.toString());
        if(obj instanceof Number) return new JsonPrimitive(((Number) obj));
        if(obj instanceof Boolean) return new JsonPrimitive((Boolean) obj);
        if(obj instanceof ListObject) {
            JsonArray arr = new JsonArray();
            for(Object elem : ((ListObject) obj)) {
                JsonElement result = toJson(elem, filter, skipUnknownTypes);
                if(result != null) arr.add(result);
            }
            return arr;
        }
        if(obj instanceof DictionaryObject) {
            JsonObject jObj = new JsonObject();
            for(Map.Entry<String, Symbol> elem : ((DictionaryObject) obj).entrySet()) {
                JsonElement result = toJson(elem.getValue().getValue(), filter, skipUnknownTypes);
                if(result != null) jObj.add(elem.getKey(), result);
            }
            return jObj;
        }
        JsonElement applied = filter != null ? filter.apply(obj) : null;
        if(applied == null && !skipUnknownTypes) throw new IllegalArgumentException("Cannot convert object of type '" + TridentTypeManager.getTypeIdentifierForObject(obj) + "' to a JSON element");
        return applied;
    }

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

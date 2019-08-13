package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.google.gson.*;

import java.util.Map;
import java.util.function.Function;

@AnalyzerMember(key = "JSON")
public class JsonLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        DictionaryObject block = new DictionaryObject();

        block.put("parse",
                new MethodWrapper<>("parse", ((instance, params) -> parseJson(new Gson().fromJson((String) params[0], JsonElement.class))), String.class).createForInstance(null));
        block.put("stringify",
                (VariableMethod) (params, patterns, pattern, ctx) -> {
                    if(params.length < 1) {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'stringify' requires 1 parameter, instead found " + params.length, pattern, ctx);
                    }
                    boolean prettyPrinting = false;

                    if(params.length >= 2) {
                        prettyPrinting = VariableMethod.HelperMethods.assertOfType(params[1], patterns[1], ctx, Boolean.class);
                    }

                    Object param = VariableMethod.HelperMethods.assertOfType(params[0], patterns[0], ctx, String.class, Number.class, Boolean.class, ListObject.class, DictionaryObject.class);

                    GsonBuilder gb = new GsonBuilder();
                    if(prettyPrinting) gb.setPrettyPrinting();

                    try {
                        return gb.create().toJson(toJson(param, null, true));
                    } catch(IllegalArgumentException x) {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.getMessage(), pattern, ctx);
                    }
                });
        globalCtx.put(new Symbol("JSON", Symbol.SymbolVisibility.GLOBAL, block));
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
        if(obj instanceof String || obj instanceof TridentUtil.ResourceLocation) return new JsonPrimitive(obj.toString());
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
        if(applied == null && !skipUnknownTypes) throw new IllegalArgumentException("Cannot convert object of type '" + VariableTypeHandler.Static.getShorthandForObject(obj) + "' to a JSON element");
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

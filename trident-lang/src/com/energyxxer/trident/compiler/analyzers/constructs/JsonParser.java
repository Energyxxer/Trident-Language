package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.default_libs.JsonLib;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;

public class JsonParser {

    private static HashMap<JsonElement, TokenPattern<?>> patternCache = new HashMap<>();

    public static JsonElement parseJson(TokenPattern<?> pattern, ISymbolContext ctx) {
        while (true) {
            TokenList entries;
            switch (pattern.getName()) {
                case "INTERPOLATION_BLOCK": {
                    Object object = InterpolationManager.parse(pattern, ctx, Double.class, Integer.class, Boolean.class, String.class);
                    if (object instanceof Double) return new JsonPrimitive((Double) object);
                    if (object instanceof Integer) return new JsonPrimitive((Integer) object);
                    if (object instanceof Boolean) return new JsonPrimitive((Boolean) object);
                    if (object instanceof String) return new JsonPrimitive((String) object);
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
                }
                case "JSON_ROOT":
                case "JSON_ELEMENT":
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                case "JSON_OBJECT":
                    JsonObject object = new JsonObject();

                    entries = (TokenList) pattern.find("JSON_OBJECT_ENTRIES");
                    if (entries != null) {
                        for (TokenPattern<?> entry : entries.getContents()) {
                            if (!entry.getName().equals("COMMA")) {
                                String key = CommonParsers.parseStringLiteral(entry.find("JSON_OBJECT_KEY.STRING"), ctx);
                                JsonElement value = parseJson(entry.find("JSON_ELEMENT"), ctx);
                                object.add(key, value);
                            }
                        }
                    }
                    patternCache.put(object, pattern);
                    return object;
                case "JSON_ARRAY":
                    JsonArray arr = new JsonArray();

                    entries = (TokenList) pattern.find("JSON_ARRAY_ENTRIES");
                    if (entries != null) {
                        for (TokenPattern<?> entry : entries.getContents()) {
                            if (!entry.getName().equals("COMMA")) {
                                arr.add(parseJson(entry, ctx));
                            }
                        }
                    }
                    patternCache.put(arr, pattern);
                    return arr;
                case "STRING":
                    JsonPrimitive string = new JsonPrimitive(CommonParsers.parseStringLiteral(pattern, ctx));
                    patternCache.put(string, pattern);
                    return string;
                case "NUMBER":
                    JsonPrimitive number = new JsonPrimitive(CommonParsers.parseDouble(pattern, ctx));
                    patternCache.put(number, pattern);
                    return number;
                case "JSON_NUMBER":
                    JsonElement jsonNumber = new CustomJSONNumber(pattern.flatten(false));
                    patternCache.put(jsonNumber, pattern);
                    return jsonNumber;
                case "BOOLEAN":
                    JsonPrimitive bool = new JsonPrimitive(pattern.flattenTokens().get(0).value.equals("true"));
                    patternCache.put(bool, pattern);
                    return bool;
                default:
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown json element production name: '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static void clearCache() {
        patternCache.clear();
    }

    public static TokenPattern<?> getPatternFor(JsonElement elem) {
        return patternCache.get(elem);
    }

    /**
     * JsonParser must not be instantiated.
     */
    private JsonParser() {
    }

    public static class CustomJSONNumber extends JsonLib.WrapperJsonElement<String> {

        public CustomJSONNumber(String wrapped) {
            super(wrapped, String.class);
        }
    }
}

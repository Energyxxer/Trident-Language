package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;

public class JsonParser {

    private static HashMap<JsonElement, TokenPattern<?>> patternCache = new HashMap<>();

    public static JsonElement parseJson(TokenPattern<?> pattern, TridentFile file) {
        TokenList entries;
        switch(pattern.getName()) {
            case "JSON_ROOT":
            case "JSON_ELEMENT":
                return parseJson(((TokenStructure) pattern).getContents(), file);
            case "JSON_OBJECT":
                JsonObject object = new JsonObject();

                entries = (TokenList) pattern.find("JSON_OBJECT_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> entry : entries.getContents()) {
                        if (!entry.getName().equals("COMMA")) {
                            String key = CommonParsers.parseStringLiteral(entry.find("JSON_OBJECT_KEY.STRING"), file);
                            JsonElement value = parseJson(entry.find("JSON_ELEMENT"), file);
                            object.add(key, value);
                        }
                    }
                }
                patternCache.put(object, pattern);
                return object;
            case "JSON_ARRAY":
                JsonArray arr = new JsonArray();

                entries = (TokenList) pattern.find("JSON_ARRAY_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> entry : entries.getContents()) {
                        if (!entry.getName().equals("COMMA")) {
                            arr.add(parseJson(entry, file));
                        }
                    }
                }
                patternCache.put(arr, pattern);
                return arr;
            case "STRING":
                JsonPrimitive string = new JsonPrimitive(CommonParsers.parseStringLiteral(pattern, file));
                patternCache.put(string, pattern);
                return string;
            case "NUMBER":
                JsonPrimitive number = new JsonPrimitive(Double.parseDouble(pattern.flattenTokens().get(0).value));
                patternCache.put(number, pattern);
                return number;
            case "BOOLEAN":
                JsonPrimitive bool = new JsonPrimitive(pattern.flattenTokens().get(0).value.equals("true"));
                patternCache.put(bool, pattern);
                return bool;
            default:
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown json element production name: '" + pattern.getName() + "'", pattern, file);
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
}

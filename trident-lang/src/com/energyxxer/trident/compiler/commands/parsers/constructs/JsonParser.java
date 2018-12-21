package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;

public class JsonParser {

    private static HashMap<JsonElement, TokenPattern<?>> patternCache = new HashMap<>();

    public static JsonElement parseJson(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenList entries;
        switch(pattern.getName()) {
            case "JSON_ROOT":
            case "JSON_ELEMENT":
                return parseJson(((TokenStructure) pattern).getContents(), compiler);
            case "JSON_OBJECT":
                JsonObject object = new JsonObject();

                entries = (TokenList) pattern.find("JSON_OBJECT_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> entry : entries.getContents()) {
                        if (!entry.getName().equals("COMMA")) {
                            String key = CommandUtils.parseQuotedString(entry.find("JSON_OBJECT_KEY").flattenTokens().get(0).value);
                            JsonElement value = parseJson(entry.find("JSON_ELEMENT"), compiler);
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
                            arr.add(parseJson(entry, compiler));
                        }
                    }
                }
                patternCache.put(arr, pattern);
                return arr;
            case "STRING_LITERAL":
                JsonPrimitive string = new JsonPrimitive(CommandUtils.parseQuotedString(pattern.flattenTokens().get(0).value));
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
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown json element production name: '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
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

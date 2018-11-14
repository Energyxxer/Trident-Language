package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.EntryParsingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonParser {

    public static JsonElement parseJson(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenList entries;
        switch(pattern.getName()) {
            case "JSON_ELEMENT":
                return parseJson(((TokenStructure) pattern).getContents(), compiler);
            case "JSON_OBJECT":
                JsonObject object = new JsonObject();

                entries = (TokenList) pattern.find("JSON_OBJECT_ENTRIES");
                for(TokenPattern<?> entry : entries.getContents()) {
                    if(!entry.getName().equals("COMMA")) {
                        String key = CommandUtils.parseQuotedString(entry.find("JSON_OBJECT_KEY").flattenTokens().get(0).value);
                        JsonElement value = parseJson(entry.find("JSON_ELEMENT"), compiler);
                        object.add(key, value);
                    }
                }
                return object;
            case "JSON_ARRAY":
                JsonArray arr = new JsonArray();

                entries = (TokenList) pattern.find("JSON_ARRAY_ENTRIES");
                for(TokenPattern<?> entry : entries.getContents()) {
                    if(!entry.getName().equals("COMMA")) {
                        arr.add(parseJson(entry, compiler));
                    }
                }
                return arr;
            case "STRING_LITERAL":
                return new JsonPrimitive(CommandUtils.parseQuotedString(pattern.flattenTokens().get(0).value));
            case "NUMBER":
                return new JsonPrimitive(Double.parseDouble(pattern.flattenTokens().get(0).value));
            case "BOOLEAN":
                return new JsonPrimitive(pattern.flattenTokens().get(0).value.equals("true"));
            default:
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown json element production name: '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
        }
    }

    /**
     * JsonParser must not be instantiated.
     */
    private JsonParser() {
    }
}

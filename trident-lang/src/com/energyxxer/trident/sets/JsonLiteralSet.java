package com.energyxxer.trident.sets;

import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.JSON_NUMBER;

public class JsonLiteralSet extends PatternProviderSet {
    public JsonLiteralSet() {
        super(null);
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {
        TokenStructureMatch JSON_ROOT = productions.getOrCreateStructure("JSON_ROOT");
        TokenStructureMatch JSON_ELEMENT = productions.getOrCreateStructure("JSON_ELEMENT");

        {
            TokenGroupMatch g = new TokenGroupMatch().setName("JSON_OBJECT");
            g.append(TridentProductions.brace("{"));
            {
                TokenGroupMatch g2 = new TokenGroupMatch();
                g2.append(wrapper(TridentProductions.string(productions)).setName("JSON_OBJECT_KEY"));
                g2.append(TridentProductions.colon());
                g2.append(JSON_ELEMENT);
                g.append(new TokenListMatch(g2, TridentProductions.comma(), true).setName("JSON_OBJECT_ENTRIES"));
            }
            g.append(TridentProductions.brace("}"));
            g.setEvaluator((p, d) -> parseJsonObject(p, (ISymbolContext) d[0]));
            JSON_ELEMENT.add(g);
            JSON_ROOT.add(g);
        }
        {
            TokenGroupMatch g = new TokenGroupMatch().setName("JSON_ARRAY");
            g.append(TridentProductions.brace("["));
            g.append(new TokenListMatch(JSON_ELEMENT, TridentProductions.comma(), true).setName("JSON_ARRAY_ENTRIES"));
            g.append(TridentProductions.brace("]"));
            g.setEvaluator((p, d) -> parseJsonArray(p, (ISymbolContext) d[0]));
            JSON_ELEMENT.add(g);
            JSON_ROOT.add(g);
        }
        JSON_ELEMENT.add(group(TridentProductions.string(productions)).setEvaluator((p, d) -> new JsonPrimitive((String) ((TokenGroup)p).getContents()[0].evaluate(d))));
        JSON_ROOT.add(group(TridentProductions.string(productions)).setEvaluator((p, d) -> new JsonPrimitive((String) ((TokenGroup)p).getContents()[0].evaluate(d))));
        JSON_ELEMENT.add(group(TridentProductions.real(productions)).setEvaluator((p, d) -> new JsonPrimitive((double) ((TokenGroup)p).getContents()[0].evaluate(d))));
        JSON_ELEMENT.add(PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), d -> new Object[] {d[0]}, (v, p, d) -> parseJsonInterpolationBlock(v, p, (ISymbolContext) d[0]), false, Double.class, Integer.class, Boolean.class, String.class));
        JSON_ELEMENT.add(group(TridentProductions.rawBoolean()).setEvaluator((p, d) -> new JsonPrimitive((boolean) ((TokenGroup)p).getContents()[0].evaluate(d))));
        JSON_ELEMENT.add(ofType(JSON_NUMBER).setName("JSON_NUMBER").setEvaluator((p, d) -> new CustomJSONNumber(p.flatten(false))));
    }


    private static HashMap<JsonElement, TokenPattern<?>> patternCache = new HashMap<>();

    public static JsonElement parseJsonInterpolationBlock(Object object, TokenPattern<?> pattern, ISymbolContext ctx) {
        if (object instanceof Double) return new JsonPrimitive((Double) object);
        if (object instanceof Integer) return new JsonPrimitive((Integer) object);
        if (object instanceof Boolean) return new JsonPrimitive((Boolean) object);
        if (object instanceof String) return new JsonPrimitive((String) object);
        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
    }

    public static JsonObject parseJsonObject(TokenPattern<?> pattern, ISymbolContext ctx) {
        JsonObject object = new JsonObject();

        TokenList entries = (TokenList) pattern.find("JSON_OBJECT_ENTRIES");
        if (entries != null) {
            for (TokenPattern<?> entry : entries.getContentsExcludingSeparators()) {
                String key = (String) entry.find("JSON_OBJECT_KEY").evaluate(ctx);
                JsonElement value = (JsonElement) entry.find("JSON_ELEMENT").evaluate(ctx);
                object.add(key, value);
            }
        }
        patternCache.put(object, pattern);
        return object;
    }

    public static JsonArray parseJsonArray(TokenPattern<?> pattern, ISymbolContext ctx) {
        JsonArray arr = new JsonArray();

        TokenList entries = (TokenList) pattern.find("JSON_ARRAY_ENTRIES");
        if (entries != null) {
            for (TokenPattern<?> entry : entries.getContents()) {
                if (!entry.getName().equals("COMMA")) {
                    arr.add((JsonElement) entry.evaluate(ctx));
                }
            }
        }
        patternCache.put(arr, pattern);
        return arr;
    }

    public static void clearCache() {
        patternCache.clear();
    }

    public static TokenPattern<?> getPatternFor(JsonElement elem) {
        return patternCache.get(elem);
    }

    public static class CustomJSONNumber extends com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection.JSON.WrapperJsonElement<String> {

        public CustomJSONNumber(String wrapped) {
            super(wrapped, String.class);
        }
    }
}

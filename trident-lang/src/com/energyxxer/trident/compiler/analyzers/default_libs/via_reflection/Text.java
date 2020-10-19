package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Text {
    private static Gson gson = new Gson();

    public static Object parse(String raw, ISymbolContext ctx, TokenPattern pattern) {
        return TextParser.jsonToTextComponent(gson.fromJson(raw, JsonElement.class), ctx, pattern, TextComponentContext.CHAT);
    }
}

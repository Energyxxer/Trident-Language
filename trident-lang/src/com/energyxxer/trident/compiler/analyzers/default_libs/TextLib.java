package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "Text")
public class TextLib implements DefaultLibraryProvider {
    private static Gson gson = new Gson();

    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass tlib = new CustomClass("Text", "trident-util:native", globalCtx);
        tlib.setConstructor(Symbol.SymbolVisibility.PRIVATE, null);
        globalCtx.put(new Symbol("Text", Symbol.SymbolVisibility.GLOBAL, tlib));

        try {
            tlib.putStaticFunction(nativeMethodsToFunction(tlib.getInnerStaticContext(), TextLib.class.getMethod("parse", String.class, ISymbolContext.class, TokenPattern.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object parse(String raw, ISymbolContext ctx, TokenPattern pattern) {
        return TextParser.parseTextComponent(gson.fromJson(raw, JsonElement.class), ctx, pattern, TextComponentContext.CHAT);
    }
}

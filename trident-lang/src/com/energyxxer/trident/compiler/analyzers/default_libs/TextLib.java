package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "Text")
public class TextLib implements DefaultLibraryProvider {
    private Gson gson = new Gson();

    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject tlib = new DictionaryObject();
        tlib.put("parse", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 1) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'parse' requires 1 parameter, instead found " + params.length, pattern, file);
            }

            String raw = assertOfType(params[0], patterns[0], file, String.class);

            try {
                return TextParser.parseTextComponent(gson.fromJson(raw, JsonElement.class), file, patterns[0], TextComponentContext.CHAT);
            } catch(TridentException | TridentException.Grouped x) {
                throw x;
            } catch(Exception x) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.toString(), pattern, file);
            }
        });
        stack.getGlobal().put(new Symbol("Text", Symbol.SymbolAccess.GLOBAL, tlib));
    }
}

package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "Text")
public class TextLib implements DefaultLibraryPopulator {
    private Gson gson = new Gson();

    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject tlib = new DictionaryObject();
        tlib.put("parse", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 1) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'parse' requires 1 parameter, instead found " + params.length, pattern));
                throw new EntryParsingException();
            }

            String raw = assertOfType(params[0], patterns[0], file, String.class);

            try {
                return TextParser.parseTextComponent(gson.fromJson(raw, JsonElement.class), file, patterns[0], TextComponentContext.CHAT);
            } catch(Exception x) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.toString(), pattern));
                throw new EntryParsingException();
            }
        });
        stack.getGlobal().put(new Symbol("Text", Symbol.SymbolAccess.GLOBAL, tlib));
    }
}

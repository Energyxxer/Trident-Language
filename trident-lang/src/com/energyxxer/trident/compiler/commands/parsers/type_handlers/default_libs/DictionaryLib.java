package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.FunctionMethod;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

import java.util.Map;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "Dictionary")
public class DictionaryLib implements DefaultLibraryProvider {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject dictLib = new DictionaryObject();

        dictLib.put("map", (VariableMethod) (params, patterns, pattern1, file1) -> {
            if(params.length < 2) {
                file1.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'map' requires at least 2 parameters, instead found " + params.length, pattern1));
                throw new EntryParsingException();
            }
            DictionaryObject dict = assertOfType(params[0], patterns[0], file1, DictionaryObject.class);
            FunctionMethod func = assertOfType(params[1], patterns[1], file1, FunctionMethod.class);

            DictionaryObject newDict = new DictionaryObject();

            for(Map.Entry<String, Symbol> entry : dict.entrySet()) {
                newDict.put(entry.getKey(), func.call(new Object[] {entry.getKey(), entry.getValue().getValue()}, new TokenPattern[] {pattern1, pattern1}, pattern1, file1));
            }

            return newDict;
        });
        stack.getGlobal().put(new Symbol("Dictionary", Symbol.SymbolAccess.GLOBAL, dictLib));
    }
}

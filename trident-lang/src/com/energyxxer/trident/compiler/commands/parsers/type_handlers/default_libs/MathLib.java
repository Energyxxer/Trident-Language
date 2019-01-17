package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "Math")
public class MathLib implements DefaultLibraryProvider {
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject math = new DictionaryObject();
        math.put("pow", (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 2) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'pow' requires 2 parameters, instead found " + params.length, pattern));
                throw new EntryParsingException();
            }

            Number base = assertOfType(params[0], patterns[0], file, Double.class, Integer.class);
            Number exponent = assertOfType(params[1], patterns[1], file, Double.class, Integer.class);

            double result = Math.pow(base.doubleValue(), exponent.doubleValue());

            if(params[0] instanceof Double || params[1] instanceof Double) return result;
            else return (int) result;
        });
        stack.getGlobal().put(new Symbol("Math", Symbol.SymbolAccess.GLOBAL, math));
    }
}

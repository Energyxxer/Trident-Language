package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        SymbolTable table = file.getCompiler().getStack().peek();
        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false));
        table.put(symbol);

        Object value = CommonParsers.parseAnything((TokenPattern<?>) ((pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE")).getContents()), file.getCompiler());

        symbol.setValue(value);
    }
}

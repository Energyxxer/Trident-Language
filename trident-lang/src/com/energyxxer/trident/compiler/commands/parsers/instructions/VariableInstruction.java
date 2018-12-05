package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

@ParserMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        Debug.log(pattern);
        SymbolTable table = file.getCompiler().getStack().peek();
        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false));
        table.put(symbol);

        Object value = CommonParsers.parseAnything(((TokenStructure) pattern.find("VARIABLE_VALUE")).getContents(), file.getCompiler());

        symbol.setValue(value);
    }
}

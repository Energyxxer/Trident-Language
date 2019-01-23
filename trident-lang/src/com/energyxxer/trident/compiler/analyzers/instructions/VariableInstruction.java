package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        SymbolTable table = file.getCompiler().getSymbolStack().peek();
        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false));

        Object value = CommonParsers.parseAnything((TokenPattern<?>) ((pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE")).getContents()), file);

        table.put(symbol);
        symbol.setValue(value);
    }
}

package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);
        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false), visibility);

        Object value = CommonParsers.parseAnything((TokenPattern<?>) ((pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE")).getContents()), ctx);

        ctx.putInContextForVisibility(visibility, symbol);
        symbol.setValue(value);
    }
}

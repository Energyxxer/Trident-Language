package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

@AnalyzerMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);
        String symbolName = pattern.find("VARIABLE_NAME").flatten(false);
        Symbol symbol = new Symbol(symbolName, visibility);

        VariableTypeHandler typeConstraint = InterpolationManager.parseType(pattern.find("VARIABLE_INITIALIZATION.VARIABLE_TYPE_CONSTRAINTS.INTERPOLATION_TYPE"), ctx);
        if(typeConstraint != null) Debug.log(typeConstraint);
        symbol.setTypeConstraint(typeConstraint);

        InterpolationManager.setNextFunctionName(symbolName);
        Object value = CommonParsers.parseAnything((TokenPattern<?>) ((pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE")).getContents()), ctx);
        InterpolationManager.setNextFunctionName(null);

        ctx.putInContextForVisibility(visibility, symbol);
        symbol.setValue(value);
    }
}

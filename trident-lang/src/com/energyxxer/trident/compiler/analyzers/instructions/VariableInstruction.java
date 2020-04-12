package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "var")
public class VariableInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);
        String symbolName = pattern.find("VARIABLE_NAME").flatten(false);
        Symbol symbol = new Symbol(symbolName, visibility);

        boolean inferConstraints = false;
        if(pattern.find("VARIABLE_TYPE_CONSTRAINTS") != null) {
            inferConstraints = pattern.find("VARIABLE_TYPE_CONSTRAINTS.VARIABLE_TYPE_CONSTRAINTS_INNER") == null;
            if(!inferConstraints) {
                symbol.setTypeConstraint(
                        InterpolationManager.parseType(pattern.find("VARIABLE_TYPE_CONSTRAINTS.VARIABLE_TYPE_CONSTRAINTS_INNER.INTERPOLATION_TYPE"), ctx),
                        pattern.find("VARIABLE_TYPE_CONSTRAINTS.VARIABLE_TYPE_CONSTRAINTS_INNER.VARIABLE_NULLABLE") != null
                );
            }
        }

        Object value = null;
        boolean initialized = false;

        if(pattern.find("VARIABLE_INITIALIZATION") != null) {
            InterpolationManager.setNextFunctionName(symbolName);
            value = CommonParsers.parseAnything((TokenPattern<?>) ((pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE")).getContents()), ctx);
            InterpolationManager.setNextFunctionName(null);
            initialized = true;
        }
        
        symbol.safeSetValue(value, initialized ? pattern.find("VARIABLE_INITIALIZATION.VARIABLE_VALUE") : pattern, ctx);

        ctx.putInContextForVisibility(visibility, symbol);
        if(inferConstraints) {
            symbol.setTypeConstraint(TridentTypeManager.getHandlerForObject(symbol.getValue()), true);
        }
    }
}

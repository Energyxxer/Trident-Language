package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListType;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "try")
public class TryInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern.find("LITERAL_RECOVERING") != null) ctx.getCompiler().getTryStack().pushRecovering();
        else ctx.getCompiler().getTryStack().pushBreaking();

        Object variable = null;

        try {
            IfInstruction.resolveBlock(pattern.find("EXECUTION_BLOCK"), ctx);
        } catch(TridentException x) { //blocking
            variable = x;
        } catch(TridentException.Grouped gx) {
            variable = new ListType(gx.getExceptions());
        } finally {
            ctx.getCompiler().getTryStack().pop();
        }

        if(variable != null) {
            String variableName = pattern.find("CATCH_CLAUSE.EXCEPTION_VARIABLE").flatten(false);
            IfInstruction.resolveBlock(pattern.find("CATCH_CLAUSE.EXECUTION_BLOCK"), ctx, new Symbol(variableName, Symbol.SymbolVisibility.LOCAL, variable));
        }
    }
}

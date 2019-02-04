package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.extensions.EObject;

@AnalyzerMember(key = "if")
public class IfInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        boolean condition = InterpolationManager.parse(pattern.find("CONDITION.INTERPOLATION_VALUE"), ctx, Boolean.class);
        EObject.assertNotNull(condition, pattern, ctx);

        if(condition) {
            resolveBlock(pattern.find("EXECUTION_BLOCK"), ctx);
        } else if(pattern.find("ELSE_CLAUSE") != null) {
            resolveBlock(pattern.find("ELSE_CLAUSE.EXECUTION_BLOCK"), ctx);
        }
    }

    public static void resolveBlock(TokenPattern<?> pattern, ISymbolContext ctx, Symbol... defaultSymbols) {
        if(pattern.getName().equals("EXECUTION_BLOCK")) {
            resolveBlock(((TokenStructure) pattern).getContents(), ctx, defaultSymbols);
            return;
        }

        SymbolContext innerScope = new SymbolContext(ctx);
        if(defaultSymbols != null) {
            for(Symbol sym : defaultSymbols) {
                innerScope.put(sym);
            }
        }
        if(pattern.getName().equals("ANONYMOUS_INNER_FUNCTION")) {
            TridentFile.resolveInnerFileIntoSection(pattern, innerScope, ctx.getWritingFile().getFunction());
        } else {
            TridentFile.resolveEntry(((TokenStructure) pattern).getContents(), innerScope, ctx.getWritingFile().getFunction(), false);
        }
    }
}

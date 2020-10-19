package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class IfInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenStructureMatch executionBlock = choice(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION"), productions.getOrCreateStructure("ENTRY")).setName("EXECUTION_BLOCK");
        return group(TridentProductions.instructionKeyword("do").setOptional(), TridentProductions.instructionKeyword("if"), TridentProductions.brace("("), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_VALUE"), false, Boolean.class).setName("CONDITION").addTags("cspn:Condition"), TridentProductions.brace(")"), executionBlock, optional(TridentProductions.instructionKeyword("else", false), executionBlock).setName("ELSE_CLAUSE")).setName("IF_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        boolean condition = (boolean) pattern.find("CONDITION").evaluate(ctx);
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
            TridentFile.resolveInnerFileIntoSection(pattern, innerScope, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
        } else {
            TridentFile.resolveEntry(((TokenStructure) pattern).getContents(), innerScope, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction(), false);
        }
    }
}

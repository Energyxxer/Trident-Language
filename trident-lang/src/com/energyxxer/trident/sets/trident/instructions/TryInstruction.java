package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.*;

public class TryInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenStructureMatch executionBlock = choice(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION"), productions.getOrCreateStructure("ENTRY")).setName("EXECUTION_BLOCK");
        return group(
                instructionKeyword("try"),
                literal("recovering").setOptional(),
                executionBlock,
                group(
                        instructionKeyword("catch").addTags(SuggestionTags.ENABLED),
                        brace("("),
                        identifierX().setName("EXCEPTION_VARIABLE").addTags("cspn:Exception Variable Name").addProcessor((p, l) -> {
                            productions.getProviderSet(ValueAccessExpressionSet.class).addPreBlockDeclaration(p);
                        }),
                        brace(")"),
                        executionBlock
                ).setName("CATCH_CLAUSE")
        ).setName("TRY_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern.find("LITERAL_RECOVERING") != null) ctx.getCompiler().getTryStack().pushRecovering();
        else ctx.getCompiler().getTryStack().pushBreaking();

        Object variable = null;

        try {
            IfInstruction.resolveBlock(pattern.find("EXECUTION_BLOCK"), ctx);
        } catch(PrismarineException x) {
            variable = x;
        } catch(PrismarineException.Grouped gx) {
            variable = new ListObject(ctx.getTypeSystem(), gx.getExceptions());
        } finally {
            ctx.getCompiler().getTryStack().pop();
        }

        if(variable != null) {
            String variableName = pattern.find("CATCH_CLAUSE.EXCEPTION_VARIABLE").flatten(false);
            IfInstruction.resolveBlock(pattern.find("CATCH_CLAUSE.EXECUTION_BLOCK"), ctx, new Symbol(variableName, TridentSymbolVisibility.LOCAL, variable));
        }
    }
}

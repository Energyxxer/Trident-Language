package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.controlflow.ReturnException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ReturnInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(TridentProductions.instructionKeyword("return"), wrapperOptional(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("RETURN_VALUE").addTags("cspn:Return Value")).setName("RETURN_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ReturnException(pattern, pattern.findThenEvaluate("RETURN_VALUE", null, ctx));
    }
}

package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.instructionKeyword;

public class EvalInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(instructionKeyword("eval"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"));
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        pattern.find("INTERPOLATION_VALUE").evaluate(ctx, null);
    }
}

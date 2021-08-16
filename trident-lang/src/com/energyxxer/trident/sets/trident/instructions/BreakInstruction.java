package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.BreakException;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.instructionKeyword;

public class BreakInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(instructionKeyword("break"), TridentProductions.identifierX().setName("BREAK_LABEL").setOptional()).setName("BREAK_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String label = null;
        if(pattern.find("BREAK_LABEL") != null) {
            label =  pattern.find("BREAK_LABEL").flatten(false);
        }
        throw new BreakException(label, pattern);
    }
}

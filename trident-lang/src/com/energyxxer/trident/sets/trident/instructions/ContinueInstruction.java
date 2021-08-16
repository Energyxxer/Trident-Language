package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.ContinueException;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.instructionKeyword;

public class ContinueInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(instructionKeyword("continue"), TridentProductions.identifierX().setName("CONTINUE_LABEL").setOptional()).setName("CONTINUE_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String label = null;
        if(pattern.find("CONTINUE_LABEL") != null) {
            label =  pattern.find("CONTINUE_LABEL").flatten(false);
        }
        throw new ContinueException(label, pattern);
    }
}

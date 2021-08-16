package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.group;

public class ThrowInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(TridentProductions.instructionKeyword("throw"), TridentProductions.noToken().addTags("cspn:Cause"), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), false, String.class, PrismarineException.class).setName("CAUSE")).setName("THROW_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        Object exception = pattern.find("CAUSE").evaluate(ctx);
        if(exception instanceof PrismarineException) {
            throw (PrismarineException) exception;
        } else {
            throw new PrismarineException(TridentExceptionUtil.Source.USER_EXCEPTION, (String) exception, pattern, ctx).setBreaking(true);
        }
    }
}

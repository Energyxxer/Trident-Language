package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ThrowInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(TridentProductions.instructionKeyword("throw"), TridentProductions.noToken().addTags("cspn:Cause"), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), false, String.class).setName("CAUSE")).setName("THROW_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = (String) pattern.find("CAUSE").evaluate(ctx);
        throw new PrismarineException(TridentExceptionUtil.Source.USER_EXCEPTION, message, pattern, ctx).setBreaking(true);
    }
}

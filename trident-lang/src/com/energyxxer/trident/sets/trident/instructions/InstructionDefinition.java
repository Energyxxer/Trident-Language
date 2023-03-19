package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.prismarine.providers.PatternProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public interface InstructionDefinition extends PatternProviderUnit<ISymbolContext> {

    void run(TokenPattern<?> pattern, ISymbolContext ctx);

    @Override
    default Object evaluate(TokenPattern<?> tokenPattern, ISymbolContext ctx, Object[] data) {
        run(tokenPattern, ctx);
        return null;
    }
}

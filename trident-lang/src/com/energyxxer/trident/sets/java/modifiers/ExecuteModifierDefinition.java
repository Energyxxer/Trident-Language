package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.Collection;

public interface ExecuteModifierDefinition extends PatternSwitchProviderUnit<ISymbolContext> {
    Collection<ExecuteModifier> parse(TokenPattern<?> pattern, ISymbolContext ctx);

    @Override
    default Object evaluate(TokenPattern<?> tokenPattern, ISymbolContext ctx, Object[] data) {
        return parse(tokenPattern, ctx);
    }
}

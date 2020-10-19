package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.Collection;
import java.util.Collections;

public interface SimpleExecuteModifierDefinition extends ExecuteModifierDefinition {
    ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx);

    @Override
    default Collection<ExecuteModifier> parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return Collections.singletonList(parseSingle(pattern, ctx));
    }
}

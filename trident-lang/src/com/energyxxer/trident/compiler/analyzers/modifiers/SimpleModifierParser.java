package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Collections;

public interface SimpleModifierParser extends ModifierParser {
    ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx);

    @Override
    default Collection<ExecuteModifier> parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return Collections.singletonList(parseSingle(pattern, ctx));
    }
}

package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public interface ContextualToString {
    String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx);
}

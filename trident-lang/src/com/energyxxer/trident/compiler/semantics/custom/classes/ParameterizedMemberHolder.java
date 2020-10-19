package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

public interface ParameterizedMemberHolder {
    Object getMemberForParameters(String memberName, TokenPattern<?> pattern, ActualParameterList params, ISymbolContext ctx, boolean keepSymbol);
}

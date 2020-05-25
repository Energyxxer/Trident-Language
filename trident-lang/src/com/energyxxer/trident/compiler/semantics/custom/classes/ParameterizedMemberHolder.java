package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public interface ParameterizedMemberHolder {
    Object getMemberForParameters(String memberName, TokenPattern<?> pattern, ActualParameterList params, ISymbolContext ctx, boolean keepSymbol);
}

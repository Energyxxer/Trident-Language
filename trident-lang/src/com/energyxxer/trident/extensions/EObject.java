package com.energyxxer.trident.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;

public class EObject {
    public static void assertNotNull(Object thiz, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(thiz == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Unexpected null value", pattern, ctx);
        }
    }
}
package com.energyxxer.trident.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class EObject {
    public static void assertNotNull(Object thiz, TokenPattern<?> pattern, TridentFile file) {
        if(thiz == null) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Unexpected null value", pattern, file);
        }
    }
}
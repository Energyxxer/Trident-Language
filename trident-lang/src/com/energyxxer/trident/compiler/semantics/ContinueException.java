package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public class ContinueException extends RuntimeException {
    private String label;

    private TokenPattern<?> pattern;

    public ContinueException(TokenPattern<?> pattern) {
        this(null, pattern);
    }

    public ContinueException(String label, TokenPattern<?> pattern) {
        this.label = label;
        this.pattern = pattern;
    }

    public String getLabel() {
        return label;
    }

    public TokenPattern<?> getPattern() {
        return pattern;
    }
}

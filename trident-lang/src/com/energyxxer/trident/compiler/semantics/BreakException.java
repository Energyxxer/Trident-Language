package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public class BreakException extends RuntimeException {
    private String label;

    private TokenPattern<?> pattern;

    public BreakException(TokenPattern<?> pattern) {
        this(null, pattern);
    }

    public BreakException(String label, TokenPattern<?> pattern) {
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

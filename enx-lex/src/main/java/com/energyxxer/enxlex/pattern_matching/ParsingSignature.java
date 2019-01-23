package com.energyxxer.enxlex.pattern_matching;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public class ParsingSignature {
    private int hashCode;
    private TokenPattern<?> pattern;

    public ParsingSignature(int hashCode, TokenPattern<?> pattern) {
        this.hashCode = hashCode;
        this.pattern = pattern;
    }

    public int getHashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public TokenPattern<?> getPattern() {
        return pattern;
    }

    public void setPattern(TokenPattern<?> pattern) {
        this.pattern = pattern;
    }
}

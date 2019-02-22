package com.energyxxer.enxlex.suggestions;

import java.util.Objects;

public class LiteralSuggestion extends Suggestion {
    private String literal;

    public LiteralSuggestion(String literal) {
        this.literal = literal;
    }

    @Override
    public String toString() {
        return "literal(" + literal + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiteralSuggestion that = (LiteralSuggestion) o;
        return literal.equals(that.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literal);
    }

    public String getLiteral() {
        return literal;
    }
}

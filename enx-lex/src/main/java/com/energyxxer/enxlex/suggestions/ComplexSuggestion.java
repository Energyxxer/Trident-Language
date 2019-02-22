package com.energyxxer.enxlex.suggestions;

import java.util.Objects;

public class ComplexSuggestion extends Suggestion {
    private String key;

    public ComplexSuggestion(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "{" + key + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexSuggestion that = (ComplexSuggestion) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    public String getKey() {
        return key;
    }
}

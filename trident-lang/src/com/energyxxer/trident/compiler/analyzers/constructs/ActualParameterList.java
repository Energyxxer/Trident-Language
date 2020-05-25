package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActualParameterList {
    private @NotNull List<Object> values;
    private @NotNull List<TokenPattern<?>> patterns;
    private @NotNull TokenPattern<?> pattern;

    public ActualParameterList(@NotNull List<Object> values, @NotNull List<TokenPattern<?>> patterns, @NotNull TokenPattern<?> pattern) {
        this.values = values;
        this.patterns = patterns;
        this.pattern = pattern;

        if(values.size() != patterns.size()) {
            throw new IllegalArgumentException("Mismatching list lengths");
        }
    }

    @NotNull
    public List<Object> getValues() {
        return values;
    }

    @NotNull
    public List<TokenPattern<?>> getPatterns() {
        return patterns;
    }

    @NotNull
    public TokenPattern<?> getPattern() {
        return pattern;
    }

    public int size() {
        return values.size();
    }
}

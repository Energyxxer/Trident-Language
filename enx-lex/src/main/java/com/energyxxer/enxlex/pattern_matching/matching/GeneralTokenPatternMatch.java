package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class GeneralTokenPatternMatch {
    public String name = "";
    public boolean optional;
    public List<String> tags = new ArrayList<>();
    protected List<BiConsumer<TokenPattern<?>, Lexer>> processors = new ArrayList<>();
    protected List<BiConsumer<Integer, Lexer>> failProcessors = new ArrayList<>();

    public GeneralTokenPatternMatch addTags(String... newTags) {
        tags.addAll(Arrays.asList(newTags));
        return this;
    }

    public GeneralTokenPatternMatch setName(String name) {
        this.name = name;
        return this;
    }

    public GeneralTokenPatternMatch addProcessor(BiConsumer<TokenPattern<?>, Lexer> processor) {
        processors.add(processor);
        return this;
    }

    public GeneralTokenPatternMatch addFailProcessor(BiConsumer<Integer, Lexer> failProcessor) {
        failProcessors.add(failProcessor);
        return this;
    }

    public abstract String deepToString(int levels);

    public abstract String toTrimmedString();

    protected void invokeProcessors(TokenPattern<?> pattern, Lexer lexer) {
        processors.forEach(p -> p.accept(pattern, lexer));
    }

    protected void invokeFailProcessors(int matchLength, Lexer lexer) {
        failProcessors.forEach(p -> p.accept(matchLength, lexer));
    }
}

package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.ComplexSuggestion;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.util.Stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class TokenPatternMatch {
    public String name = "";
    public boolean optional;
    public List<String> tags = new ArrayList<>();
    protected List<BiConsumer<TokenPattern<?>, Lexer>> processors = new ArrayList<>();
    protected List<BiConsumer<Integer, Lexer>> failProcessors = new ArrayList<>();

    public TokenPatternMatch addTags(String... newTags) {
        tags.addAll(Arrays.asList(newTags));
        return this;
    }

    public TokenPatternMatch setName(String name) {
        this.name = name;
        return this;
    }

    public TokenPatternMatch addProcessor(BiConsumer<TokenPattern<?>, Lexer> processor) {
        processors.add(processor);
        return this;
    }

    public TokenPatternMatch addFailProcessor(BiConsumer<Integer, Lexer> failProcessor) {
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





    public TokenMatchResponse match(int index, Lexer lexer) {
        return match(index, lexer, new Stack());
    }

    public abstract TokenMatchResponse match(int index, Lexer lexer, Stack st);

    public TokenPatternMatch setOptional() {
        return setOptional(true);
    }

    public TokenPatternMatch setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    protected int handleSuggestionTags(Lexer lexer, int index) {
        int popSuggestionStatus = 0;
        if(lexer.getSuggestionModule() != null) {

            if(tags.contains(SuggestionTags.ENABLED)) {
                lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.ENABLED);
                popSuggestionStatus++;
            } else if(tags.contains(SuggestionTags.DISABLED)) {
                lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.DISABLED);
                popSuggestionStatus++;
            }

            if(lexer.getSuggestionModule().isAtSuggestionIndex(index) && lexer.getSuggestionModule().getCaretIndex() == lexer.getSuggestionModule().getSuggestionIndex()) {
                if(tags.contains(SuggestionTags.ENABLED_INDEX)) {
                    lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.ENABLED);
                    popSuggestionStatus++;
                } else if(tags.contains(SuggestionTags.DISABLED_INDEX)) {
                    lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.DISABLED);
                    popSuggestionStatus++;
                }
            }

            if(lexer.getSuggestionModule().isAtSuggestionIndex(index)) {
                ComplexSuggestion complexSuggestion = null;
                for(String tag : tags) {
                    if((tag.startsWith("csk:") && lexer.getSuggestionModule().shouldSuggest()) || tag.startsWith("ctx:") || tag.startsWith("cspn:")) {
                        lexer.getSuggestionModule().addSuggestion(complexSuggestion = new ComplexSuggestion(tag));
                    } else if((tag.startsWith("cst:") || tag.startsWith("mst:")) && complexSuggestion != null && lexer.getSuggestionModule().shouldSuggest()) {
                        complexSuggestion.addTag(tag);
                    }
                }
            }
        }
        return popSuggestionStatus;
    }
}

package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.ComplexSuggestion;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.util.Stack;

import java.util.function.BiConsumer;

public abstract class LazyTokenPatternMatch extends GeneralTokenPatternMatch {

    public TokenMatchResponse match(int index, LazyLexer lexer) {
        return match(index, lexer, new Stack());
    }

    public abstract TokenMatchResponse match(int index, LazyLexer lexer, Stack st);

    @Override
    public LazyTokenPatternMatch addTags(String... newTags) {
        super.addTags(newTags);
        return this;
    }

    @Override
    public LazyTokenPatternMatch setName(String name) {
        super.setName(name);
        return this;
    }

    public LazyTokenPatternMatch setOptional() {
        return setOptional(true);
    }

    public LazyTokenPatternMatch setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    @Override
    public LazyTokenPatternMatch addProcessor(BiConsumer<TokenPattern<?>, Lexer> processor) {
        super.addProcessor(processor);
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

            if(lexer.getSuggestionModule().isAtFocusedIndex(index)) {
                if(tags.contains(SuggestionTags.ENABLED_INDEX)) {
                    lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.ENABLED);
                    popSuggestionStatus++;
                } else if(tags.contains(SuggestionTags.DISABLED_INDEX)) {
                    lexer.getSuggestionModule().pushStatus(SuggestionModule.SuggestionStatus.DISABLED);
                    popSuggestionStatus++;
                }
            }

            if(lexer.getSuggestionModule().isAtFocusedIndex(index) && lexer.getSuggestionModule().shouldSuggest()) {
                ComplexSuggestion complexSuggestion = null;
                for(String tag : tags) {
                    if(tag.startsWith("csk:")) {
                        lexer.getSuggestionModule().addSuggestion(complexSuggestion = new ComplexSuggestion(tag));
                    } else if(tag.startsWith("cst:") && complexSuggestion != null) {
                        complexSuggestion.addTag(tag);
                    }
                }
            }
        }
        return popSuggestionStatus;
    }
}

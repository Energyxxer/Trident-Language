package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
import com.energyxxer.enxlex.suggestions.ComplexSuggestion;
import com.energyxxer.enxlex.suggestions.LiteralSuggestion;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

public class LazyTokenItemMatch extends LazyTokenPatternMatch {
    private TokenType type;
    private String stringMatch = null;

    public LazyTokenItemMatch(TokenType type) {
        this.type = type;
        this.optional = false;
    }

    public LazyTokenItemMatch(TokenType type, String stringMatch) {
        this.type = type;
        this.stringMatch = stringMatch;
        this.optional = false;
    }

    public LazyTokenItemMatch(TokenType type, boolean optional) {
        this.type = type;
        this.optional = optional;
    }

    public LazyTokenItemMatch(TokenType type, String stringMatch, boolean optional) {
        this.type = type;
        this.stringMatch = stringMatch;
        this.optional = optional;
    }

    @Override
    public LazyTokenItemMatch setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, LazyLexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {index});

        if(st.find(thisInvoc)) {
            invokeFailProcessors(0, lexer);
            return new TokenMatchResponse(false, null, 0, this, null);
        }
        st.push(thisInvoc);

        int popSuggestionStatus = handleSuggestionTags(lexer, index);

        boolean matched;
        Token faultyToken = null;

        if(lexer.getSuggestionModule() != null && lexer.getSuggestionModule().shouldSuggest() && lexer.getSuggestionModule().isAtSuggestionIndex(index)) {
            if(this.stringMatch != null) {
                LiteralSuggestion suggestion = new LiteralSuggestion(this.stringMatch);
                for(String tag : this.tags) {
                    if(tag.startsWith("cst:") || tag.startsWith("mst:")) {
                        suggestion.addTag(tag);
                    }
                }
                lexer.getSuggestionModule().addSuggestion(suggestion);
            } else {
                lexer.getSuggestionModule().addSuggestion(new ComplexSuggestion(this.type.toString()));
            }
        }

        Token retrieved = lexer.retrieveTokenOfType(this.type);
        if(retrieved == null) {
            st.pop();
            invokeFailProcessors(0, lexer);
            return new TokenMatchResponse(false, lexer.retrieveAnyToken(), 0, this, null);
        }

        matched = stringMatch == null || retrieved.value.equals(stringMatch);

        if (!matched) {
            faultyToken = retrieved;
        }

        int length = (matched) ? retrieved.loc.index + retrieved.value.length() - index : 0;

        TokenItem item = new TokenItem(retrieved).setName(this.name).addTags(this.tags);

        st.pop();
        while(--popSuggestionStatus >= 0) {
            lexer.getSuggestionModule().popStatus();
        }
        if(matched) invokeProcessors(item, lexer);
        else invokeFailProcessors(0, lexer);
        return new TokenMatchResponse(matched, faultyToken, length, (matched) ? null : this, item);
    }

    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        String s = "";
        if (optional) {
            s += "[";
        } else {
            s += "<";
        }
        s += type;
        if (stringMatch != null) {
            s += ":" + stringMatch;
        }
        if (optional) {
            s += "]";
        } else {
            s += ">";
        }
        return s;
    }

    @Override
    public String deepToString(int levels) {
        return toString();
    }

    @Override
    public String toTrimmedString() {
        return (stringMatch != null) ? "'" + stringMatch + "'" : type.getHumanReadableName();
    }
}

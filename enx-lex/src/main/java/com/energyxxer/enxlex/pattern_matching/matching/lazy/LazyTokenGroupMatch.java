package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.suggestions.ComplexSuggestion;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.ArrayList;

import static com.energyxxer.enxlex.pattern_matching.TokenMatchResponse.*;

public class LazyTokenGroupMatch extends LazyTokenPatternMatch {
    public ArrayList<LazyTokenPatternMatch> items;

    public LazyTokenGroupMatch() {
        this.optional = false;
        items = new ArrayList<>();
    }

    public LazyTokenGroupMatch(boolean optional) {
        this.optional = optional;
        items = new ArrayList<>();
    }

    @Override
    public LazyTokenGroupMatch setName(String name) {
        super.setName(name);
        return this;
    }

    public LazyTokenGroupMatch append(LazyTokenPatternMatch i) {
        items.add(i);
        return this; //I'm so sorry for this
    }


    @Override
    public TokenMatchResponse match(int index, LazyLexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        if(items.size() == 0) return new TokenMatchResponse(true, null, 0, null, new TokenGroup());

        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {index});

        st.push(thisInvoc);

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
                for(String tag : tags) {
                    if(tag.startsWith("csk:")) {
                        lexer.getSuggestionModule().addSuggestion(new ComplexSuggestion(tag.substring("csk:".length())));
                    }
                }
            }
        }

        TokenGroup group = new TokenGroup().setName(this.name).addTags(this.tags);
        int currentIndex = index;
        boolean hasMatched = true;
        Token faultyToken = null;
        int length = 0;
        GeneralTokenPatternMatch expected = null;
        itemLoop: for (int i = 0; i < items.size(); i++) {

            if (currentIndex > lexer.getFileLength() && !items.get(i).optional) {
                hasMatched = false;
                expected = items.get(i);
                break;
            }

            TokenMatchResponse itemMatch = items.get(i).match(currentIndex, lexer, st);
            switch(itemMatch.getMatchType()) {
                case NO_MATCH: {
                    if(!items.get(i).optional) {
                        hasMatched = false;
                        faultyToken = itemMatch.faultyToken;
                        expected = itemMatch.expected;
                        break itemLoop;
                    }
                    break;
                }
                case PARTIAL_MATCH: {
                    if(!(items.get(i).optional && i+1 < items.size() && items.get(i+1).match(currentIndex, lexer, st).matched)) {
                        hasMatched = false;
                        length += itemMatch.length;
                        faultyToken = itemMatch.faultyToken;
                        expected = itemMatch.expected;
                        break itemLoop;
                    } else {
                        break;
                    }
                }
                case COMPLETE_MATCH: {
                    if(itemMatch.pattern != null) group.add(itemMatch.pattern);
                    currentIndex += itemMatch.length;
                    length += itemMatch.length;
                }
            }
        }
        st.pop();
        while(--popSuggestionStatus >= 0) {
            lexer.getSuggestionModule().popStatus();
        }
        return new TokenMatchResponse(hasMatched, faultyToken, length, expected, group);
    }

    @Override
    public String toString() {
        String s = "";
        if (this.optional) {
            s += "[";
        } else {
            s += "<";
        }
        for (int i = 0; i < items.size(); i++) {
            s += items.get(i);
            if (i < items.size() - 1) {
                s += " ";
            }
        }
        if (this.optional) {
            s += "]";
        } else {
            s += ">";
        }
        return s;
    }

    @Override
    public String deepToString(int levels) {
        if(levels <= 0) return toString();
        String s = "";
        if (this.optional) {
            s += "[";
        } else {
            s += "<";
        }
        for (int i = 0; i < items.size(); i++) {
            s += items.get(i).deepToString(levels-1);
            if (i < items.size() - 1) {
                s += " ";
            }
        }
        if (this.optional) {
            s += "]";
        } else {
            s += ">";
        }
        return s;
    }

    @Override
    public String toTrimmedString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i).toTrimmedString());
            if (i < items.size() - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}

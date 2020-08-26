package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.ArrayList;

import static com.energyxxer.enxlex.pattern_matching.TokenMatchResponse.*;

public class TokenGroupMatch extends TokenPatternMatch {
    public ArrayList<TokenPatternMatch> items;

    public TokenGroupMatch() {
        this.optional = false;
        items = new ArrayList<>();
    }

    public TokenGroupMatch(boolean optional) {
        this.optional = optional;
        items = new ArrayList<>();
    }

    @Override
    public TokenGroupMatch setName(String name) {
        super.setName(name);
        return this;
    }

    public TokenGroupMatch append(TokenPatternMatch i) {
        items.add(i);
        return this;
    }


    @Override
    public TokenMatchResponse match(int index, Lexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        if(items.size() == 0) return new TokenMatchResponse(true, null, 0, null, new TokenGroup(this));

        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {index});

        st.push(thisInvoc);

        int popSuggestionStatus = handleSuggestionTags(lexer, index);

        TokenGroup group = new TokenGroup(this).setName(this.name).addTags(this.tags);
        int currentIndex = index;
        boolean hasMatched = true;
        Token faultyToken = null;
        int length = 0;
        TokenPatternMatch expected = null;
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
        TokenMatchResponse response = new TokenMatchResponse(hasMatched, faultyToken, length, expected, group);
        if(hasMatched) {
            invokeProcessors(group, lexer);
        } else {
            invokeFailProcessors(group, lexer);
        }
        return response;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (this.optional) {
            s.append("[");
        } else {
            s.append("<");
        }
        for (int i = 0; i < items.size(); i++) {
            s.append(items.get(i));
            if (i < items.size() - 1) {
                s.append(" ");
            }
        }
        if (this.optional) {
            s.append("]");
        } else {
            s.append(">");
        }
        return s.toString();
    }

    @Override
    public String deepToString(int levels) {
        if(levels <= 0) return toString();
        StringBuilder s = new StringBuilder();
        if (this.optional) {
            s.append("[");
        } else {
            s.append("<");
        }
        for (int i = 0; i < items.size(); i++) {
            s.append(items.get(i).deepToString(levels - 1));
            if (i < items.size() - 1) {
                s.append(" ");
            }
        }
        if (this.optional) {
            s.append("]");
        } else {
            s.append(">");
        }
        return s.toString();
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

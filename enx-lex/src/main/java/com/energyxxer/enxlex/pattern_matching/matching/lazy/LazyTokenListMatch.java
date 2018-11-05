package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import static com.energyxxer.enxlex.pattern_matching.TokenMatchResponse.*;

public class LazyTokenListMatch extends LazyTokenPatternMatch {
    protected LazyTokenPatternMatch pattern;
    protected LazyTokenPatternMatch separator = null;

    public LazyTokenListMatch(TokenType type) {
        this.pattern = new LazyTokenItemMatch(type);
        this.optional = false;
    }

    public LazyTokenListMatch(TokenType type, TokenType separator) {
        this.pattern = new LazyTokenItemMatch(type);
        this.optional = false;
        this.separator = new LazyTokenItemMatch(separator);
    }

    public LazyTokenListMatch(TokenType type, boolean optional) {
        this.pattern = new LazyTokenItemMatch(type);
        this.optional = optional;
    }

    public LazyTokenListMatch(TokenType type, TokenType separator, boolean optional) {
        this.pattern = new LazyTokenItemMatch(type);
        this.optional = optional;
        this.separator = new LazyTokenItemMatch(separator);
    }

    public LazyTokenListMatch(LazyTokenPatternMatch type) {
        this.pattern = type;
        this.optional = false;
    }

    public LazyTokenListMatch(LazyTokenPatternMatch type, LazyTokenPatternMatch separator) {
        this.pattern = type;
        this.optional = false;
        this.separator = separator;
    }

    public LazyTokenListMatch(LazyTokenPatternMatch type, boolean optional) {
        this.pattern = type;
        this.optional = optional;
    }

    public LazyTokenListMatch(LazyTokenPatternMatch type, LazyTokenPatternMatch separator, boolean optional) {
        this.pattern = type;
        this.optional = optional;
        this.separator = separator;
    }

    @Override
    public LazyTokenListMatch setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, LazyLexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {index});
        if(st.find(thisInvoc)) {
            return new TokenMatchResponse(false, null, 0, this.pattern, null);
        }
        st.push(thisInvoc);
        boolean expectSeparator = false;

        boolean hasMatched = true;
        Token faultyToken = null;
        int length = 0;
        GeneralTokenPatternMatch expected = null;
        TokenList list = new TokenList().setName(this.name).addTags(this.tags);

        Stack tempStack = st.clone();

        itemLoop: for (int i = index; i < lexer.getFileLength();) {
            MethodInvocation tempInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {i});
            tempStack.push(tempInvoc);

            lexer.setCurrentIndex(i);

            if (this.separator != null && expectSeparator) {
                TokenMatchResponse itemMatch = this.separator.match(i, lexer, tempStack);
                expectSeparator = false;
                switch(itemMatch.getMatchType()) {
                    case NO_MATCH: {
                        break itemLoop;
                    }
                    case PARTIAL_MATCH: {
                        hasMatched = false;
                        faultyToken = itemMatch.faultyToken;
                        expected = itemMatch.expected;
                        length += itemMatch.length;
                        if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                        break itemLoop;
                    }
                    case COMPLETE_MATCH: {
                        i += itemMatch.length;
                        length += itemMatch.length;
                        if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                    }
                }
            } else {
                if (this.separator != null) {
                    TokenMatchResponse itemMatch = this.pattern.match(i, lexer, tempStack);
                    switch(itemMatch.getMatchType()) {
                        case NO_MATCH: {
                            hasMatched = false;
                            faultyToken = itemMatch.faultyToken;
                            expected = itemMatch.expected;
                            length += itemMatch.length;
                            if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                            break itemLoop;
                        }
                        case PARTIAL_MATCH: {
                            hasMatched = false;
                            faultyToken = itemMatch.faultyToken;
                            expected = itemMatch.expected;
                            length += itemMatch.length;
                            if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                            break itemLoop;
                        }
                        case COMPLETE_MATCH: {
                            i += itemMatch.length;
                            length += itemMatch.length;
                            if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                            expectSeparator = true;
                        }
                    }
                } else {
                    TokenMatchResponse itemMatch = this.pattern.match(i, lexer, tempStack);
                    length += itemMatch.length;
                    switch(itemMatch.getMatchType()) {
                        case NO_MATCH: {
                            if(length <= 0) {
                                hasMatched = false;
                                faultyToken = itemMatch.faultyToken;
                                expected = itemMatch.expected;
                                length += itemMatch.length;
                                if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                            }
                            break itemLoop;
                        }
                        case PARTIAL_MATCH: {
                            hasMatched = false;
                            faultyToken = itemMatch.faultyToken;
                            expected = itemMatch.expected;
                            if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                            break itemLoop;
                        }
                        case COMPLETE_MATCH: {
                            i += itemMatch.length;
                            if(itemMatch.pattern != null) list.add(itemMatch.pattern);
                        }
                    }
                }
            }
            tempStack.pop();
        }
        st.pop();
        return new TokenMatchResponse(hasMatched, faultyToken, length, expected, list);
    }

    @Override
    public String toString() {
        String s = "";
        if (optional) {
            s += "[";
        } else {
            s += "<";
        }
        s += pattern;
        if (separator != null) {
            s += "," + separator;
        }
        s += "...";
        if (optional) {
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
        if (optional) {
            s += "[";
        } else {
            s += "<";
        }
        s += pattern.deepToString(levels-1);
        if (separator != null) {
            s += "," + separator;
        }
        s += "...";
        if (optional) {
            s += "]";
        } else {
            s += ">";
        }
        return s;
    }

    @Override
    public String toTrimmedString() {
        String s = pattern.toTrimmedString();
        if (separator != null) {
            s += "," + separator.toTrimmedString();
        }
        s += "...";
        return s;
    }
}

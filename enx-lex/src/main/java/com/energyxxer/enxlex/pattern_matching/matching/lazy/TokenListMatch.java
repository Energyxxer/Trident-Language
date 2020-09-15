package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.StandardTags;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;

import static com.energyxxer.enxlex.pattern_matching.TokenMatchResponse.*;

public class TokenListMatch extends TokenPatternMatch {
    protected TokenPatternMatch pattern;
    protected TokenPatternMatch separator = null;

    public TokenListMatch(TokenType type) {
        this.pattern = new TokenItemMatch(type);
        this.optional = false;
    }

    public TokenListMatch(TokenType type, TokenType separator) {
        this.pattern = new TokenItemMatch(type);
        this.optional = false;
        this.separator = new TokenItemMatch(separator);
    }

    public TokenListMatch(TokenType type, boolean optional) {
        this.pattern = new TokenItemMatch(type);
        this.optional = optional;
    }

    public TokenListMatch(TokenType type, TokenType separator, boolean optional) {
        this.pattern = new TokenItemMatch(type);
        this.optional = optional;
        this.separator = new TokenItemMatch(separator);
    }

    public TokenListMatch(TokenPatternMatch type) {
        this.pattern = type;
        this.optional = false;
    }

    public TokenListMatch(TokenPatternMatch type, TokenPatternMatch separator) {
        this.pattern = type;
        this.optional = false;
        this.separator = separator;
    }

    public TokenListMatch(TokenPatternMatch type, boolean optional) {
        this.pattern = type;
        this.optional = optional;
    }

    public TokenListMatch(TokenPatternMatch type, TokenPatternMatch separator, boolean optional) {
        this.pattern = type;
        this.optional = optional;
        this.separator = separator;
    }

    @Override
    public TokenListMatch setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, Lexer lexer) {
        lexer.setCurrentIndex(index);
        boolean expectSeparator = false;

        boolean hasMatched = true;
        Token faultyToken = null;
        int length = 0;
        TokenPatternMatch expected = null;
        TokenList list = new TokenList(this).setName(this.name).addTags(this.tags);

        itemLoop: for (int i = index; i < lexer.getFileLength();) {
            lexer.setCurrentIndex(i);

            if (this.separator != null && expectSeparator) {
                TokenMatchResponse itemMatch = this.separator.match(i, lexer);
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
                    TokenMatchResponse itemMatch = this.pattern.match(i, lexer);
                    switch(itemMatch.getMatchType()) {
                        case NO_MATCH:
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
                            if(itemMatch.pattern instanceof TokenStructure && ((TokenStructure) itemMatch.pattern).getContents().hasTag(StandardTags.LIST_TERMINATOR)) {
                                break itemLoop;
                            }
                        }
                    }
                } else {
                    TokenMatchResponse itemMatch = this.pattern.match(i, lexer);
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
                            if(itemMatch.pattern instanceof TokenStructure && ((TokenStructure) itemMatch.pattern).getContents().hasTag(StandardTags.LIST_TERMINATOR)) {
                                break itemLoop;
                            }
                        }
                    }
                }
            }
        }
        if(!hasMatched) {
            invokeFailProcessors(list, lexer);
        } else {
            invokeProcessors(list, lexer);
        }
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

    public TokenPatternMatch getSeparatorMatch() {
        return separator;
    }
    public TokenPatternMatch getEntryMatch() {
        return pattern;
    }
}

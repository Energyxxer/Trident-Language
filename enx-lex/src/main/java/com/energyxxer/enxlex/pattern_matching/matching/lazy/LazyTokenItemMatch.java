package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
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
        this.name = name;
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, LazyLexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"int"}, new Object[] {index});

        if(st.find(thisInvoc)) {
            return new TokenMatchResponse(false, null, 0, this, null);
        }
        st.push(thisInvoc);
        boolean matched;
        Token faultyToken = null;

        Token retrieved = lexer.retrieveTokenOfType(this.type);
        if(retrieved == null) return new TokenMatchResponse(false, lexer.retrieveAnyToken(), 0, this, null);

        matched = stringMatch == null || retrieved.value.equals(stringMatch);

        if (!matched) {
            faultyToken = retrieved;
        }

        int length = (matched) ? retrieved.loc.index + retrieved.value.length() - index : 0;

        TokenItem item = new TokenItem(retrieved).setName(this.name).addTags(this.tags);

        st.pop();
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

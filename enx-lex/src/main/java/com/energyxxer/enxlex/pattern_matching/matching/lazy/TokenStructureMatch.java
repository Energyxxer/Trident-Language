package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.ArrayList;

public class TokenStructureMatch extends TokenPatternMatch {
    private ArrayList<TokenPatternMatch> entries = new ArrayList<>();
    /**
     * When greedy: false
     * The structure will always try to return a positive match, even if there are longer negative matches.
     *
     * When greedy: true
     * The structure will always return the longest match, regardless of whether it's positive or not.
     *
     * Use greedy structures whenever the first few tokens of the entries overlap, so error messages point to the
     * point where the match failed, rather than some point far before it failed.
     * */
    private boolean greedy = false;

    public TokenStructureMatch(String name) {
        this.name = name;
        optional = false;
    }

    public TokenStructureMatch(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }

    @Override
    public TokenStructureMatch setName(String name) {
        super.setName(name);
        return this;
    }

    public TokenStructureMatch add(TokenPatternMatch g) {
        if(!entries.contains(g)) entries.add(g);
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, Lexer lexer, Stack st) {
        lexer.setCurrentIndex(index);
        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[]{"int"}, new Object[]{index});
        st.push(thisInvoc);

        int popSuggestionStatus = handleSuggestionTags(lexer, index);

        TokenMatchResponse longestMatch = null;

        if(entries.isEmpty()) {
            //throw new IllegalStateException("Cannot attempt match; TokenStructureMatch '" + this.name + "' is empty.");
            invokeFailProcessors(0, lexer);
            return new TokenMatchResponse(false, null, 0, this, null);
        }
        for (TokenPatternMatch entry : entries) {
            lexer.setCurrentIndex(index);

            MethodInvocation newInvoc = new MethodInvocation(entry, "match", new String[]{"int"}, new Object[]{index});
            //if(st.find(newInvoc)) continue;
            TokenMatchResponse itemMatch = entry.match(index, lexer, st);

            if (longestMatch == null) {
                longestMatch = itemMatch;
            } else if(itemMatch.length >= longestMatch.length) {
                if (!longestMatch.matched || itemMatch.matched || (greedy && itemMatch.length > longestMatch.length)) {
                    longestMatch = itemMatch;
                }
            }
        }

        st.pop();
        while(--popSuggestionStatus >= 0) {
            lexer.getSuggestionModule().popStatus();
        }

        if (longestMatch == null || longestMatch.matched) {
            if(longestMatch != null) {
                TokenStructure struct = new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags);
                invokeProcessors(struct, lexer);
                return new TokenMatchResponse(true, null, longestMatch.length, struct);
            } else {
                return new TokenMatchResponse(true, null, 0, null);
            }
        } else {
            invokeFailProcessors(longestMatch.length, lexer);
            if (longestMatch.length <= 0 && entries.size() > 1) {
                return new TokenMatchResponse(false, longestMatch.faultyToken, longestMatch.length, this, null/*new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags)*/);
            } else {
                return new TokenMatchResponse(false, longestMatch.faultyToken, longestMatch.length, longestMatch.expected, null/*new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags)*/);
            }
        }
    }

    @Override
    public String toString() {
        return ((optional) ? "[" : "<") + "-" + name + "-" + ((optional) ? "]" : ">");
    }

    public String deepToString(int levels) {
        if(levels <= 0) return toString();
        StringBuilder s = new StringBuilder(((optional) ? "[" : "<") + "-");
        for (int i = 0; i < entries.size(); i++) {
            s.append(entries.get(i).deepToString(levels - 1));
            if (i < entries.size() - 1) {
                s.append("\n OR ");
            }
        }
        return s + "-" + ((optional) ? "]" : ">");
    }

    @Override
    public String toTrimmedString() {
        String humanReadableName = name.toLowerCase().replace('_',' ');
        humanReadableName = humanReadableName.substring(0,1).toUpperCase() + humanReadableName.substring(1);
        return humanReadableName;
    }

    public TokenStructureMatch exclude(TokenPatternMatch entryToExclude) {
        TokenStructureMatch newStruct = new TokenStructureMatch(name, optional);
        for(TokenPatternMatch entry : entries) {
            if(entry != entryToExclude) {
                newStruct.add(entry);
            }
        }
        return newStruct;
    }

    public TokenStructureMatch setGreedy(boolean greedy) {
        this.greedy = greedy;
        return this;
    }

    public boolean getGreedy() {
        return greedy;
    }

    public void remove(TokenPatternMatch pattern) {
        entries.remove(pattern);
    }
}

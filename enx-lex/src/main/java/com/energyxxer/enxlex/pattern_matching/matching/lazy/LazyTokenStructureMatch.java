package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.ArrayList;

public class LazyTokenStructureMatch extends LazyTokenPatternMatch {
    private ArrayList<LazyTokenPatternMatch> entries = new ArrayList<>();
    private ArrayList<LazyTokenPatternMatch> nestedEntries = new ArrayList<>();

    private java.util.Stack<NestedEntry> nestedStack = new java.util.Stack<>();

    public LazyTokenStructureMatch(String name) {
        this.name = name;
        optional = false;
    }

    public LazyTokenStructureMatch(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }

    @Override
    public LazyTokenStructureMatch setName(String name) {
        super.setName(name);
        return this;
    }

    public LazyTokenStructureMatch add(LazyTokenPatternMatch g) {
        entries.add(g);
        return this;
    }

    public LazyTokenStructureMatch addNested(LazyTokenPatternMatch g) {
        nestedEntries.add(g);
        return this;
    }

    @Override
    public TokenMatchResponse match(int index, LazyLexer lexer, Stack st) {
        if(!nestedStack.isEmpty() && nestedStack.peek().index == index) {
            return nestedStack.peek().response;
        }

        lexer.setCurrentIndex(index);
        MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[]{"int"}, new Object[]{index});
        st.push(thisInvoc);

        TokenMatchResponse longestMatch = null;

        if(entries.isEmpty()) throw new IllegalStateException("Cannot attempt match; TokenStructureMatch '" + this.name + "' is empty.");
        for (LazyTokenPatternMatch entry : entries) {
            lexer.setCurrentIndex(index);

            MethodInvocation newInvoc = new MethodInvocation(entry, "match", new String[]{"int"}, new Object[]{index});
            //if(st.find(newInvoc)) continue;
            TokenMatchResponse itemMatch = entry.match(index, lexer, st);

            if (longestMatch == null) {
                longestMatch = itemMatch;
            } else if(itemMatch.length >= longestMatch.length) {
                if (!longestMatch.matched || itemMatch.matched) {
                    longestMatch = itemMatch;
                }
            }
        }

        if(longestMatch != null && longestMatch.matched && !nestedEntries.isEmpty()) {
            nestedStack.push(new NestedEntry(index, new TokenMatchResponse(true, null, longestMatch.length, new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags))));
            boolean changed = true;
            while(changed) {
                changed = false;
                for(LazyTokenPatternMatch nested : nestedEntries) {
                    lexer.setCurrentIndex(index);

                    MethodInvocation newInvoc = new MethodInvocation(nested, "match", new String[]{"int"}, new Object[]{index});
                    //if(st.find(newInvoc)) continue;
                    TokenMatchResponse itemMatch = nested.match(index, lexer, st);

                    if(itemMatch.length > longestMatch.length) {
                        if (!longestMatch.matched || itemMatch.matched) {
                            changed = true;
                            longestMatch = itemMatch;
                            nestedStack.peek().response = new TokenMatchResponse(true, null, longestMatch.length, new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags));
                        }
                    }
                }
            }
            nestedStack.pop();
        }

        st.pop();

        if (longestMatch == null || longestMatch.matched) {
            return new TokenMatchResponse(true, null, (longestMatch == null) ? 0 : longestMatch.length, (longestMatch == null) ? null : new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags));
        } else {
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
        String s = ((optional) ? "[" : "<") + "-";
        for (int i = 0; i < entries.size(); i++) {
            s += entries.get(i).deepToString(levels-1);
            if (i < entries.size() - 1) {
                s += "\n OR ";
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

    public LazyTokenStructureMatch exclude(LazyTokenPatternMatch entryToExclude) {
        LazyTokenStructureMatch newStruct = new LazyTokenStructureMatch(name, optional);
        for(LazyTokenPatternMatch entry : entries) {
            if(entry != entryToExclude) {
                newStruct.add(entry);
            }
        }
        return newStruct;
    }

    private class NestedEntry {
        int index;
        TokenMatchResponse response;

        public NestedEntry(int index, TokenMatchResponse response) {
            this.index = index;
            this.response = response;
        }
    }
}

package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a token structure, containing multiple ways a series of tokens
 * could match with this structure.
 */
public class TokenStructureMatch extends TokenPatternMatch {
	private ArrayList<TokenPatternMatch> entries = new ArrayList<>();
	
	public TokenStructureMatch(String name) {
		this.name = name;
		optional = false;
	}
	
	public TokenStructureMatch(String name, boolean optional) {
		this.name = name;
		this.optional = optional;
	}

	public TokenStructureMatch add(TokenPatternMatch g) {
		entries.add(g);
		return this;
	}
	
	public TokenMatchResponse match(List<Token> tokens) {
		return match(tokens, null, new Stack());
	}
	
	@Override
	public TokenStructureMatch setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st) {
		MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[]{"List<Token>"}, new Object[]{tokens});
		st.push(thisInvoc);

		TokenMatchResponse longestMatch = null;

		if(entries.isEmpty()) throw new IllegalStateException("Cannot attempt match; TokenStructureMatch '" + this.name + "' is empty.");
		for (TokenPatternMatch entry : entries) {
			List<Token> subList = tokens.subList(0,tokens.size());
			MethodInvocation newInvoc = new MethodInvocation(entry, "match", new String[]{"List<Token>"}, new Object[]{subList});
			if(st.find(newInvoc)) continue;
			TokenMatchResponse itemMatch = entry.match(subList, lastToken, st);

			if (longestMatch == null) {
				longestMatch = itemMatch;
			} else if(itemMatch.length >= longestMatch.length) {
				if (!longestMatch.matched || itemMatch.matched) {
					longestMatch = itemMatch;
				}
			}
		}

		st.pop();

		if (longestMatch == null || longestMatch.matched) {
			return new TokenMatchResponse(true, null, (longestMatch == null) ? 0 : longestMatch.length, (longestMatch == null) ? null : new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags));
		} else {
			if (longestMatch.length <= 0 && entries.size() > 1) {
				return new TokenMatchResponse(false, longestMatch.faultyToken, longestMatch.length, this, new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags));
			} else {
				return new TokenMatchResponse(false, longestMatch.faultyToken, longestMatch.length, longestMatch.expected, new TokenStructure(this.name, longestMatch.pattern).addTags(this.tags));
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

	public TokenStructureMatch exclude(TokenPatternMatch entryToExclude) {
	    TokenStructureMatch newStruct = new TokenStructureMatch(name, optional);
	    for(TokenPatternMatch entry : entries) {
	        if(entry != entryToExclude) {
                newStruct.add(entry);
            }
        }
        return newStruct;
    }
}

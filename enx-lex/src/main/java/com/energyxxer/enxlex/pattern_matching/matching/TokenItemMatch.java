package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.List;

/**
 * Represents a condition a single token should meet for it to be considered
 * matching to a token structure.
 */
public class TokenItemMatch extends TokenPatternMatch {
	private TokenType type;
	private String stringMatch = null;

	public TokenItemMatch(TokenType type) {
		this.type = type;
		this.optional = false;
	}

	public TokenItemMatch(TokenType type, String stringMatch) {
		this.type = type;
		this.stringMatch = stringMatch;
		this.optional = false;
	}

	public TokenItemMatch(TokenType type, boolean optional) {
		this.type = type;
		this.optional = optional;
	}

	public TokenItemMatch(TokenType type, String stringMatch, boolean optional) {
		this.type = type;
		this.stringMatch = stringMatch;
		this.optional = optional;
	}
	
	public TokenMatchResponse match(List<Token> tokens) {
		return match(tokens, null, new Stack());
	}
	
	@Override
	public TokenItemMatch setName(String name) {
		this.name = name;
		return this;
	}

	public TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st) {
		MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"List<Token>"}, new Object[] {tokens});

		if(tokens.size() <= 0 || st.find(thisInvoc)) {
			return new TokenMatchResponse(false, null, 0, this, null);
		}
		st.push(thisInvoc);
		boolean matched;
		Token faultyToken = null;

		matched = tokens.size() > 0 && (this.type == null || tokens.get(0).type == this.type) && (stringMatch == null || tokens.get(0).value.equals(stringMatch));

		if (!matched && tokens.size() > 0) {
			faultyToken = tokens.get(0);
		}

		int length = (matched) ? 1 : 0;
		
		TokenItem item = null;
		if(tokens.size() > 0) item = new TokenItem(tokens.get(0)).setName(this.name).addTags(this.tags);

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

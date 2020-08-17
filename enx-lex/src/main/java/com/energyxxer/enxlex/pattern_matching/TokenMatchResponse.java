package com.energyxxer.enxlex.pattern_matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

public class TokenMatchResponse {
	public final boolean matched;
	public final Token faultyToken;
	public final int length;
	public TokenPatternMatch expected = null;
	public TokenPattern<?> pattern = null;

	public static final int NO_MATCH = 0;
	public static final int PARTIAL_MATCH = 1;
	public static final int COMPLETE_MATCH = 2;

	public TokenMatchResponse(boolean matched, Token faultyToken, int length, TokenPattern<?> pattern) {
		this(matched, faultyToken, length, null, pattern);
	}
	
	public TokenMatchResponse(boolean matched, Token faultyToken, int length, TokenPatternMatch expected, TokenPattern<?> pattern) {
		this.matched = matched;
		this.faultyToken = faultyToken;
		this.length = length;
		this.expected = expected;
		this.pattern = pattern;
	}

	public int getMatchType() {
		if(matched) return COMPLETE_MATCH;
		if(length > 0) return PARTIAL_MATCH;
		return NO_MATCH;
	}

	@Override
	public String toString() {
		return "TokenMatchResponse{" +
				"matched=" + matched +
				", faultyToken=" + faultyToken +
				", length=" + length +
				", expected=" + expected +
				", pattern=" + pattern +
				", matchType=" + getMatchType() +
				'}';
	}

	public String getErrorMessage() {
		if (!matched) {
			if(faultyToken == null || faultyToken.type == TokenType.END_OF_FILE) {
				return "Expected " + expected.toTrimmedString();
			}
			return "Unexpected token '" + faultyToken.value + "'. " + expected.toTrimmedString() + " expected";
		}
		return null;
	}
}

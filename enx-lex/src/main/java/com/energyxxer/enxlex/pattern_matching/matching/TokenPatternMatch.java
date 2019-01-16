package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.util.Stack;

import java.util.List;

public abstract class TokenPatternMatch extends GeneralTokenPatternMatch {
	
	public abstract TokenMatchResponse match(List<Token> tokens);

	public abstract TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st);

	public TokenPatternMatch setOptional() {
		return setOptional(true);
	}

	public TokenPatternMatch setOptional(boolean optional) {
		this.optional = optional;
		return this;
	}
}
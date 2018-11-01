package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.util.Stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TokenPatternMatch {
	public String name = "";
	public boolean optional;
	public List<String> tags = new ArrayList<>();
	
	public abstract TokenMatchResponse match(List<Token> tokens);

	public abstract TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st);
	
	public TokenPatternMatch setName(String name) {
		this.name = name;
		return this;
	}

	public abstract String deepToString(int levels);

	public abstract String toTrimmedString();

    public TokenPatternMatch addTags(String... newTags) {
    	tags.addAll(Arrays.asList(newTags));
		return this;
    }
}
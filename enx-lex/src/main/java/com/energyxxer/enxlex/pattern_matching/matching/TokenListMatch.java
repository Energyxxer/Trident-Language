package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.util.MethodInvocation;
import com.energyxxer.util.Stack;

import java.util.List;

import static com.energyxxer.enxlex.pattern_matching.TokenMatchResponse.*;

/**
 * Represents a sequence of tokens of a given type, of an undefined length.
 */
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
		this.name = name;
		return this;
	}
	
	public TokenMatchResponse match(List<Token> tokens) {
		return match(tokens, null, new Stack());
	}

	@Override
	public TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st) {
		MethodInvocation thisInvoc = new MethodInvocation(this, "match", new String[] {"List<Token>"}, new Object[] {tokens});
		if(tokens.size() <= 0 || st.find(thisInvoc)) {
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

		itemLoop: for (int i = 0; i < tokens.size();) {
			List<Token> subList = tokens.subList(i, tokens.size());

			MethodInvocation tempInvoc = new MethodInvocation(this, "match", new String[] {"List<Token>"}, new Object[] {subList});
			tempStack.push(tempInvoc);

			if (this.separator != null && expectSeparator) {
				TokenMatchResponse itemMatch = this.separator.match(subList, lastToken, tempStack);
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
					TokenMatchResponse itemMatch = this.pattern.match(subList, lastToken, tempStack);
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
					TokenMatchResponse itemMatch = this.pattern.match(subList, lastToken, tempStack);
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
			if(i > 0) lastToken = tokens.get(i-1);
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

package com.energyxxer.enxlex.pattern_matching.structures;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.PatternEvaluator;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.logger.Debug;
import com.sun.istack.internal.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class TokenPattern<T> {

	protected String name = "";
	protected ArrayList<String> tags = new ArrayList<>();
	public final TokenPatternMatch source;

	public TokenPattern(TokenPatternMatch source) {
		this.source = source;
	}

	public abstract T getContents();
	public abstract TokenPattern<T> setName(String name);

	public abstract List<Token> search(TokenType type);
	public abstract List<Token> deepSearch(TokenType type);
	public abstract List<TokenPattern<?>> searchByName(String name);
	public abstract List<TokenPattern<?>> deepSearchByName(String name);

	public abstract TokenPattern<?> find(String path);

	@NotNull public TokenPattern<?> tryFind(String path) {
		TokenPattern<?> rv = find(path);
		if(rv == null) {
			rv = this;
		}
		return rv;
	}

	public abstract String flatten(boolean separate);

	public abstract File getFile();

	public String getLocation() {
		StringLocation loc = getStringLocation();
		return getFile().getName() + ":" + loc.line + ":" + loc.column + "#" + loc.index;
	}

	public abstract StringLocation getStringLocation();
	public abstract StringBounds getStringBounds();
	public int getCharLength() {
		ArrayList<Token> tokens = flattenTokens();
		if(tokens.size() == 0) return 0;
		int start = tokens.get(0).loc.index;
		Token lastToken = tokens.get(tokens.size()-1);
		int end = lastToken.loc.index + lastToken.value.length();
		return end - start;
	}

	public String getFormattedPath() {
		StringLocation loc = getStringLocation();
		return "\b" + getFile() + "\b" + loc.index + "\b" + getCharLength() + "\b"
				+ getLocation() + "\b";
	}

	public abstract ArrayList<Token> flattenTokens();

	public abstract String getType();

    public String getName() {
        return name;
    }

	public TokenPattern addTag(String newTag) {
		tags.add(newTag);
		return this;
	}

	public TokenPattern addTags(List<String> newTags) {
		tags.addAll(newTags);
		return this;
	}

	public List<String> getTags() {
    	return tags;
	}

	public boolean hasTag(String tag) {
    	return tags.contains(tag);
	}

	public abstract void validate();

	public Object findThenEvaluate(String path, Object defaultValue, Object... data) {
		TokenPattern<?> found = find(path);
		if(found == null) return defaultValue;
		return found.evaluate(data);
	}

	public Object findThenEvaluateLazyDefault(String path, Supplier<Object> defaultValue, Object... data) {
		TokenPattern<?> found = find(path);
		if(found == null) return defaultValue.get();
		return found.evaluate(data);
	}

    public Object evaluate(Object... data) {
    	SimplificationDomain simplified = new SimplificationDomain(this, data).simplifyFully();

    	TokenPatternMatch simplifiedSource = simplified.pattern.source;

    	PatternEvaluator evaluator = simplifiedSource.getEvaluator();
    	if(evaluator == null) {
    		Debug.log("Missing evaluator for pattern " + simplifiedSource);
    		throw new NullPointerException();
		}
    	return evaluator.evaluate(simplified.pattern, simplified.data);
	}

	public static Object evaluate(TokenPattern<?> pattern, Object... data) {
		SimplificationDomain domain = new SimplificationDomain(pattern, data).simplifyFully();
    	return domain.pattern.evaluate(domain.data);
	}

	public void simplify(SimplificationDomain domain) {
    	//domain.pattern == this
		Consumer<SimplificationDomain> simplificationFunction = source.getSimplificationFunction();
		if(simplificationFunction != null) {
			simplificationFunction.accept(domain);
		}
	}

	public static class SimplificationDomain {
    	public TokenPattern<?> pattern;
    	public Object[] data;

		public SimplificationDomain(TokenPattern<?> pattern, Object[] data) {
			this.pattern = pattern;
			this.data = data;
		}

		public SimplificationDomain simplifyOnce() {
			pattern.simplify(this);
			return this;
		}

		public SimplificationDomain simplifyFully() {
			TokenPattern previous = null;
			while(pattern != previous) {
				previous = pattern;
				simplifyOnce();
			}
			return this;
		}
	}
}

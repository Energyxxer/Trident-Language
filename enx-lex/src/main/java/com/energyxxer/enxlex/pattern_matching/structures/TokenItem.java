package com.energyxxer.enxlex.pattern_matching.structures;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TokenItem extends TokenPattern<Token> {
	private Token token;
	
	public TokenItem(Token token) {
		this.token = token;
	}

	@Override
	public Token getContents() {
		return token;
	}
	
	@Override
	public TokenItem setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return "(I)" + ((name != null && name.length() > 0) ? name + ": " : "") + "{ " + token + " }";
	}

	@Override
	public List<Token> search(TokenType type) {
		ArrayList<Token> list = new ArrayList<>();
		if(token.type == type) list.add(token);
		return list;
	}

	@Override
	public List<Token> deepSearch(TokenType type) {
		return search(type);
	}

	@Override
	public List<TokenPattern<?>> searchByName(String name) {
		return new ArrayList<>();
	}

	@Override
	public List<TokenPattern<?>> deepSearchByName(String name) {
		return new ArrayList<>();
	}

	@Override
	public TokenPattern<?> find(String path) {
		return (this.name != null && name.equals(path)) ? this : null;
	}

	@Override
	public String flatten(boolean separate) {
		return token.value;
	}

	@Override
	public File getFile() {
		return new File(token.file);
	}

	@Override
	public StringLocation getStringLocation() {
		return new StringLocation(token.loc.index, token.loc.line, token.loc.column);
	}

	@Override
	public StringBounds getStringBounds() {
		return new StringBounds(
				new StringLocation(token.loc.index, token.loc.line, token.loc.column),
				new StringLocation(token.loc.index + token.value.length(), token.loc.line, token.loc.column + token.value.length())
		);
	}

    @Override
    public ArrayList<Token> flattenTokens() {
	    ArrayList<Token> list = new ArrayList<>();
	    list.add(token);
        return list;
    }

	@Override
	public String getType() {
		return "ITEM";
	}

	@Override
	public TokenItem addTags(List<String> newTags) {
		super.addTags(newTags);
		return this;
	}

	@Override
	public void validate() {
		if(this.name != null && this.name.length() > 0) this.tags.add(name);

		for(String tag : this.tags) {
			if(!tag.startsWith("__")) this.token.tags.add(tag);
		}
	}
}

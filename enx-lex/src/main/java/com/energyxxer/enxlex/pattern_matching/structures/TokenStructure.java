package com.energyxxer.enxlex.pattern_matching.structures;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TokenStructure extends TokenPattern<TokenPattern<?>> {
	private TokenPattern<?> group;
	
	public TokenStructure(String name, TokenPattern<?> group) {
		this.name = name;
		this.group = group;
	}

	@Override
	public TokenPattern<?> getContents() {
		return group;
	}
	
	@Override
	public TokenStructure setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		return "(S)" + name + ": {" + group.toString() + "}";
	}

	@Override
	public List<Token> search(TokenType type) {
		return group.search(type);
	}

	@Override
	public List<Token> deepSearch(TokenType type) {
		return group.deepSearch(type);
	}

	@Override
	public List<TokenPattern<?>> searchByName(String name) {
		return group.searchByName(name);
	}

	@Override
	public List<TokenPattern<?>> deepSearchByName(String name) {
		ArrayList<TokenPattern<?>> list = new ArrayList<>();
		if(group.name.equals(name)) list.add(group);
		list.addAll(group.deepSearchByName(name));
		return list;
	}

	@Override
	public TokenPattern<?> find(String path) {

		String[] subPath = path.split("\\.",2);

		if(subPath.length == 1) return (this.name.equals(path)) ? this : group.find(path);

		return (group.name.equals(subPath[0])) ? group.find(subPath[1]) : group.find(path);
	}

	@Override
	public String flatten(boolean separate) {
		return group.flatten(separate);
	}

	@Override
	public File getFile() {
		return group.getFile();
	}

	@Override
	public StringLocation getStringLocation() {
		return group.getStringLocation();
	}

	@Override
	public StringBounds getStringBounds() { return group.getStringBounds(); }

    @Override
    public ArrayList<Token> flattenTokens() {
        return group.flattenTokens();
    }

	@Override
	public String getType() {
		return "STRUCTURE";
	}

	@Override
	public TokenStructure addTags(List<String> newTags) {
		super.addTags(newTags);
		return this;
	}

	@Override
	public void validate() {
		if(this.name != null && this.name.length() > 0) this.tags.add(name);
		group.addTags(this.tags);
		group.validate();
	}
}

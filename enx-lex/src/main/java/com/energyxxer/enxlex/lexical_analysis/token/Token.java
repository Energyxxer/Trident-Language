package com.energyxxer.enxlex.lexical_analysis.token;

import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class containing a value, or token, its type, source file and location within
 * it.
 */
public class Token {
	public String value;
	public TokenType type;
	public String file;
	public String filename;
	public StringLocation loc;
	public HashMap<String, Object> attributes;
	public HashMap<TokenSection, String> subSections;

	public ArrayList<String> tags = new ArrayList<>();

    public Token(String value, File file, StringLocation loc) {
		this.value = value;
		this.type = TokenType.UNKNOWN;
		this.file = file.getAbsolutePath();
		this.filename = file.getName();
		this.loc = loc;
		this.attributes = new HashMap<>();
		this.subSections = new HashMap<>();
	}

	public Token(String value, File file, StringLocation loc, HashMap<TokenSection, String> subSections) {
		this.value = value;
		this.type = TokenType.UNKNOWN;
		this.file = file.getAbsolutePath();
		this.filename = file.getName();
		this.loc = loc;
		this.attributes = new HashMap<>();
		this.subSections = (subSections != null) ? subSections : new HashMap<>();
	}

	public Token(String value, TokenType tokenType, File file, StringLocation loc) {
		this.value = value;
		this.type = (tokenType != null) ? tokenType : TokenType.UNKNOWN;
		this.file = file.getAbsolutePath();
		this.filename = file.getName();
		this.loc = loc;
		this.attributes = new HashMap<>();
		this.subSections = new HashMap<>();
	}

	public Token(String value, TokenType tokenType, File file, StringLocation loc, HashMap<TokenSection, String> subSections) {
		this.value = value;
		this.type = (tokenType != null) ? tokenType : TokenType.UNKNOWN;
		this.file = file.getAbsolutePath();
		this.filename = file.getName();
		this.loc = loc;
		this.attributes = new HashMap<>();
		this.subSections = (subSections != null) ? subSections : new HashMap<>();
	}

	public boolean isSignificant() {
		return type.isSignificant();
	}

	public String getLocation() {
		return filename + ":" + loc.line + ":" + loc.column + "#" + loc.index;
	}

	public String getFormattedPath() {
		return "\b" + file + "\b" + loc.index + "\b" + value.length() + "\b"
				+ getLocation() + "\b";
	}

	@Override
	public String toString() {
    	boolean verbose = true;
    	if(verbose) {
    		return type.getHumanReadableName() + " '" + value + "' (" + (isSignificant() ? "" : "in") + "significant)";
		} else
		return value;
	}
	
	public static Token merge(TokenType type, Token... tokens) {
		StringBuilder s = new StringBuilder();
		for(Token t : tokens) {
			s.append(t.value);
		}
		return new Token(s.toString(),type,new File(tokens[0].file),tokens[0].loc);
	}

	public HashMap<TokenSection, String> getSubSections() {
		return subSections;
	}

	public void setSubSections(HashMap<TokenSection, String> subSections) {
		this.subSections = subSections;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Token token = (Token) o;

		if (value != null ? !value.equals(token.value) : token.value != null) return false;
		if (type != null ? !type.equals(token.type) : token.type != null) return false;
		if (file != null ? !file.equals(token.file) : token.file != null) return false;
		if (loc != null ? !loc.equals(token.loc) : token.loc != null) return false;
		return attributes != null ? attributes.equals(token.attributes) : token.attributes == null;
	}

	@Override
	public int hashCode() {
		int result = value != null ? value.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (file != null ? file.hashCode() : 0);
		result = 31 * result + (loc != null ? loc.hashCode() : 0);
		result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
		return result;
	}

	public StringBounds getStringBounds() {
		return new StringBounds(loc, new StringLocation(loc.index + value.length(), loc.line, loc.column + value.length()));
	}
}

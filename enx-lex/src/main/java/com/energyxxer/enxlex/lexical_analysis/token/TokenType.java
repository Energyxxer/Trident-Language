package com.energyxxer.enxlex.lexical_analysis.token;

/**
 * Utility class containing all token types, and a method to match a given
 * string to a token type, based on patterns present in LangConstants.
 */
public class TokenType {

	public static final TokenType
			FILE_HEADER = new TokenType("FILE_HEADER"), // Contains information about file type in attributes
			UNKNOWN = new TokenType("UNKNOWN"), // Default
			NEWLINE = new TokenType("NEWLINE"), // New line
			END_OF_FILE = new TokenType("END_OF_FILE"); // End of file

	private String name;
	private String humanReadableName;
	private boolean significant;

	public TokenType(String name) {
		this(name, true);
	}

	public TokenType(String name, boolean significant) {
		this.name = name;
		this.significant = significant;

		this.humanReadableName = name.toLowerCase().replace('_',' ');
		this.humanReadableName = this.humanReadableName.substring(0,1).toUpperCase() + this.humanReadableName.substring(1);
	}

	public boolean isSignificant() {
		return this.significant;
	}

	public String getHumanReadableName() {
		return humanReadableName;
	}

	@Override
	public String toString() {
		return name;
	}
}

package com.energyxxer.util;

public class StringLocation {
	public int index;
	public int line;
	public int column;

	public StringLocation(int length) {
		this(length, 0, length);
	}
	
	public StringLocation(int index, int line, int column) {
		this.index = index;
		this.line = line;
		this.column = column;
	}

	@Override
	public String toString() {
		return "" + line + ':' + column + '#' + index;
	}
}

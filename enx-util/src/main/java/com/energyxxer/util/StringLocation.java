package com.energyxxer.util;

import java.util.Objects;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringLocation that = (StringLocation) o;
		return index == that.index &&
				line == that.line &&
				column == that.column;
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, line, column);
	}

	@Override
	public String toString() {
		return "" + line + ':' + column + '#' + index;
	}
}

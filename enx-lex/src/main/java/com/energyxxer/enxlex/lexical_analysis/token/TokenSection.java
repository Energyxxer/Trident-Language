package com.energyxxer.enxlex.lexical_analysis.token;

/**
 * Created by User on 2/25/2017.
 */
public class TokenSection {
    public final int start, length;

    public TokenSection(int start, int length) {
        this.start = start;
        this.length = length;
    }

    @Override
    public String toString() {
        return "TokenSection{" +
                "start=" + start +
                ", length=" + length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenSection that = (TokenSection) o;

        if (start != that.start) return false;
        return length == that.length;
    }

    @Override
    public int hashCode() {
        int result = start;
        result = 31 * result + length;
        return result;
    }
}

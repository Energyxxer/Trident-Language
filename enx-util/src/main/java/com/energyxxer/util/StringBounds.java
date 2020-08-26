package com.energyxxer.util;

import java.util.Objects;

/**
 * Created by User on 1/1/2017.
 */
public class StringBounds {
    public StringLocation start;
    public StringLocation end;

    public StringBounds(StringLocation start, StringLocation end) {
        if(end.index < start.index) {
            this.start = end;
            this.end = start;
        } else {
            this.start = start;
            this.end = end;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringBounds that = (StringBounds) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" +
                "start=" + start +
                " - end=" + end +
                ']';
    }
}

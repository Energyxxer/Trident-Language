package com.energyxxer.enxlex.pattern_matching.matching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class GeneralTokenPatternMatch {
    public String name = "";
    public boolean optional;
    public List<String> tags = new ArrayList<>();


    public GeneralTokenPatternMatch addTags(String... newTags) {
        tags.addAll(Arrays.asList(newTags));
        return this;
    }

    public GeneralTokenPatternMatch setName(String name) {
        this.name = name;
        return this;
    }

    public abstract String deepToString(int levels);

    public abstract String toTrimmedString();
}

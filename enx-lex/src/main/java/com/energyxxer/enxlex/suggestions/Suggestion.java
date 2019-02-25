package com.energyxxer.enxlex.suggestions;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Suggestion {
    protected ArrayList<String> tags = new ArrayList<>();

    public void addTag(String tag) {
        tags.add(tag);
    }

    public Collection<String> getTags() {
        return tags;
    }
}

package com.energyxxer.enxlex.lexical_analysis.summary;

import java.util.function.Function;

public abstract class SummaryModule {
    public void onStart() {}
    public void onEnd() {}
    public abstract void updateIndices(Function<Integer, Integer> h);
    public abstract ProjectSummary getParentSummary();
}

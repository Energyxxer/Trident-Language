package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;

@AnalyzerGroup
public interface ScoreEventCriteriaHandler {
    void start(ScoreEventCriteriaData data);
    void mid(ScoreEventCriteriaData data);
    void end(ScoreEventCriteriaData data);
}

package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

@AnalyzerGroup(
        classes="BrokenScoreEvent,DroppedScoreEvent,PickedUpScoreEvent,UsedScoreEvent"
)
public interface ScoreEventCriteriaHandler {
    //once
    void startOnce(ItemEventFile itemEventFile);

    //looping
    void start(ItemEventFile itemEventFile);
    void mid(ItemEventFile itemEventFile, ScoreEventCriteriaData data);
    void end(ItemEventFile itemEventFile);

    //once
    void endOnce(ItemEventFile itemEventFile);
}

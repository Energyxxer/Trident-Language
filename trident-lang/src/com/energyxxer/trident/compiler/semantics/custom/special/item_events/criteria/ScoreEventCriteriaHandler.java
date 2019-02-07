package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;

@AnalyzerGroup
public interface ScoreEventCriteriaHandler {
    //once
    void globalStart(SpecialFileManager mgr);

    //looping
    void start(SpecialFileManager data, ItemEventFile itemEventFile);
    void mid(ScoreEventCriteriaData data);
    void end(SpecialFileManager data, ItemEventFile itemEventFile);

    //once
    void globalEnd(SpecialFileManager mgr);
}

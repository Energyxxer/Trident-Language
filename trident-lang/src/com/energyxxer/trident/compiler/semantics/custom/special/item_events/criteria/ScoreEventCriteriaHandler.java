package com.energyxxer.trident.compiler.semantics.custom.special.item_events.criteria;

import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;

@ParserGroup
public interface ScoreEventCriteriaHandler {
    void start(ScoreEventCriteriaData data);
    void mid(ScoreEventCriteriaData data);
    void end(ScoreEventCriteriaData data);
}

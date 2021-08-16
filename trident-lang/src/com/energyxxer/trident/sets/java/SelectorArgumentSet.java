package com.energyxxer.trident.sets.java;

import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.sets.java.selector_arguments.*;

public class SelectorArgumentSet extends PatternProviderSet {
    public SelectorArgumentSet() {
        super("SELECTOR_ARGUMENT");

        importUnits(
                AdvancementArgumentParser.class,
                ComponentArgumentParser.class,
                DistanceArgumentParser.class,
                DXArgumentParser.class,
                DYArgumentParser.class,
                DZArgumentParser.class,
                GamemodeArgumentParser.class,
                LevelArgumentParser.class,
                LimitArgumentParser.class,
                NameArgumentParser.class,
                NBTArgumentParser.class,
                PitchArgumentParser.class,
                PredicateArgumentParser.class,
                ScoresArgumentParser.class,
                SortArgumentParser.class,
                TagArgumentParser.class,
                TeamArgumentParser.class,
                TypeArgumentParser.class,
                XArgumentParser.class,
                YArgumentParser.class,
                YawArgumentParser.class,
                ZArgumentParser.class
        );
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure, PrismarineProjectWorker worker) {
        providerStructure.addTags(SuggestionTags.ENABLED);
    }
}

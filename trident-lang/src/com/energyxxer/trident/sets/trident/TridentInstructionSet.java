package com.energyxxer.trident.sets.trident;

import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.sets.trident.instructions.*;

public class TridentInstructionSet extends PatternProviderSet {
    public TridentInstructionSet() {
        super("INSTRUCTION");
        importUnits(
                BreakInstruction.class,
                ContinueInstruction.class,
                DefineInstruction.class,
                EvalInstruction.class,
                IfInstruction.class,
                LoopInstruction.ForInstruction.class,
                LoopInstruction.WhileInstruction.class,
                SwitchInstruction.class,
                LogInstruction.class,
                ReturnInstruction.class,
                ThrowInstruction.class,
                TryInstruction.class,
                UsingInstruction.class,
                VariableInstruction.class,
                WithinInstruction.class
        );
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure, PrismarineProjectWorker worker) {
        providerStructure.addTags(SuggestionTags.ENABLED, TridentSuggestionTags.CONTEXT_ENTRY);
    }
}

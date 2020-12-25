package com.energyxxer.trident.sets.trident;

import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.trident.sets.trident.instructions.*;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.suggestions.SuggestionTags;

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
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {
        providerStructure.addTags(SuggestionTags.ENABLED, TridentSuggestionTags.CONTEXT_ENTRY);
    }
}

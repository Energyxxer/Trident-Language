package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultyQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultySetCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.wrapperOptional;

public class DifficultyCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"difficulty"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("difficulty"),
                wrapperOptional(productions.getOrCreateStructure("DIFFICULTY_ID")).setName("NEW_DIFFICULTY")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Type newDifficulty = (Type) pattern.findThenEvaluate("NEW_DIFFICULTY", null, ctx);
        if (newDifficulty != null) {
            return new DifficultySetCommand(newDifficulty);
        } else {
            return new DifficultyQueryCommand();
        }
    }
}

package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultyQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultySetCommand;
import com.energyxxer.commodore.types.defaults.DifficultyType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "difficulty")
public class DifficultyParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> rawDifficulty = pattern.find(".DIFFICULTY");
        if(rawDifficulty != null) {
            return new DifficultySetCommand(CommonParsers.parseType(rawDifficulty.find("DIFFICULTY_ID"), ctx, DifficultyType.CATEGORY));
        } else return new DifficultyQueryCommand();
    }
}

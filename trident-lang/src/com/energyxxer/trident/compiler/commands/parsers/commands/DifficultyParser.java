package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultyQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.difficulty.DifficultySetCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "difficulty")
public class DifficultyParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> rawDifficulty = pattern.find(".DIFFICULTY");
        if(rawDifficulty != null) {
            return new DifficultySetCommand(file.getCompiler().getModule().minecraft.types.difficulty.get(rawDifficulty.flatten(false)));
        } else return new DifficultyQueryCommand();
    }
}

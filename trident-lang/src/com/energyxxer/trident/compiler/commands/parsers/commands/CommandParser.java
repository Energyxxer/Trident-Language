package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserGroup
public interface CommandParser {
    Command parse(TokenPattern<?> pattern, TridentFile file);
}

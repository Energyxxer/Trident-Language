package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserGroup
public interface Instruction {
    void run(TokenPattern<?> pattern, TridentFile file);
}

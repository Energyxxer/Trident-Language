package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserGroup
public interface ModifierParser {
    ExecuteModifier parse(TokenPattern<?> pattern, TridentFile file);
}

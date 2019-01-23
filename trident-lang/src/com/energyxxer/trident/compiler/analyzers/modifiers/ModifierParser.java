package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerGroup
public interface ModifierParser {
    ExecuteModifier parse(TokenPattern<?> pattern, TridentFile file);
}

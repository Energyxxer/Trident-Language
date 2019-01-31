package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

@AnalyzerGroup
public interface SimpleModifierParser extends ModifierParser {
    ExecuteModifier parseSingle(TokenPattern<?> pattern, TridentFile file);

    @Override
    default @NotNull Collection<ExecuteModifier> parse(TokenPattern<?> pattern, TridentFile file) {
        return Collections.singletonList(parseSingle(pattern, file));
    }
}

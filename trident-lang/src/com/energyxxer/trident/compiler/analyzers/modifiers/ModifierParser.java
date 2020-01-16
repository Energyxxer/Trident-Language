package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;

@AnalyzerGroup(
        classes="AlignParser,AnchoredParser,AsParser,AtParser,ConditionalParser,FacingParser,InParser,PositionedParser,RawParser,RotatedParser,StoreParser"
)
public interface ModifierParser {
    Collection<ExecuteModifier> parse(TokenPattern<?> pattern, ISymbolContext ctx);
}

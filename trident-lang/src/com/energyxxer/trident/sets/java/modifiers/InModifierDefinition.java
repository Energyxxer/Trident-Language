package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteInDimension;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.wrapper;

public class InModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"in"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.modifierHeader("in"),
                wrapper(productions.getOrCreateStructure("DIMENSION_ID"), (v, p, d) -> new ExecuteInDimension((Type) v))
        ).setSimplificationFunctionContentIndex(1);
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}

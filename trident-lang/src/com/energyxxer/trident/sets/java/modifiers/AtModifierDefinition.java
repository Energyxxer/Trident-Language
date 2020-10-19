package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.modifierHeader;

public class AtModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"at"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                modifierHeader("at"),
                productions.getOrCreateStructure("ENTITY")
        );
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new ExecuteAtEntity((Entity) pattern.find("ENTITY").evaluate(ctx));
    }
}

package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAlignment;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.ofType;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.SWIZZLE;

public class AlignModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"align"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.modifierHeader("align"),
                ofType(SWIZZLE).addTags("cspn:Axes").setName("SWIZZLE").setEvaluator((p, d) -> {
                    String flat = p.flatten(false);
                    return new boolean[]{flat.contains("x"), flat.contains("y"), flat.contains("z")};
                })
        );
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        boolean[] swizzle = (boolean[]) pattern.find("SWIZZLE").evaluate(ctx);
        return new ExecuteAlignment(swizzle[0], swizzle[1], swizzle[2]);
    }
}

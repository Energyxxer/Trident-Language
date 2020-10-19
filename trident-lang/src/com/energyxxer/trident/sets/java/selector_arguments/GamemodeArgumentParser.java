package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.GamemodeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class GamemodeArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"gamemode"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("gamemode").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), productions.getOrCreateStructure("GAMEMODE_ID")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Type gamemode = (Type) p.find("GAMEMODE_ID").evaluate(ctx);
                    return new GamemodeArgument(gamemode, p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}

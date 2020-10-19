package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.teammsg.TeamMessageCommand;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.TRAILING_STRING;

public class TeamMsgCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"teammsg", "tm"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                choice(TridentProductions.commandHeader("teammsg"), TridentProductions.commandHeader("tm")),
                ofType(TRAILING_STRING).setName("MESSAGE").addTags("cspn:Message").setEvaluator((p, d) -> p.flatten(false))
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = (String) pattern.find("MESSAGE").evaluate(ctx);

        try {
            return new TeamMessageCommand(message);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}

package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.say.SayCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.SAY_STRING;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.WHITESPACE;

public class SayCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"say"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("say"),
                ofType(WHITESPACE),
                list(
                        choice(
                                ofType(SAY_STRING).setEvaluator((p, d) -> p.flatten(false)),
                                group(TridentProductions.sameLine(), productions.getOrCreateStructure("SELECTOR")).setSimplificationFunctionContentIndex(1)
                        ).setName("SAY_PART")
                ).setName("SAY_MESSAGE").addTags("cspn:Message")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (TokenPattern<?> part : ((TokenList) pattern.find("SAY_MESSAGE")).getContents()) {
            sb.append(part.evaluate(ctx));
        }
        return new SayCommand(sb.toString());
    }
}
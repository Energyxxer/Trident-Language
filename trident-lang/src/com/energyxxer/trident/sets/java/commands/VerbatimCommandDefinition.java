package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.RawCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.VERBATIM_COMMAND;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.VERBATIM_COMMAND_HEADER;

public class VerbatimCommandDefinition implements PatternProviderUnit<ISymbolContext> {
    @Override
    public String[] getTargetProductionNames() {
        return new String[] {"COMMAND"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                ofType(VERBATIM_COMMAND_HEADER),
                TridentProductions.sameLine(),
                choice(
                        ofType(VERBATIM_COMMAND).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new RawCommand(p.flatten(false))),
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> null,
                                (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new RawCommand(v.toString()),
                                false,
                                String.class, Command.class
                        )
                ).setName("VERBATIM_COMMAND_INNER")
        ).setSimplificationFunctionFind("VERBATIM_COMMAND_INNER");
    }
}

package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadAddCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadRemoveAllCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadRemoveCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ForceloadCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"forceload"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("forceload"),
                choice(
                        group(
                                literal("add"),
                                wrapper(productions.getOrCreateStructure("TWO_COORDINATE_SET")).setName("CHUNK_FROM").addTags("cspn:From"),
                                wrapperOptional(productions.getOrCreateStructure("TWO_COORDINATE_SET")).setName("CHUNK_TO").addTags("cspn:To")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet from = (CoordinateSet) p.find("CHUNK_FROM").evaluate(ctx, null);
                            CoordinateSet to = (CoordinateSet) p.findThenEvaluate("CHUNK_TO", null, ctx, null);
                            return new ForceLoadAddCommand(from, to);
                        }),
                        group(
                                literal("query"),
                                wrapperOptional(
                                        productions.getOrCreateStructure("TWO_COORDINATE_SET")
                                ).setName("FORCELOAD_QUERY_COLUMN")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet queryColumn = (CoordinateSet) p.findThenEvaluate("FORCELOAD_QUERY_COLUMN", null, ctx, null);
                            return new ForceLoadQueryCommand(queryColumn);
                        }),
                        group(
                                literal("remove"),
                                choice(
                                        group(
                                                wrapper(productions.getOrCreateStructure("TWO_COORDINATE_SET")).setName("CHUNK_FROM").addTags("cspn:XZ Position 1"),
                                                wrapperOptional(productions.getOrCreateStructure("TWO_COORDINATE_SET")).setName("CHUNK_TO").addTags("cspn:XZ Position 2")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            CoordinateSet from = (CoordinateSet) p.find("CHUNK_FROM").evaluate(ctx, null);
                                            CoordinateSet to = (CoordinateSet) p.findThenEvaluate("CHUNK_TO", null, ctx, null);
                                            return new ForceLoadRemoveCommand(from, to);
                                        }),
                                        group(literal("all")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new ForceLoadRemoveAllCommand())
                                )
                        ).setSimplificationFunctionContentIndex(1)
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}

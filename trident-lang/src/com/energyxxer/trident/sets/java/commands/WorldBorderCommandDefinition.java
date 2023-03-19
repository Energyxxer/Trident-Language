package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.worldborder.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class WorldBorderCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"worldborder"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("worldborder"),
                choice(
                        literal("get").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderGetWidth()),
                        group(
                                choice(
                                        literal("add").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderAddDistance((double) d[0], (int) d[1])),
                                        literal("set").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetDistance((double) d[0], (int) d[1]))
                                ).setName("INNER"),
                                TridentProductions.real(productions).setName("DISTANCE").addTags("cspn:Distance"),
                                TridentProductions.integer(productions).setOptional().setName("TIME").addTags("cspn:Transition time")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            double distance = (double) p.find("DISTANCE").evaluate(ctx, null);
                            int time = (int) p.findThenEvaluate("TIME", 0, ctx, null);

                            try {
                                return p.find("INNER").evaluate(ctx, new Object[] {distance, time});
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map("DISTANCE", p.tryFind("DISTANCE"))
                                        .map("TIME", p.tryFind("TIME"))
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        group(
                                literal("center"),
                                wrapper(
                                        productions.getOrCreateStructure("TWO_COORDINATE_SET"),
                                        (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetCenter((CoordinateSet) v)
                                )
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("damage"),
                                choice(
                                        group(
                                                literal("amount"),
                                                TridentProductions.real(productions).setName("DAMAGE").addTags("cspn:Damage Per Block Per Second")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetDamageAmount((double) p.find("DAMAGE").evaluate(ctx, null))),
                                        group(
                                                literal("buffer"),
                                                TridentProductions.real(productions).setName("DISTANCE").addTags("cspn:Distance")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetDamageBuffer((double) p.find("DISTANCE").evaluate(ctx, null)))
                                )
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("warning"),
                                choice(
                                        group(
                                                literal("distance"),
                                                TridentProductions.integer(productions).setName("DISTANCE").addTags("cspn:Distance")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetWarningDistance((int) p.find("DISTANCE").evaluate(ctx, null))),
                                        group(
                                                literal("time"),
                                                TridentProductions.integer(productions).setName("TIME").addTags("cspn:Time")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new WorldBorderSetWarningTime((int) p.find("TIME").evaluate(ctx, null)))
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

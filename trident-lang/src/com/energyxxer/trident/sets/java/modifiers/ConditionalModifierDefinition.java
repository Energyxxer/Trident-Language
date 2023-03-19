package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreComparison;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.DataHolder;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.defaults.PredicateReference;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.CUSTOM_COMMAND_KEYWORD;

public class ConditionalModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"if", "unless"};
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        PostValidationPatternEvaluator<ISymbolContext> entityConditionEvaluator = (v, p, ctx, d) -> {
            try {
                return new ExecuteConditionEntity(
                        (ExecuteCondition.ConditionType) d[0],
                        (Entity) v
                );
            } catch(CommodoreException x) {
                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                        .map(CommodoreException.Source.ENTITY_ERROR, p)
                        .invokeThrow();
                return null;
            }
        };

        return group(
                choice(
                        TridentProductions.modifierHeader("if").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ExecuteCondition.ConditionType.IF),
                        TridentProductions.modifierHeader("unless").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ExecuteCondition.ConditionType.UNLESS)
                ).setName("HEADER"),
                choice(
                        wrapper(productions.getOrCreateStructure("SELECTOR"), d -> null, entityConditionEvaluator),
                        group(
                                literal("entity"),
                                wrapper(productions.getOrCreateStructure("ENTITY"), d -> null, entityConditionEvaluator)
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("predicate"),
                                TridentProductions.noToken().addTags("cspn:Predicate"),
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION"), d -> null, (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    PredicateReference predicate = new PredicateReference(
                                            ctx.get(SetupModuleTask.INSTANCE).getNamespace(((ResourceLocation)v).namespace),
                                            ((ResourceLocation)v).body
                                    );
                                    return new ExecuteConditionPredicate((ExecuteCondition.ConditionType) d[0], predicate);
                                })
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("block"),
                                productions.getOrCreateStructure("COORDINATE_SET"),
                                productions.getOrCreateStructure("BLOCK_TAGGED")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);
                            Block block = (Block) p.find("BLOCK_TAGGED").evaluate(ctx, null);
                            return new ExecuteConditionBlock((ExecuteCondition.ConditionType) d[0], pos, block);
                        }),
                        group(
                                literal("score"),
                                productions.getOrCreateStructure("SCORE"),
                                choice(
                                        matchItem(CUSTOM_COMMAND_KEYWORD, "isset").setName("ISSET").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            try {
                                                return new ExecuteConditionScoreMatch(
                                                        (ExecuteCondition.ConditionType) d[0],
                                                        (LocalScore) d[1],
                                                        new IntegerRange(null, null)
                                                );
                                            } catch(CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                        .map("TARGET_ENTITY", (TokenPattern<?>) d[2])
                                                        .invokeThrow();
                                                return null;
                                            }
                                        }),
                                        group(
                                                choice(
                                                        TridentProductions.symbol("<").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ScoreComparison.LESS_THAN),
                                                        TridentProductions.symbol("<=").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ScoreComparison.LESS_THAN_EQUAL),
                                                        TridentProductions.symbol("=").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ScoreComparison.EQUAL),
                                                        TridentProductions.symbol(">=").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ScoreComparison.GREATER_THAN_EQUAL),
                                                        TridentProductions.symbol(">").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ScoreComparison.GREATER_THAN)
                                                ).setName("OPERATOR"),
                                                productions.getOrCreateStructure("SCORE")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            ScoreComparison operator = (ScoreComparison) p.find("OPERATOR").evaluate(ctx, null);
                                            LocalScore otherScore = (LocalScore) p.find("SCORE").evaluate(ctx, null);
                                            try {
                                                return new ExecuteConditionScoreComparison(
                                                        (ExecuteCondition.ConditionType) d[0],
                                                        (LocalScore) d[1],
                                                        operator,
                                                        otherScore
                                                );
                                            } catch(CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                        .map("TARGET_ENTITY", (TokenPattern<?>) d[2])
                                                        .map("SOURCE_ENTITY", p.tryFind("SCORE"))
                                                        .invokeThrow();
                                                return null;
                                            }
                                        }),
                                        group(
                                                literal("matches"),
                                                wrapper(productions.getOrCreateStructure("INTEGER_NUMBER_RANGE"), d -> null, (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                                    try {
                                                        return new ExecuteConditionScoreMatch(
                                                                (ExecuteCondition.ConditionType) d[0],
                                                                (LocalScore) d[1],
                                                                (IntegerRange) v
                                                        );
                                                    } catch(CommodoreException x) {
                                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                                .map("TARGET_ENTITY", (TokenPattern<?>) d[2])
                                                                .invokeThrow();
                                                        return null;
                                                    }
                                                })
                                        ).setSimplificationFunctionContentIndex(1)
                                ).setName("INNER")
                        ).setSimplificationFunction(d -> {
                            TokenPattern<?> pattern = d.pattern;
                            ISymbolContext ctx = (ISymbolContext) d.ctx;
                            ExecuteCondition.ConditionType conditionType = (ExecuteCondition.ConditionType) d.data[0];

                            d.unlock(); d = null;

                            TokenPattern<?> scorePattern = pattern.find("SCORE");

                            LocalScore score = (LocalScore) scorePattern.evaluate(ctx, null);
                            TokenPattern.SimplificationDomain.get(pattern.find("INNER"), ctx, new Object[] {conditionType, score, scorePattern});
                        }),
                        group(literal("blocks"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("FROM").addTags("cspn:From"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TO").addTags("cspn:To"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TEMPLATE").addTags("cspn:Template"),
                                enumChoice(ExecuteConditionRegion.AirPolicy.class).setName("AIR_POLICY")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            return new ExecuteConditionRegion(
                                    (ExecuteCondition.ConditionType) d[0],
                                    (CoordinateSet) p.find("FROM").evaluate(ctx, null),
                                    (CoordinateSet) p.find("TO").evaluate(ctx, null),
                                    (CoordinateSet) p.find("TEMPLATE").evaluate(ctx, null),
                                    (ExecuteConditionRegion.AirPolicy) p.find("AIR_POLICY").evaluate(ctx, null)
                            );
                        }),
                        group(literal("data"),
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                productions.getOrCreateStructure("NBT_PATH")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            try {
                                return new ExecuteConditionDataHolder(
                                        (ExecuteCondition.ConditionType) d[0],
                                        (DataHolder) p.find("DATA_HOLDER").evaluate(ctx, null),
                                        (NBTPath) p.find("NBT_PATH").evaluate(ctx, null)
                                );
                            } catch(CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.find("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                return null;
                            }
                        })
                ).setName("INNER")
        ).setSimplificationFunction(d -> {
            TokenPattern<?> pattern = d.pattern;
            ISymbolContext ctx = (ISymbolContext) d.ctx;

            d.unlock(); d = null;
            ExecuteCondition.ConditionType conditionType = (ExecuteCondition.ConditionType) pattern.find("HEADER").evaluate(ctx, null);

            TokenPattern.SimplificationDomain.get(pattern.find("INNER"), ctx, new Object[] {conditionType});
        });
    }
}

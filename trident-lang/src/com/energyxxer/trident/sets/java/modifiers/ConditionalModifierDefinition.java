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
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.CUSTOM_COMMAND_KEYWORD;
import static com.energyxxer.prismarine.PrismarineProductions.*;

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
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        PostValidationPatternEvaluator entityConditionEvaluator = (v, p, d) -> {
            try {
                return new ExecuteConditionEntity(
                        (ExecuteCondition.ConditionType) d[1],
                        (Entity) v
                );
            } catch(CommodoreException x) {
                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                        .map(CommodoreException.Source.ENTITY_ERROR, p)
                        .invokeThrow();
                return null;
            }
        };

        return group(
                choice(
                        TridentProductions.modifierHeader("if").setEvaluator((p, d) -> ExecuteCondition.ConditionType.IF),
                        TridentProductions.modifierHeader("unless").setEvaluator((p, d) -> ExecuteCondition.ConditionType.UNLESS)
                ).setName("HEADER"),
                choice(
                        wrapper(productions.getOrCreateStructure("SELECTOR"), d -> new Object[] {d[0]}, entityConditionEvaluator),
                        group(
                                literal("entity"),
                                wrapper(productions.getOrCreateStructure("ENTITY"), d -> new Object[] {d[0]}, entityConditionEvaluator)
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("predicate"),
                                TridentProductions.noToken().addTags("cspn:Predicate"),
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION"), d -> new Object[] {d[0]}, (v, p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    PredicateReference predicate = new PredicateReference(
                                            ctx.get(SetupModuleTask.INSTANCE).getNamespace(((ResourceLocation)v).namespace),
                                            ((ResourceLocation)v).body
                                    );
                                    return new ExecuteConditionPredicate((ExecuteCondition.ConditionType) d[1], predicate);
                                })
                        ).setSimplificationFunctionContentIndex(1),
                        group(
                                literal("block"),
                                productions.getOrCreateStructure("COORDINATE_SET"),
                                productions.getOrCreateStructure("BLOCK_TAGGED")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                            Block block = (Block) p.find("BLOCK_TAGGED").evaluate(ctx);
                            return new ExecuteConditionBlock((ExecuteCondition.ConditionType) d[1], pos, block);
                        }),
                        group(
                                literal("score"),
                                productions.getOrCreateStructure("SCORE"),
                                choice(
                                        matchItem(CUSTOM_COMMAND_KEYWORD, "isset").setName("ISSET").setEvaluator((p, d) -> {
                                            try {
                                                return new ExecuteConditionScoreMatch(
                                                        (ExecuteCondition.ConditionType) d[1],
                                                        (LocalScore) d[2],
                                                        new IntegerRange(null, null)
                                                );
                                            } catch(CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                        .map("TARGET_ENTITY", (TokenPattern<?>) d[3])
                                                        .invokeThrow();
                                                return null;
                                            }
                                        }),
                                        group(
                                                choice(
                                                        TridentProductions.symbol("<").setEvaluator((p, d) -> ScoreComparison.LESS_THAN),
                                                        TridentProductions.symbol("<=").setEvaluator((p, d) -> ScoreComparison.LESS_THAN_EQUAL),
                                                        TridentProductions.symbol("=").setEvaluator((p, d) -> ScoreComparison.EQUAL),
                                                        TridentProductions.symbol(">=").setEvaluator((p, d) -> ScoreComparison.GREATER_THAN_EQUAL),
                                                        TridentProductions.symbol(">").setEvaluator((p, d) -> ScoreComparison.GREATER_THAN)
                                                ).setName("OPERATOR"),
                                                productions.getOrCreateStructure("SCORE")
                                        ).setEvaluator((p, d) -> {
                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                            ScoreComparison operator = (ScoreComparison) p.find("OPERATOR").evaluate();
                                            LocalScore otherScore = (LocalScore) p.find("SCORE").evaluate(ctx);
                                            try {
                                                return new ExecuteConditionScoreComparison(
                                                        (ExecuteCondition.ConditionType) d[1],
                                                        (LocalScore) d[2],
                                                        operator,
                                                        otherScore
                                                );
                                            } catch(CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                        .map("TARGET_ENTITY", (TokenPattern<?>) d[3])
                                                        .map("SOURCE_ENTITY", p.tryFind("SCORE"))
                                                        .invokeThrow();
                                                return null;
                                            }
                                        }),
                                        group(
                                                literal("matches"),
                                                wrapper(productions.getOrCreateStructure("INTEGER_NUMBER_RANGE"), d -> new Object[] {d[0]}, (v, p, d) -> {
                                                    try {
                                                        return new ExecuteConditionScoreMatch(
                                                                (ExecuteCondition.ConditionType) d[1],
                                                                (LocalScore) d[2],
                                                                (IntegerRange) v
                                                        );
                                                    } catch(CommodoreException x) {
                                                        TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                                .map("TARGET_ENTITY", (TokenPattern<?>) d[3])
                                                                .invokeThrow();
                                                        return null;
                                                    }
                                                })
                                        ).setSimplificationFunctionContentIndex(1)
                                ).setName("INNER")
                        ).setSimplificationFunction(d -> {
                            ISymbolContext ctx = (ISymbolContext) d.data[0];
                            ExecuteCondition.ConditionType conditionType = (ExecuteCondition.ConditionType) d.data[1];
                            TokenPattern<?> scorePattern = d.pattern.find("SCORE");
                            LocalScore score = (LocalScore) scorePattern.evaluate(ctx);
                            d.pattern = d.pattern.find("INNER");
                            d.data = new Object[] {ctx, conditionType, score, scorePattern};
                        }),
                        group(literal("blocks"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("FROM").addTags("cspn:From"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TO").addTags("cspn:To"),
                                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TEMPLATE").addTags("cspn:Template"),
                                enumChoice(ExecuteConditionRegion.AirPolicy.class).setName("AIR_POLICY")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            return new ExecuteConditionRegion(
                                    (ExecuteCondition.ConditionType) d[1],
                                    (CoordinateSet) p.find("FROM").evaluate(ctx),
                                    (CoordinateSet) p.find("TO").evaluate(ctx),
                                    (CoordinateSet) p.find("TEMPLATE").evaluate(ctx),
                                    (ExecuteConditionRegion.AirPolicy) p.find("AIR_POLICY").evaluate()
                            );
                        }),
                        group(literal("data"),
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                productions.getOrCreateStructure("NBT_PATH")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            try {
                                return new ExecuteConditionDataHolder(
                                        (ExecuteCondition.ConditionType) d[1],
                                        (DataHolder) p.find("DATA_HOLDER").evaluate(ctx),
                                        (NBTPath) p.find("NBT_PATH").evaluate(ctx)
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
            ISymbolContext ctx = (ISymbolContext) d.data[0];
            ExecuteCondition.ConditionType conditionType = (ExecuteCondition.ConditionType) d.pattern.find("HEADER").evaluate();
            d.pattern = d.pattern.find("INNER");
            d.data = new Object[] {ctx, conditionType};
        });
    }
}

package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.defaults.BossbarReference;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class StoreModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"store"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenPatternMatch optionalType = wrapperOptional(productions.getOrCreateStructure("NUMERIC_NBT_TYPE")).setName("NUMERIC_TYPE");
        return group(
                TridentProductions.modifierHeader("store"),
                enumChoice(ExecuteStore.StoreValue.class).setName("STORE_VALUE"),
                choice(
                        group(
                                literal("bossbar"),
                                TridentProductions.noToken().addTags("cspn:Bossbar"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION"),
                                enumChoice(ExecuteStoreBossbar.BossbarVariable.class).setName("BOSSBAR_VARIABLE")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ExecuteStore.StoreValue storeValue = (ExecuteStore.StoreValue) d[1];

                            ResourceLocation bossbarLoc = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx);
                            ExecuteStoreBossbar.BossbarVariable bossbarVariable = (ExecuteStoreBossbar.BossbarVariable) p.find("BOSSBAR_VARIABLE").evaluate();

                            BossbarReference bossbar = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(bossbarLoc.namespace), bossbarLoc.body);

                            return new ExecuteStoreBossbar(storeValue, bossbar, bossbarVariable);
                        }),
                        group(
                                productions.getOrCreateStructure("DATA_HOLDER"),
                                productions.getOrCreateStructure("NBT_PATH"),
                                optionalType,
                                wrapperOptional(TridentProductions.real(productions).addTags("cspn:Scale")).setName("SCALE")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ExecuteStore.StoreValue storeValue = (ExecuteStore.StoreValue) d[1];

                            DataHolder holder = (DataHolder) p.find("DATA_HOLDER").evaluate(ctx);
                            NBTPath path = (NBTPath) p.find("NBT_PATH").evaluate(ctx);

                            Object unboxedHolder =
                                    holder instanceof DataHolderEntity ?
                                            ((DataHolderEntity) holder).getEntity() :
                                            holder instanceof DataHolderBlock ?
                                                    ((DataHolderBlock) holder).getPos() :
                                                    holder instanceof DataHolderStorage ?
                                                            new ResourceLocation(((DataHolderStorage) holder).getTarget())
                                                            : null;

                            NumericNBTType type = parseNumericType(p.find("NUMERIC_TYPE"), unboxedHolder, path, ctx, p, true);
                            double scale = (double) p.findThenEvaluate("SCALE", 1.0, ctx);
                            try {
                                return new ExecuteStoreDataHolder(storeValue, holder, path, type, scale);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("DATA_HOLDER.ENTITY"))
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        group(
                                literal("score"),
                                productions.getOrCreateStructure("SCORE")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ExecuteStore.StoreValue storeValue = (ExecuteStore.StoreValue) d[1];
                            LocalScore score = (LocalScore) p.find("SCORE").evaluate(ctx);
                            return new ExecuteStoreScore(storeValue, score);
                        })
                ).setName("INNER")
        ).setSimplificationFunction(d -> {
            ISymbolContext ctx = (ISymbolContext) d.data[0];
            ExecuteStore.StoreValue storeValue = (ExecuteStore.StoreValue) d.pattern.find("STORE_VALUE").evaluate();
            d.pattern = d.pattern.find("INNER");
            d.data = new Object[] {ctx, storeValue};
        });
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }

    public static NumericNBTType parseNumericType(TokenPattern<?> pattern, Object body, NBTPath path, ISymbolContext ctx, TokenPattern<?> outer, boolean strict) {
        if(pattern == null) {
            return CommonParsers.getNumericType(body, path, ctx, outer, strict);
        }
        return (NumericNBTType) pattern.evaluate(ctx);
    }
}

package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteRotatedAsEntity;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class RotatedModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"rotated"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.modifierHeader("rotated"),
                choice(
                        group(
                                literal("as").setOptional(),
                                productions.getOrCreateStructure("ENTITY")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            try {
                                return new ExecuteRotatedAsEntity(entity);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        productions.getOrCreateStructure("ROTATION"),
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> new Object[] {d[0]},
                                (v, p, d) -> {
                                    if (v instanceof Rotation) return v;
                                    try {
                                        return new ExecuteRotatedAsEntity((Entity) v);
                                    } catch (CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                .invokeThrow();
                                        return null;
                                    }
                                },
                                false,
                                Entity.class,
                                Rotation.class
                        )
                ).addTags("cspn:Rotation")
        ).setSimplificationFunctionContentIndex(1);
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}

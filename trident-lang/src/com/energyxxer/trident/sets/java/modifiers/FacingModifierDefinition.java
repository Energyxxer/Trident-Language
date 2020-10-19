package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.EntityAnchor;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteFacingBlock;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteFacingEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class FacingModifierDefinition implements SimpleExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"facing"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.modifierHeader("facing"),
                choice(
                        group(
                                literal("entity").setOptional(),
                                productions.getOrCreateStructure("ENTITY"),
                                wrapperOptional(productions.getOrCreateStructure("ANCHOR")).setName("ANCHOR")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            EntityAnchor anchor = (EntityAnchor) p.findThenEvaluate("ANCHOR", EntityAnchor.FEET, ctx);
                            try {
                                return new ExecuteFacingEntity(entity, anchor);
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        wrapper(productions.getOrCreateStructure("COORDINATE_SET"), (v, p, d) -> new ExecuteFacingBlock((CoordinateSet) v))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}

package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.literal;

public class ComponentArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"component"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("component").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(
                        TridentProductions.not().setOptional(),
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_VALUE"),
                                false,
                                CustomEntity.class
                        ).setName("COMPONENT_NAME")
                ).setName("SELECTOR_ARGUMENT_VALUE").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    CustomEntity component = (CustomEntity) p.find("COMPONENT_NAME").evaluate(ctx, null);
                    if(!component.isComponent()) {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected entity component, instead got custom entity", p, ctx);
                    }
                    return new TagArgument(component.getIdTag(), p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}

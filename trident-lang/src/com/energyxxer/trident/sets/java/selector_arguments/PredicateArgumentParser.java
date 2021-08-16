package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.PredicateArgument;
import com.energyxxer.commodore.types.defaults.PredicateReference;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.literal;

public class PredicateArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"predicate"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("predicate").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), TridentProductions.noToken().addTags("cspn:Predicate"), productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    ResourceLocation reference = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx);

                    return new PredicateArgument(new PredicateReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(reference.namespace), reference.body), p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}

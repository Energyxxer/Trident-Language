package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import java.util.ArrayList;

import static com.energyxxer.prismarine.PrismarineProductions.choice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class TypeArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"type"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                choice("type").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), productions.getOrCreateStructure("TRIDENT_ENTITY_ID_TAGGED")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];

                    boolean negated = p.find("NEGATED") != null;

                    Object reference = p.find("TRIDENT_ENTITY_ID_TAGGED").evaluate(ctx);

                    ArrayList<SelectorArgument> args = new ArrayList<>();

                    if(reference instanceof Type) {
                        args.add(new TypeArgument((Type) reference, negated));
                    } else if(reference instanceof CustomEntity) {
                        CustomEntity ce = (CustomEntity) reference;
                        if(ce.getBaseType() != null && !negated) args.add(new TypeArgument(ce.getBaseType()));

                        args.add(new TagArgument(ce.getIdTag(), negated));
                    } else {
                        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), p.tryFind("TRIDENT_ENTITY_ID_TAGGED"), ctx);
                    }

                    if(args.size() == 1) return args.get(0);
                    return args;
                })
        ).setSimplificationFunctionContentIndex(2);
    }

    public static SelectorArgument[] getFilterForCustomEntity(CustomEntity ce) {
        return getFilterForCustomEntity(ce, false);
    }

    public static SelectorArgument[] getFilterForCustomEntity(CustomEntity ce, boolean negated) {
        ArrayList<SelectorArgument> args = new ArrayList<>();
        if(ce.getBaseType() != null && !negated) {
            args.add(new TypeArgument(ce.getBaseType()));
        }
        args.add(new TagArgument(ce.getIdTag(), negated));
        return args.toArray(new SelectorArgument[0]);
    }
}
package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.SENDER;

@AnalyzerMember(key = "type")
public class TypeArgumentParser implements SelectorArgumentParser {
    @Override
    public Collection<SelectorArgument> parse(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        boolean negated = pattern.find("NEGATED") != null;
        ArrayList<SelectorArgument> args = new ArrayList<>();

        Object reference = CommonParsers.parseEntityReference(pattern.find("ENTITY_ID_TAGGED"), ctx);

        if(reference instanceof Type) {
            args.add(new TypeArgument((Type) reference, negated));
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            if(ce.getBaseType() != null && !negated) args.add(new TypeArgument(ce.getBaseType()));

            args.add(new TagArgument(ce.getIdTag(), negated));
        } else {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID"), ctx);
        }

        return args;
    }

    public static Entity getFilterForCustomEntity(CustomEntity ce) {
        return getFilterForCustomEntity(ce, false);
    }

    public static Entity getFilterForCustomEntity(CustomEntity ce, boolean negated) {
        Selector sel = new Selector(SENDER);
        if(ce.getBaseType() != null) {
            sel.addArgument(new TypeArgument(ce.getBaseType(), negated));
        }
        sel.addArgument(new TagArgument(ce.getIdTag(), negated));
        return sel;
    }
}
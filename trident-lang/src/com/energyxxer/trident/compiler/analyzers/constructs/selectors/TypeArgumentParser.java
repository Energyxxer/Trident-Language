package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import java.util.ArrayList;
import java.util.Collection;

import static com.energyxxer.commodore.functionlogic.selector.Selector.BaseSelector.ALL_ENTITIES;

@AnalyzerMember(key = "type")
public class TypeArgumentParser implements SelectorArgumentParser {
    @Override
    public Collection<SelectorArgument> parse(TokenPattern<?> pattern, TridentFile file) {
        boolean negated = pattern.find("NEGATED") != null;
        ArrayList<SelectorArgument> args = new ArrayList<>();

        Object reference = CommonParsers.parseEntityReference(pattern.find("ENTITY_ID_TAGGED"), file);

        if(reference instanceof Type) {
            args.add(new TypeArgument((Type) reference, negated));
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            args.add(new TypeArgument(ce.getDefaultType(), negated));

            args.add(new TagArgument(ce.getIdTag(), negated));
        } else {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID"), file);
        }

        return args;
    }

    public static Entity getSelectorForCustomEntity(CustomEntity ce) {
        return getSelectorForCustomEntity(ce, false);
    }

    public static Entity getSelectorForCustomEntity(CustomEntity ce, boolean negated) {
        return new Selector(ALL_ENTITIES, new TypeArgument(ce.getDefaultType(), negated), new TagArgument(ce.getIdTag(), negated));
    }
}
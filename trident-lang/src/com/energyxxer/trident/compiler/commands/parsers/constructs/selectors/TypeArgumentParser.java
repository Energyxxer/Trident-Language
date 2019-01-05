package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

import java.util.ArrayList;
import java.util.Collection;

@ParserMember(key = "type")
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
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), pattern.find("ENTITY_ID")));
            throw new EntryParsingException();
        }

        return args;
    }

    public static Entity getSelectorForCustomEntity(CustomEntity ce) {
        return getSelectorForCustomEntity(ce, false);
    }

    public static Entity getSelectorForCustomEntity(CustomEntity ce, boolean negated) {
        return new GenericEntity(new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(ce.getDefaultType(), negated), new TagArgument(ce.getIdTag(), negated)));
    }
}
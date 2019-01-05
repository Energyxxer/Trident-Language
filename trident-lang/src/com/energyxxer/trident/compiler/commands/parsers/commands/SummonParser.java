package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

@ParserMember(key = "summon")
public class SummonParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> id = pattern.find("ENTITY_ID");
        Type type = null;
        CoordinateSet pos = CoordinateParser.parse(pattern.find(".COORDINATE_SET"), file);
        TagCompound nbt = NBTParser.parseCompound(pattern.find("..NBT_COMPOUND"), file);
        if(nbt != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, type);
            NBTParser.analyzeTag(nbt, context, pattern.find("..NBT_COMPOUND"), file);
        }

        Object reference = CommonParsers.parseEntityReference(id, file);

        if(reference instanceof Type) {
            type = (Type) reference;
        } else if(reference instanceof CustomEntity) {
            CustomEntity ce = (CustomEntity) reference;
            type = ce.getDefaultType();

            if(nbt == null) nbt = new TagCompound();
            try {
                nbt = ce.getDefaultNBT().merge(nbt);
            } catch(IllegalArgumentException x) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Error while merging given NBT with custom entity's NBT: " + x.getMessage(), pattern));
            }
        } else {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown entity reference return type: " + reference.getClass().getSimpleName(), id));
            throw new EntryParsingException();
        }

        if(pos == null && nbt != null) pos = new CoordinateSet();

        return new SummonCommand(type, pos, nbt);
    }
}
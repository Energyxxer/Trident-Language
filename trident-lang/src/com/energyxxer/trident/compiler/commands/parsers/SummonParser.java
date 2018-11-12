package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@CommandParserAnnotation(headerCommand = "summon")
public class SummonParser implements CommandParser {
    @Override
    public void parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> id = pattern.find("ENTITY_ID");
        EntityType type = CommonParsers.parseEntityType(id, file.getCompiler().getModule());
        CoordinateSet pos = CoordinateParser.parse(pattern.find(".COORDINATE_SET"));
        TagCompound nbt = NBTParser.parseCompound(pattern.find("..NBT_COMPOUND"));
        file.getFunction().append(new SummonCommand(type, pos, nbt));
    }
}
package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemBlockCommand;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemEntityCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "replaceitem")
public class ReplaceItemParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Type slot = file.getCompiler().getModule().minecraft.types.slot.get(pattern.find("SLOT_ID").flatten(false));
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), file.getCompiler());

        TokenPattern<?> rawCoords = pattern.find("TARGET.COORDINATE_SET");
        if(rawCoords != null) {
            return new ReplaceItemBlockCommand(CoordinateParser.parse(rawCoords, file.getCompiler()), slot, item);
        } else return new ReplaceItemEntityCommand(EntityParser.parseEntity(pattern.find("TARGET.ENTITY"), file.getCompiler()), slot, item);
    }
}

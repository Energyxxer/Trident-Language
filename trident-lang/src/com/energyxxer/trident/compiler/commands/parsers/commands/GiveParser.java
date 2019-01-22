package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.give.GiveCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

@ParserMember(key = "give")
public class GiveParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), file, NBTMode.SETTING);
        TokenPattern<?> amountPattern = pattern.find("AMOUNT");
        int amount = amountPattern != null ? CommonParsers.parseInt(amountPattern, file) : 1;

        if(!item.getItemType().isStandalone()) {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Item tags aren't allowed in this context", pattern.find("ITEM"), file);
        }

        return new GiveCommand(EntityParser.parseEntity(pattern.find("ENTITY"), file), item, amount);
    }
}

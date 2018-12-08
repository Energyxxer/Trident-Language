package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "clear")
public class ClearParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Item item = CommonParsers.parseItem(pattern.find("..ITEM_TAGGED"), file.getCompiler());
        TokenPattern<?> amountPattern = pattern.find("..AMOUNT");
        int amount = amountPattern != null ? CommonParsers.parseInt(amountPattern, file.getCompiler()) : -1;
        return new ClearCommand(EntityParser.parseEntity(pattern.find(".ENTITY"), file.getCompiler()), item, amount);
    }
}
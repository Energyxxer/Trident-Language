package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.give.GiveCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "give")
public class GiveParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), file.getCompiler());
        TokenPattern<?> amountPattern = pattern.find("AMOUNT");
        int amount = amountPattern != null ? Integer.parseInt(amountPattern.flattenTokens().get(0).value) : 1;

        return new GiveCommand(EntityParser.parseEntity(pattern.find("ENTITY"), file.getCompiler()), item, amount);
    }
}

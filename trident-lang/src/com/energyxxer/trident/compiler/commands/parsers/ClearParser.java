package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "clear")
public class ClearParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Item item = CommonParsers.parseItem(pattern.find("..ITEM_TAGGED"), file.getCompiler());
        TokenPattern<?> amountPattern = pattern.find("..AMOUNT");
        int amount = amountPattern != null ? Integer.parseInt(amountPattern.flattenTokens().get(0).value) : 1;
        return new ClearCommand(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)), item, amount);
    }
}
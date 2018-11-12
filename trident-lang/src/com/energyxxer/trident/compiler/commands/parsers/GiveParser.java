package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.give.GiveCommand;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@CommandParserAnnotation(headerCommand = "give")
public class GiveParser implements CommandParser {
    @Override
    public void parse(TokenPattern<?> pattern, TridentFile file) {
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), file.getCompiler().getModule());
        TokenPattern<?> amountPattern = pattern.find("AMOUNT");
        int amount = amountPattern != null ? Integer.parseInt(amountPattern.flattenTokens().get(0).value) : 1;
        file.getFunction().append(new GiveCommand(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)), item, amount));
    }
}

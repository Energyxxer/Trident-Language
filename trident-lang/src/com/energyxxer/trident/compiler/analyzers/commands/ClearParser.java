package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clear.ClearCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

@AnalyzerMember(key = "clear")
public class ClearParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Item item = CommonParsers.parseItem(pattern.find("..ITEM_TAGGED"), ctx, NBTMode.TESTING);
        TokenPattern<?> amountPattern = pattern.find("..AMOUNT");
        int amount = amountPattern != null ? CommonParsers.parseInt(amountPattern, ctx) : -1;
        try {
            return new ClearCommand(EntityParser.parseEntity(pattern.find(".ENTITY"), ctx), item, amount);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind(".ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("..AMOUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("..ITEM_TAGGED"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
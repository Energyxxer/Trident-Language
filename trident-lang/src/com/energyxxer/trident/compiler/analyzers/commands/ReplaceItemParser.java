package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemBlockCommand;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemEntityCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ItemSlot;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "replaceitem")
public class ReplaceItemParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Type slot = CommonParsers.parseType(pattern.find("SLOT_ID"), ctx, ItemSlot.CATEGORY);
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), ctx, NBTMode.SETTING);
        int count = 1;

        if(pattern.find("COUNT") != null) {
            count = CommonParsers.parseInt(pattern.find("COUNT"), ctx);
        }

        TokenPattern<?> rawCoords = pattern.find("TARGET.COORDINATE_SET");
        try {
            if(rawCoords != null) {
                return new ReplaceItemBlockCommand(CoordinateParser.parse(rawCoords, ctx), slot, item, count);
            } else return new ReplaceItemEntityCommand(EntityParser.parseEntity(pattern.find("TARGET.ENTITY"), ctx), slot, item, count);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("TARGET.ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("COUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("ITEM"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}

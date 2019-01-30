package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemBlockCommand;
import com.energyxxer.commodore.functionlogic.commands.replaceitem.ReplaceItemEntityCommand;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

@AnalyzerMember(key = "replaceitem")
public class ReplaceItemParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Type slot = file.getCompiler().getModule().minecraft.types.slot.get(pattern.find("SLOT_ID").flatten(false));
        Item item = CommonParsers.parseItem(pattern.find("ITEM"), file, NBTMode.SETTING);
        int count = 1;

        if(pattern.find("COUNT") != null) {
            count = CommonParsers.parseInt(pattern.find("COUNT"), file);
        }

        TokenPattern<?> rawCoords = pattern.find("TARGET.COORDINATE_SET");
        try {
            if(rawCoords != null) {
                return new ReplaceItemBlockCommand(CoordinateParser.parse(rawCoords, file), slot, item, count);
            } else return new ReplaceItemEntityCommand(EntityParser.parseEntity(pattern.find("TARGET.ENTITY"), file), slot, item, count);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("TARGET.ENTITY"))
                    .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.find("COUNT"))
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.find("ITEM"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, file);
        }
    }
}

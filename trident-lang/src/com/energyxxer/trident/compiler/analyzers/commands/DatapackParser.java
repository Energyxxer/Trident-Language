package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackDisableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackEnableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackListCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "datapack")
public class DatapackParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "DATAPACK_LIST": {
                DataPackListCommand.Filter filter = null;
                if(inner.find("DATAPACK_FILTER") != null) {
                    switch(inner.find("DATAPACK_FILTER").flatten(false)) {
                        case "enabled": filter = DataPackListCommand.Filter.ENABLED; break;
                        case "available": filter = DataPackListCommand.Filter.AVAILABLE; break;
                    }
                }
                return new DataPackListCommand(filter);
            }
            case "DATAPACK_DISABLE": {
                String datapack = CommonParsers.parseStringLiteralOrIdentifierA(inner.find("STRING_LITERAL_OR_IDENTIFIER_A"), ctx);
                return new DataPackDisableCommand(datapack);
            }
            case "DATAPACK_ENABLE": {
                String datapack = CommonParsers.parseStringLiteralOrIdentifierA(inner.find("STRING_LITERAL_OR_IDENTIFIER_A"), ctx);

                TokenPattern<?> rawOrder = inner.find("CHOICE");
                DataPackEnableCommand.Order order;
                if(rawOrder != null) {
                    rawOrder = ((TokenStructure) rawOrder).getContents();

                    String secondPack = null;

                    switch(rawOrder.getName()) {
                        case "DATAPACK_ENABLE_FIRST": order = DataPackEnableCommand.Order.FIRST; break;
                        case "DATAPACK_ENABLE_LAST": order = DataPackEnableCommand.Order.LAST; break;
                        case "DATAPACK_ENABLE_BEFORE": order = DataPackEnableCommand.Order.BEFORE; break;
                        case "DATAPACK_ENABLE_AFTER": order = DataPackEnableCommand.Order.AFTER; break;
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + rawOrder.getName() + "'", rawOrder, ctx);
                        }
                    }

                    if(order == DataPackEnableCommand.Order.BEFORE || order == DataPackEnableCommand.Order.AFTER) {
                        secondPack = CommonParsers.parseStringLiteralOrIdentifierA(rawOrder.find("STRING_LITERAL_OR_IDENTIFIER_A"), ctx);
                    }

                    return new DataPackEnableCommand(datapack, order, secondPack);
                } else {
                    return new DataPackEnableCommand(datapack);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}

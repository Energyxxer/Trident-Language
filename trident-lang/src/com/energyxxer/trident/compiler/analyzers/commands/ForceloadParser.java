package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadAddCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadRemoveAllCommand;
import com.energyxxer.commodore.functionlogic.commands.chunk.ForceLoadRemoveCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "forceload")
public class ForceloadParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "FORCELOAD_ADD": {
                CoordinateSet from = CoordinateParser.parse(inner.find("TWO_COORDINATE_SET"), ctx);
                CoordinateSet to = CoordinateParser.parse(inner.find("CHUNK_TO.TWO_COORDINATE_SET"), ctx);

                return new ForceLoadAddCommand(from, to);
            }
            case "FORCELOAD_QUERY": {
                CoordinateSet column = CoordinateParser.parse(inner.find("FORCELOAD_QUERY_COLUMN.TWO_COORDINATE_SET"), ctx);

                return new ForceLoadQueryCommand(column);
            }
            case "FORCELOAD_REMOVE": {
                inner = ((TokenStructure)inner.find("CHOICE")).getContents();
                switch(inner.getName()) {
                    case "FORCELOAD_REMOVE_ONE": {
                        CoordinateSet from = CoordinateParser.parse(inner.find("TWO_COORDINATE_SET"), ctx);
                        CoordinateSet to = CoordinateParser.parse(inner.find("CHUNK_TO.TWO_COORDINATE_SET"), ctx);
                        return new ForceLoadRemoveCommand(from, to);
                    }
                    case "FORCELOAD_REMOVE_ALL": {
                        return new ForceLoadRemoveAllCommand();
                    }
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
                    }
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}

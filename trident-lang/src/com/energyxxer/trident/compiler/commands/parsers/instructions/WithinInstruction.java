package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "within")
public class WithinInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {

        SymbolTable table = new SymbolTable(file);
        file.getCompiler().getSymbolStack().push(table);

        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false));
        table.put(symbol);

        CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"), file);
        CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"), file);

        double step = 1;
        if(pattern.find("STEP") != null) {
            step = CommonParsers.parseDouble(pattern.find("STEP.REAL"), file);
            if(step <= 0) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Within step must be positive", pattern.find("STEP")));
                throw new EntryParsingException();
            }
        }

        if(
                from.getX().getType() != to.getX().getType() ||
                        from.getY().getType() != to.getY().getType() ||
                        from.getZ().getType() != to.getZ().getType()
        ) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "'from' and 'to' coordinate sets must have matching coordinate types", pattern.find("TO")));
            file.getCompiler().getSymbolStack().pop();
            throw new EntryParsingException();
        }

        double fromX = Math.min(from.getX().getCoord(), to.getX().getCoord());
        double fromY = Math.min(from.getY().getCoord(), to.getY().getCoord());
        double fromZ = Math.min(from.getZ().getCoord(), to.getZ().getCoord());
        double toX = Math.max(from.getX().getCoord(), to.getX().getCoord());
        double toY = Math.max(from.getY().getCoord(), to.getY().getCoord());
        double toZ = Math.max(from.getZ().getCoord(), to.getZ().getCoord());

        for(double x = fromX; x <= toX; x += step) {
            for(double y = fromY; y <= toY; y += step) {
                for(double z = fromZ; z <= toZ; z += step) {
                    symbol.setValue(new CoordinateSet(new Coordinate(from.getX().getType(), x), new Coordinate(from.getY().getType(), y), new Coordinate(from.getZ().getType(), z)));
                    try {
                        TridentFile.resolveInnerFileIntoSection(pattern.find("ANONYMOUS_INNER_FUNCTION"), file, file.getFunction());
                    } finally {
                        file.getCompiler().getSymbolStack().pop();
                    }
                }
            }
        }

        file.getCompiler().getSymbolStack().pop();
    }
}

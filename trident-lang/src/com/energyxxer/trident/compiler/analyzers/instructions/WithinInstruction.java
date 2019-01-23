package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "within")
public class WithinInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {

        SymbolTable table = new SymbolTable(file);
        file.getCompiler().getSymbolStack().push(table);

        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false));
        table.put(symbol);

        try {
            CoordinateSet from = CoordinateParser.parse(pattern.find("FROM.COORDINATE_SET"), file);
            CoordinateSet to = CoordinateParser.parse(pattern.find("TO.COORDINATE_SET"), file);

            double step = 1;
            if (pattern.find("STEP") != null) {
                step = CommonParsers.parseDouble(pattern.find("STEP.REAL"), file);
                if (step <= 0) {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "Within step must be positive", pattern.find("STEP"), file);
                }
            }

            if (
                    from.getX().getType() != to.getX().getType() ||
                            from.getY().getType() != to.getY().getType() ||
                            from.getZ().getType() != to.getZ().getType()
            ) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "'from' and 'to' coordinate sets must have matching coordinate types", pattern.find("TO"), file);
            }

            double fromX = Math.min(from.getX().getCoord(), to.getX().getCoord());
            double fromY = Math.min(from.getY().getCoord(), to.getY().getCoord());
            double fromZ = Math.min(from.getZ().getCoord(), to.getZ().getCoord());
            double toX = Math.max(from.getX().getCoord(), to.getX().getCoord());
            double toY = Math.max(from.getY().getCoord(), to.getY().getCoord());
            double toZ = Math.max(from.getZ().getCoord(), to.getZ().getCoord());

            for (double x = fromX; x <= toX; x += step) {
                for (double y = fromY; y <= toY; y += step) {
                    for (double z = fromZ; z <= toZ; z += step) {
                        symbol.setValue(new CoordinateSet(new Coordinate(from.getX().getType(), x), new Coordinate(from.getY().getType(), y), new Coordinate(from.getZ().getType(), z)));
                        TridentFile.resolveInnerFileIntoSection(pattern.find("ANONYMOUS_INNER_FUNCTION"), file, file.getFunction());
                    }
                }
            }
        } finally {
            file.getCompiler().getSymbolStack().pop();
        }
    }
}

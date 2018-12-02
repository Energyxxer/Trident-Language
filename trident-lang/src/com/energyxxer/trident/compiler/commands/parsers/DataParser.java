package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.DataGetCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "data")
public class DataParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GET": return parseGet(inner, file.getCompiler());
            case "MERGE": return parseMerge(inner, file.getCompiler());
            case "MODIFY": return parseModify(inner, file.getCompiler());
            case "REMOVE": return parseRemove(inner, file.getCompiler());
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'"));
                return null;
            }
        }
    }

    private Command parseRemove(TokenPattern<?> inner, TridentCompiler compiler) {
        return null;
    }

    private Command parseModify(TokenPattern<?> inner, TridentCompiler compiler) {
        return null;
    }

    private Command parseMerge(TokenPattern<?> inner, TridentCompiler compiler) {
        return null;
    }

    private Command parseGet(TokenPattern<?> inner, TridentCompiler compiler) {
        Object target = parseTarget(inner.find("DATA_TARGET"), compiler);

        TokenPattern<?> pathClause = inner.find("PATH_CLAUSE");
        if(pathClause != null) {
            NBTPath path = NBTParser.parsePath(pathClause.find("NBT_PATH"));
            TokenPattern<?> scalePattern = pathClause.find("SCALE");
            double scale = 1;
            if(scalePattern != null) {
                scale = Double.parseDouble(scalePattern.flatten(false));
            }
            if(target instanceof CoordinateSet) {
                return new DataGetCommand((CoordinateSet) target, path, scale);
            } else {
                return new DataGetCommand((Entity) target, path, scale);
            }
        }

        if(target instanceof CoordinateSet) {
            return new DataGetCommand((CoordinateSet) target);
        } else {
            return new DataGetCommand((Entity) target);
        }
    }

    private Object parseTarget(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> rawCoords = pattern.find("COORDINATE_SET");
        if(rawCoords != null) return CoordinateParser.parse(rawCoords);
        else return EntityParser.parseEntity(pattern.find("ENTITY"), compiler);
    }
}

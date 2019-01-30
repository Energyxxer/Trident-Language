package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;

@AnalyzerMember(key = "data")
public class DataParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GET": return parseGet(inner, file);
            case "MERGE": return parseMerge(inner, file);
            case "MODIFY": return parseModify(inner, file);
            case "REMOVE": return parseRemove(inner, file);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }

    private Command parseRemove(TokenPattern<?> inner, TridentFile file) {
        Object target = parseTarget(inner.find("DATA_TARGET"), file);
        NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), file);

        try {
            if(target instanceof CoordinateSet) {
                return new DataRemoveCommand((CoordinateSet) target, path);
            } else {
                return new DataRemoveCommand((Entity) target, path);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, file);
        }
    }

    private Command parseModify(TokenPattern<?> pattern, TridentFile file) {
        Object target = parseTarget(pattern.find("DATA_TARGET"), file);
        NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), file);
        DataModifyCommand.ModifyOperation operation;


        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "MODIFY_APPEND": {
                operation = DataModifyCommand.APPEND();
                break;
            }
            case "MODIFY_INSERT": {
                operation = DataModifyCommand.INSERT(
                        inner.find("CHOICE").flatten(false).equals("AFTER") ?
                                DataModifyCommand.InsertOrder.AFTER :
                                DataModifyCommand.InsertOrder.BEFORE,
                        CommonParsers.parseInt(inner.find("INTEGER"), file));
                break;
            }
            case "MODIFY_MERGE":
                operation = DataModifyCommand.MERGE();
                break;
            case "MODIFY_PREPEND":
                operation = DataModifyCommand.PREPEND();
                break;
            case "MODIFY_SET":
                operation = DataModifyCommand.SET();
                break;
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }

        DataModifyCommand.ModifySource source = parseSource(inner.find("DATA_SOURCE"), file);

        try {
            if(target instanceof CoordinateSet) {
                return new DataModifyCommand((CoordinateSet) target, path, operation, source);
            } else {
                return new DataModifyCommand((Entity) target, path, operation, source);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, file);
        }
    }

    private Command parseMerge(TokenPattern<?> inner, TridentFile file) {
        Object target = parseTarget(inner.find("DATA_TARGET"), file);
        TagCompound nbt = NBTParser.parseCompound(inner.find("NBT_COMPOUND"), file);

        try {
            if(target instanceof CoordinateSet) {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
                NBTParser.analyzeTag(nbt, context, inner.find("NBT_COMPOUND"), file);
                return new DataMergeCommand((CoordinateSet) target, nbt);
            } else {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
                NBTParser.analyzeTag(nbt, context, inner.find("NBT_COMPOUND"), file);
                return new DataMergeCommand((Entity) target, nbt);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, file);
        }
    }

    private Command parseGet(TokenPattern<?> inner, TridentFile file) {
        Object target = parseTarget(inner.find("DATA_TARGET"), file);

        try {
            TokenPattern<?> pathClause = inner.find("PATH_CLAUSE");
            if(pathClause != null) {
                NBTPath path = NBTParser.parsePath(pathClause.find("NBT_PATH"), file);
                TokenPattern<?> scalePattern = pathClause.find("SCALE");
                double scale = 1;
                if(scalePattern != null) {
                    scale = CommonParsers.parseDouble(scalePattern, file);
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
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, file)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, file);
        }
    }

    private Object parseTarget(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> rawCoords = pattern.find("COORDINATE_SET");
        if(rawCoords != null) return CoordinateParser.parse(rawCoords, file);
        else return EntityParser.parseEntity(pattern.find("ENTITY"), file);
    }

    private DataModifyCommand.ModifySource parseSource(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern).getContents();
        switch(inner.getName()) {
            case "LITERAL_SOURCE": {
                return new ModifySourceValue(NBTParser.parseValue(inner.find("NBT_VALUE"), file));
            }
            case "TARGET_SOURCE": {
                Object target = parseTarget(inner.find("DATA_TARGET"), file);
                NBTPath path = NBTParser.parsePath(inner.find("PATH_CLAUSE.NBT_PATH"), file);

                if(target instanceof CoordinateSet) {
                    return new ModifySourceFromBlock((CoordinateSet) target, path);
                } else {
                    return new ModifySourceFromEntity((Entity) target, path);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }
}

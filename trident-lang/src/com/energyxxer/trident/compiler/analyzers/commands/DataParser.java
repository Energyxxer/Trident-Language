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
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;

@AnalyzerMember(key = "data")
public class DataParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GET": return parseGet(inner, ctx);
            case "MERGE": return parseMerge(inner, ctx);
            case "MODIFY": return parseModify(inner, ctx);
            case "REMOVE": return parseRemove(inner, ctx);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private Command parseRemove(TokenPattern<?> inner, ISymbolContext ctx) {
        Object target = parseTarget(inner.find("DATA_TARGET"), ctx);
        NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), ctx);

        try {
            if(target instanceof CoordinateSet) {
                return new DataRemoveCommand((CoordinateSet) target, path);
            } else {
                return new DataRemoveCommand((Entity) target, path);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private Command parseModify(TokenPattern<?> pattern, ISymbolContext ctx) {
        Object target = parseTarget(pattern.find("DATA_TARGET"), ctx);
        NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), ctx);
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
                        CommonParsers.parseInt(inner.find("INTEGER"), ctx));
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
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }

        DataModifyCommand.ModifySource source = parseSource(inner.find("DATA_SOURCE"), ctx);

        try {
            if(target instanceof CoordinateSet) {
                return new DataModifyCommand((CoordinateSet) target, path, operation, source);
            } else {
                return new DataModifyCommand((Entity) target, path, operation, source);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    private Command parseMerge(TokenPattern<?> inner, ISymbolContext ctx) {
        Object target = parseTarget(inner.find("DATA_TARGET"), ctx);
        TagCompound nbt = NBTParser.parseCompound(inner.find("NBT_COMPOUND"), ctx);

        try {
            if(target instanceof CoordinateSet) {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
                NBTParser.analyzeTag(nbt, context, inner.find("NBT_COMPOUND"), ctx);
                return new DataMergeCommand((CoordinateSet) target, nbt);
            } else {
                PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
                NBTParser.analyzeTag(nbt, context, inner.find("NBT_COMPOUND"), ctx);
                return new DataMergeCommand((Entity) target, nbt);
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private Command parseGet(TokenPattern<?> inner, ISymbolContext ctx) {
        Object target = parseTarget(inner.find("DATA_TARGET"), ctx);

        try {
            TokenPattern<?> pathClause = inner.find("PATH_CLAUSE");
            if(pathClause != null) {
                NBTPath path = NBTParser.parsePath(pathClause.find("NBT_PATH"), ctx);
                TokenPattern<?> scalePattern = pathClause.find("SCALE");
                double scale = 1;
                if(scalePattern != null) {
                    scale = CommonParsers.parseDouble(scalePattern, ctx);
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
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private Object parseTarget(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> rawCoords = pattern.find("COORDINATE_SET");
        if(rawCoords != null) return CoordinateParser.parse(rawCoords, ctx);
        else return EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
    }

    private DataModifyCommand.ModifySource parseSource(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern).getContents();
        switch(inner.getName()) {
            case "LITERAL_SOURCE": {
                return new ModifySourceValue(NBTParser.parseValue(inner.find("NBT_VALUE"), ctx));
            }
            case "TARGET_SOURCE": {
                Object target = parseTarget(inner.find("DATA_TARGET"), ctx);
                NBTPath path = NBTParser.parsePath(inner.find("PATH_CLAUSE.NBT_PATH"), ctx);

                if(target instanceof CoordinateSet) {
                    return new ModifySourceFromBlock((CoordinateSet) target, path);
                } else {
                    return new ModifySourceFromEntity((Entity) target, path);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}

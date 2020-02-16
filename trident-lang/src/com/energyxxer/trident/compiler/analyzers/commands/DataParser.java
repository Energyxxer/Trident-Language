package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.data.*;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTListMatch;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathCompoundRoot;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import com.energyxxer.commodore.types.defaults.StorageTarget;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;

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
        DataHolder target = parseTarget(inner.find("DATA_TARGET"), ctx);
        NBTPath path = NBTParser.parsePath(inner.find("NBT_PATH"), ctx);

        try {
            return new DataRemoveCommand(target, path);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private Command parseModify(TokenPattern<?> pattern, ISymbolContext ctx) {
        DataHolder target = parseTarget(pattern.find("DATA_TARGET"), ctx);
        NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), ctx);
        DataModifyCommand.ModifyOperation operation;

        boolean appendListNode = false;


        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "MODIFY_APPEND": {
                operation = DataModifyCommand.APPEND();
                appendListNode = true;
                break;
            }
            case "MODIFY_INSERT": {
                appendListNode = true;
                operation = DataModifyCommand.INSERT(
                        CommonParsers.parseInt(inner.find("INTEGER"), ctx));
                break;
            }
            case "MODIFY_MERGE":
                operation = DataModifyCommand.MERGE();
                break;
            case "MODIFY_PREPEND":
                appendListNode = true;
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

        NBTPath analysisPath = path;
        if(appendListNode) {
            ArrayList<NBTPathNode> newNodes = new ArrayList<>();
            for(NBTPath subPath : path) {
                newNodes.add(subPath.getNode());
            }
            newNodes.add(new NBTListMatch());
            analysisPath = new NBTPath(newNodes.toArray(new NBTPathNode[0]));
        }

        try {
            if(source instanceof ModifySourceValue) {
                NBTParser.analyzeTag(((ModifySourceValue) source).getValue(), NBTParser.createContextForDataHolder(target, ctx), analysisPath, pattern, ctx);
            } else {
                NBTPath sourcePath = ((ModifySourceFromHolder) source).getSourcePath();
                if(sourcePath == null) sourcePath = new NBTPath(new NBTPathCompoundRoot(new TagCompound()));
                NBTParser.comparePaths(analysisPath, NBTParser.createContextForDataHolder(target, ctx), sourcePath, NBTParser.createContextForDataHolder(((ModifySourceFromHolder) source).getHolder(), ctx), pattern, ctx);
            }
            return new DataModifyCommand(target, path, operation, source);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    private Command parseMerge(TokenPattern<?> inner, ISymbolContext ctx) {
        DataHolder target = parseTarget(inner.find("DATA_TARGET"), ctx);
        TagCompound nbt = NBTParser.parseCompound(inner.find("NBT_COMPOUND"), ctx);

        try {
            PathContext context = NBTParser.createContextForDataHolder(target, ctx);
            NBTParser.analyzeTag(nbt, context, inner.find("NBT_COMPOUND"), ctx);
            return new DataMergeCommand(target, nbt);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private Command parseGet(TokenPattern<?> inner, ISymbolContext ctx) {
        DataHolder target = parseTarget(inner.find("DATA_TARGET"), ctx);

        try {
            TokenPattern<?> pathClause = inner.find("PATH_CLAUSE");
            if(pathClause != null) {
                NBTPath path = NBTParser.parsePath(pathClause.find("NBT_PATH"), ctx);
                TokenPattern<?> scalePattern = pathClause.find("SCALE");
                double scale = 1;
                if(scalePattern != null) {
                    scale = CommonParsers.parseDouble(scalePattern, ctx);
                }
                return new DataGetCommand(target, path, scale);
            }

            return new DataGetCommand(target);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, inner, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, inner.find("DATA_TARGET.ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
        }
    }

    private DataHolder parseTarget(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(((TokenPattern<?>)pattern.getContents()).getName()) {
            case "BLOCK_TARGET": {
                return new DataHolderBlock(CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx));
            }
            case "ENTITY_TARGET": {
                return new DataHolderEntity(EntityParser.parseEntity(pattern.find("ENTITY"), ctx));
            }
            case "STORAGE_TARGET": {
                TridentUtil.ResourceLocation loc = CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION"), ctx);
                return new DataHolderStorage(new StorageTarget(ctx.getCompiler().getModule().getNamespace(loc.namespace), loc.body));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + ((TokenPattern<?>)pattern.getContents()).getName() + "'", pattern, ctx);
            }
        }
    }

    private DataModifyCommand.ModifySource parseSource(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern).getContents();
        switch(inner.getName()) {
            case "LITERAL_SOURCE": {
                return new ModifySourceValue(NBTParser.parseValue(inner.find("NBT_VALUE"), ctx));
            }
            case "TARGET_SOURCE": {
                DataHolder target = parseTarget(inner.find("DATA_TARGET"), ctx);
                NBTPath path = NBTParser.parsePath(inner.find("PATH_CLAUSE.NBT_PATH"), ctx);

                return new ModifySourceFromHolder(target, path);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}

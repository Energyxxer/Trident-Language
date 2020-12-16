package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupTypeMapTask;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataType;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.energyxxer.nbtmapper.tags.PathProtocol.STORAGE;

public class CommonParsers {

    @Contract("null, _ -> null")
    public static String parseFunctionPath(TokenPattern<?> id, ISymbolContext ctx) {
        if(id == null) return null;
        String flat = id.find("RAW_RESOURCE_LOCATION").flatten(false);
        if(flat.startsWith("/")) {
            flat = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction().getFullName() + flat;
        }
        String prefix = flat.substring(0, flat.indexOf(":")+1);
        flat = flat.substring(prefix.length());
        ArrayList<String> splits = new ArrayList<>(Arrays.asList(flat.split("/",-1)));
        for(int i = 0; i < splits.size()-1; i++) {
            if("..".equals(splits.get(i+1))) {
                splits.remove(i);
                splits.remove(i);
                i--;
                if(i >= 0) i--;
            }
        }

        flat = prefix + String.join("/", splits);

        if("..".equals(splits.get(0))) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal resource location: " + flat + " - path cannot go backwards any further", id, ctx);
        }

        if(flat.isEmpty()) {
            return ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction().getPath();
        }
        return flat;
    }

    public static ResourceLocation parseResourceLocation(@NotNull String str, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(str.equals("/")) {
            return ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getResourceLocation();
        }
        ResourceLocation loc;

        if(str.startsWith("/")) {
            str = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getResourceLocation() + str;
        }

        loc = ResourceLocation.createStrict(str);

        if(loc == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal resource location: '" + str + "'", pattern, ctx);
        }

        ArrayList<String> splits = new ArrayList<>(Arrays.asList(loc.body.split("/",-1)));
        for(int i = 0; i < splits.size()-1; i++) {
            if("..".equals(splits.get(i+1))) {
                splits.remove(i);
                splits.remove(i);
                i--;
                if(i >= 0) i--;
            }
        }

        loc.body = String.join("/", splits);

        if(splits.isEmpty()) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal resource location: " + str + " - path cannot go backwards any further", pattern, ctx);
        }
        if("..".equals(splits.get(0))) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Illegal resource location: " + loc + " - path cannot go backwards any further", pattern, ctx);
        }

        return loc;
    }

    @Contract("null, _ -> null")
    public static Type parseFunctionTag(TokenStructure pattern, ISymbolContext ctx) {
        if(pattern == null) return null;

        TokenPattern<?> inner = pattern.getContents();

        ResourceLocation typeLoc = (ResourceLocation) inner.evaluate(ctx);
        Namespace ns = ctx.get(SetupModuleTask.INSTANCE).getNamespace(typeLoc.namespace);
        Type type = null;
        if(typeLoc.isTag) {
            type = ns.tags.functionTags.get(typeLoc.body);
        } else {
            Function subjectFunction = ns.functions.get(typeLoc.body);
            if(subjectFunction != null) type = new FunctionReference(subjectFunction);
        }

        if(typeLoc.equals(new ResourceLocation(ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getResourceLocation().toString() + "/")))
            return new FunctionReference(ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());

        if(type == null) {
            if(typeLoc.isTag) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "No such function tag exists: " + typeLoc, inner, ctx);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "No such function exists: " + typeLoc, inner, ctx);
            }
        }
        return type;
    }

    /**
     * CommonParsers should not be instantiated.
     * */
    @Contract(pure = true)
    private CommonParsers() {
    }

    @Nullable
    public static Type guessEntityType(Entity entity, @NotNull ISymbolContext ctx) {
        TypeDictionary dict = ctx.get(SetupModuleTask.INSTANCE).minecraft.types.entity;
        if(entity instanceof PlayerName) return dict.get("player");
        if(entity instanceof Selector) {
            Selector selector = ((Selector) entity);
            List<SelectorArgument> typeArg = new ArrayList<>(selector.getArgumentsByKey("type"));
            if(typeArg.isEmpty() || ((TypeArgument) typeArg.get(0)).isNegated()) return null;
            return ((TypeArgument) typeArg.get(0)).getType();
        } else throw new IllegalArgumentException("entity");
    }

    @Nullable
    public static NumericNBTType getNumericType(Object body, NBTPath path, @NotNull ISymbolContext ctx, TokenPattern<?> pattern, boolean strict) {

        PathContext context = new PathContext().setIsSetting(true).setProtocol(
                body instanceof Entity ? PathProtocol.ENTITY :
                        body instanceof CoordinateSet ? PathProtocol.BLOCK_ENTITY :
                                STORAGE,
                body instanceof Entity ?
                        guessEntityType((Entity) body, ctx) :
                        body instanceof ResourceLocation ?
                                body.toString() :
                                null
        );

        DataTypeQueryResponse response = ctx.get(SetupTypeMapTask.INSTANCE).collectTypeInformation(path, context);

        if(response.isEmpty()) {
            if(strict) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Cannot infer correct NBT data type for the path '" + path + "'", pattern, ctx);
            } else return null;
        } else {
            if(response.getPossibleTypes().size() > 1 && strict) {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Ambiguous NBT data type for the path '" + path + "': possible types include " + response.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")) + ". Assuming " + response.getPossibleTypes().stream().findFirst().get().getShortTypeName(), pattern));
            }
            DataType dataType = response.getPossibleTypes().toArray(new DataType[0])[0];
            if(NumericNBTTag.class.isAssignableFrom(dataType.getCorrespondingTagType())) {
                try {
                    NBTTag sample = dataType.getCorrespondingTagType().newInstance();
                    return ((NumericNBTTag) sample).getNumericType();
                } catch (InstantiationException | IllegalAccessException x) {
                    throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Exception while instantiating default " + dataType.getCorrespondingTagType().getSimpleName() + ": " + x, pattern, ctx);
                }
            } else if (strict) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Path '" + path + "' is inferred to be of type " + dataType.getShortTypeName() + ", and thus a numeric NBT data type cannot be inferred", pattern, ctx);
            }
            return null;
        }
    }

    @Contract("null, _, _ -> param3")
    public static SymbolVisibility parseVisibility(TokenPattern<?> pattern, ISymbolContext ctx, SymbolVisibility defaultValue) {
        if(pattern == null) return defaultValue;
        switch(pattern.flatten(false)) {
            case "global": return SymbolVisibility.GLOBAL;
            case "public": return SymbolVisibility.PUBLIC;
            case "local": return TridentSymbolVisibility.LOCAL;
            case "private": return TridentSymbolVisibility.PRIVATE;
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.flatten(false) + "'", pattern, ctx);
            }
        }
    }
}
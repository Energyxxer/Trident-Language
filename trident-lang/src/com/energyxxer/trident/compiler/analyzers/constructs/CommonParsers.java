package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.BlockTag;
import com.energyxxer.commodore.tags.ItemTag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.tags.TagManager;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.commodore.types.defaults.TypeManager;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.commodore.util.TimeSpan;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataType;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.modifiers.ModifierParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CommonParsers {
    public static Object parseEntityReference(TokenPattern<?> id, ISymbolContext ctx) {
        if(id.getName().equals("ENTITY_ID_TAGGED")) return parseEntityReference(((TokenStructure)id).getContents(), ctx);
        if(id.getName().equals("ENTITY_ID_WRAPPER")) return parseEntityReference(id.find("ENTITY_ID"), ctx);
        if(id.getName().equals("ABSTRACT_RESOURCE")) return parseTag(id, ctx, EntityType.CATEGORY, g -> g.entity, g -> g.entityTypeTags);
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("INTERPOLATION_BLOCK")) {
            return InterpolationManager.parse(((TokenStructure) id).getContents(), ctx, Type.class, CustomEntity.class);
        } else return parseType(id, ctx, m -> m.entity);
    }
    public static Type parseItemType(TokenPattern<?> id, ISymbolContext ctx) {
        return parseType(id, ctx, m -> m.item);
    }
    public static Type parseBlockType(TokenPattern<?> id, ISymbolContext ctx) {
        return parseType(id, ctx, m -> m.block);
    }
    public static Type parseType(TokenPattern<?> id, ISymbolContext ctx, TypeGroupPicker picker) {
        if(id == null) return null;
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("INTERPOLATION_BLOCK")) {
            Type result = InterpolationManager.parse(((TokenStructure) id).getContents(), ctx, Type.class);
            EObject.assertNotNull(result, id, ctx);
            return result;
        }
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id);
        return picker.pick(ctx.getCompiler().getModule().getNamespace(typeLoc.namespace).types).get(typeLoc.body);
    }

    public static Type parseTag(TokenPattern<?> id, ISymbolContext ctx, String category, TypeGroupPicker typePicker, TagGroupPicker tagPicker) {
        if(id == null) return null;
        boolean isTag = id.find("") != null;
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id.find("RESOURCE_LOCATION").flatten(false));
        Namespace ns = ctx.getCompiler().getModule().getNamespace(typeLoc.namespace);

        Type type = null;

        if(isTag) {
            type = tagPicker.pick(ns.tags).get(typeLoc.body);
        } else {
            type = typePicker.pick(ns.types).get(typeLoc.body);
        }

        if(type == null) {
            if(isTag) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such " + category + " tag exists: " + typeLoc, id, ctx);
            } else {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such " + category + " type exists: " + typeLoc, id, ctx);
            }
        }

        return type;
    }

    public static String parseFunctionPath(TokenPattern<?> id, ISymbolContext ctx) {
        if(id == null) return null;
        String flat = id.find("RAW_RESOURCE_LOCATION").flatten(false);
        if(flat.startsWith("/")) {
            flat = ctx.getWritingFile().getFunction().getFullName() + flat;
        }
        if(flat.isEmpty()) {
            return ctx.getWritingFile().getFunction().getPath();
        }
        return flat;
    }

    public static TridentUtil.ResourceLocation parseResourceLocation(String str, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(str.equals("/")) {
            return ctx.getWritingFile().getResourceLocation();
        }
        TridentUtil.ResourceLocation loc;

        if(str.startsWith("/")) {
            str = ctx.getWritingFile().getResourceLocation() + str;
        }

        loc = TridentUtil.ResourceLocation.createStrict(str);

        if(loc != null) {
            return loc;
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Illegal resource location: '" + str + "'", pattern, ctx);
        }
    }

    public static Type parseFunctionTag(TokenStructure pattern, ISymbolContext ctx) {
        if(pattern == null) return null;

        TokenPattern<?> inner = pattern.getContents();

        TridentUtil.ResourceLocation typeLoc;

        switch(inner.getName()) {
            case "RAW_RESOURCE_LOCATION_TAGGED": {
                String flat = parseFunctionPath(inner, ctx);
                typeLoc = new TridentUtil.ResourceLocation(flat);
                typeLoc.isTag = inner.find("TAG_HEADER_WRAPPER") != null;
                break;
            }
            case "INTERPOLATION_BLOCK": {
                typeLoc = InterpolationManager.parse(inner, ctx, TridentUtil.ResourceLocation.class);
                EObject.assertNotNull(typeLoc, inner, ctx);
                break;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
        Namespace ns = ctx.getCompiler().getModule().getNamespace(typeLoc.namespace);
        Type type = null;
        if(typeLoc.isTag) {
            type = ns.tags.functionTags.get(typeLoc.body);
        } else if(ns.functions.contains(typeLoc.body)) {
            type = new FunctionReference(ns.functions.get(typeLoc.body));
        }

        if(typeLoc.equals(new TridentUtil.ResourceLocation(ctx.getWritingFile().getResourceLocation().toString() + "/"))) return new FunctionReference(ctx.getWritingFile().getFunction());

        if(type == null) {
            if(typeLoc.isTag) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such function tag exists: " + typeLoc, inner, ctx);
            } else {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such function exists: " + typeLoc, inner, ctx);
            }
        }
        return type;
    }

    public static ItemTag parseItemTag(TokenPattern<?> id, ISymbolContext ctx) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        ItemTag returned = ctx.getCompiler().getModule().getNamespace(tagLoc.namespace).tags.itemTags.get(tagLoc.body);
        if(returned == null) {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such item tag exists: " + tagLoc, id, ctx);
        }
        return returned;
    }

    public static BlockTag parseBlockTag(TokenPattern<?> id, ISymbolContext ctx) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        BlockTag returned = ctx.getCompiler().getModule().getNamespace(tagLoc.namespace).tags.blockTags.get(tagLoc.body);
        if(returned == null) {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "No such block tag exists: " + tagLoc, id, ctx);
        }
        return returned;
    }

    public static Item parseItem(TokenPattern<?> pattern, ISymbolContext ctx, NBTMode mode) {
        if(pattern == null) return null;
        if(pattern.getName().equals("ITEM_TAGGED") || pattern.getName().equals("ITEM")) return parseItem(((TokenStructure) pattern).getContents(), ctx, mode);

        if(pattern.getName().equals("ITEM_VARIABLE")) {
            Object reference = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, Item.class, CustomItem.class);
            Item item;
            if(reference == null) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Unexpected null value for item", pattern.find("INTERPOLATION_BLOCK"), ctx);
            } else if(reference instanceof Item) {
                item = (Item) reference;
            } else if(reference instanceof CustomItem) {
                item = ((CustomItem) reference).constructItem(mode);
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Unknown item reference return type: " + reference.getClass().getSimpleName(), pattern, ctx);
            }

            TokenPattern<?> appendedNBT = pattern.find("APPENDED_NBT.NBT_COMPOUND");
            if(appendedNBT != null) {
                TagCompound nbt = item.getNBT();
                if(nbt == null) nbt = new TagCompound();

                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, ctx));

                PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
                NBTParser.analyzeTag(nbt, context, appendedNBT, ctx);

                item = new Item(item.getItemType(), mergedNBT);
            }
            return item;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseItemType(pattern.find("RESOURCE_NAME.ITEM_ID"), ctx);
        } else {
            type = parseItemTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), ctx);
        }

        TagCompound tag = NBTParser.parseCompound(pattern.find(".NBT_COMPOUND"), ctx);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
            NBTParser.analyzeTag(tag, context, pattern.find(".NBT_COMPOUND"), ctx);
        }
        return new Item(type, tag);
    }

    public static Block parseBlock(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        if(pattern.getName().equals("BLOCK_TAGGED") || pattern.getName().equals("BLOCK")) return parseBlock(((TokenStructure) pattern).getContents(), ctx);

        if(pattern.getName().equals("INTERPOLATION_BLOCK")) {
            Block result = InterpolationManager.parse(pattern, ctx, Block.class);
            EObject.assertNotNull(result, pattern, ctx);
            return result;
        }
        if(pattern.getName().equals("BLOCK_VARIABLE")) {
            Block block = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, Block.class);
            EObject.assertNotNull(block, pattern.find("INTERPOLATION_BLOCK"), ctx);
            TokenPattern<?> appendedState = pattern.find("APPENDED_BLOCKSTATE.BLOCKSTATE");
            if(appendedState != null) {
                Blockstate state = block.getBlockstate();
                if(state == null) state = new Blockstate();
                block = new Block(block.getBlockType(), state.merge(parseBlockstate(appendedState, ctx)), block.getNBT());
            }
            TokenPattern<?> appendedNBT = pattern.find("APPENDED_NBT.NBT_COMPOUND");
            if(appendedNBT != null) {
                TagCompound nbt = block.getNBT();
                if(nbt == null) nbt = new TagCompound();
                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, ctx));
                PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
                NBTParser.analyzeTag(mergedNBT, context, appendedNBT, ctx);
                block = new Block(block.getBlockType(), block.getBlockstate(), mergedNBT);
            }
            return block;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseBlockType(pattern.find("RESOURCE_NAME.BLOCK_ID"), ctx);
        } else {
            type = parseBlockTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), ctx);
        }


        Blockstate state = parseBlockstate(pattern.find("BLOCKSTATE_CLAUSE.BLOCKSTATE"), ctx);
        TagCompound tag = NBTParser.parseCompound(pattern.find("NBT_CLAUSE.NBT_COMPOUND"), ctx);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
            NBTParser.analyzeTag(tag, context, pattern.find("NBT_CLAUSE.NBT_COMPOUND"), ctx);
        }
        return new Block(type, state, tag);
    }

    public static Blockstate parseBlockstate(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        TokenPattern<?> rawList = pattern.find("BLOCKSTATE_LIST");

        Blockstate blockstate = null;
        if(rawList instanceof TokenList) {
            TokenList list = (TokenList) rawList;
            for(TokenPattern<?> inner : list.getContents()) {
                if(inner.getName().equals("BLOCKSTATE_PROPERTY")) {
                    String key = parseIdentifierA(inner.find("BLOCKSTATE_PROPERTY_KEY.IDENTIFIER_A"), ctx);
                    String value = parseIdentifierA(inner.find("BLOCKSTATE_PROPERTY_VALUE.IDENTIFIER_A"), ctx);

                    if(blockstate == null) blockstate = new Blockstate();
                    blockstate.put(key, value);
                }
            }
        }
        return blockstate;
    }

    @SuppressWarnings("unchecked")
    public static IntegerRange parseIntRange(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            TokenPattern<?> variable = pattern.find("INTERPOLATION_BLOCK");
            if(variable != null) {
                Object value = InterpolationManager.parse(variable, ctx, Integer.class, IntegerRange.class);
                if(value instanceof Integer) value = new IntegerRange((int) value);
                return ((IntegerRange) value);
            }
            TokenPattern<?> exact = pattern.find("EXACT");
            if(exact != null) return new IntegerRange(parseInt(exact, ctx));
            List<TokenPattern<?>> minRaw = pattern.searchByName("MIN");
            List<TokenPattern<?>> maxRaw = pattern.deepSearchByName("MAX");
            Integer min = null;
            Integer max = null;
            if(!minRaw.isEmpty()) {
                min = Integer.parseInt(minRaw.get(0).flatten(false));
            }
            if(!maxRaw.isEmpty()) {
                max = Integer.parseInt(maxRaw.get(0).flatten(false));
            }
            return new IntegerRange(min, max);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    public static DoubleRange parseRealRange(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            TokenPattern<?> variable = pattern.find("INTERPOLATION_BLOCK");
            if(variable != null) {
                Object value = InterpolationManager.parse(variable, ctx, Double.class, DoubleRange.class);
                if(value instanceof Double) value = new DoubleRange((double) value);
                return (DoubleRange) value;
            }
            TokenPattern<?> exact = pattern.find("EXACT");
            if(exact != null) return new DoubleRange(parseDouble(exact, ctx));
            List<TokenPattern<?>> minRaw = pattern.searchByName("MIN");
            List<TokenPattern<?>> maxRaw = pattern.deepSearchByName("MAX");
            Double min = null;
            Double max = null;
            if(!minRaw.isEmpty()) {
                min = CommonParsers.parseDouble(minRaw.get(0), ctx);
            }
            if(!maxRaw.isEmpty()) {
                max = CommonParsers.parseDouble(maxRaw.get(0), ctx);
            }
            return new DoubleRange(min, max);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    public static Objective parseObjective(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        TokenPattern<?> inner = pattern.find("IDENTIFIER_A");
        if(inner == null) inner = pattern;
        String name = parseIdentifierA(inner, ctx);
        if(!ctx.getCompiler().getModule().getObjectiveManager().contains(name)) {
            ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Undefined objective name '" + name + "'", pattern));
            return ctx.getCompiler().getModule().getObjectiveManager().create(name, true);
        } else {
            return ctx.getCompiler().getModule().getObjectiveManager().get(name);
        }
    }

    public static String parseStringLiteralOrIdentifierA(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "STRING_LITERAL_OR_IDENTIFIER_A":
            case "STRING_LITERAL_OR_IDENTIFIER_D":
                return parseStringLiteralOrIdentifierA(((TokenStructure) pattern).getContents(), ctx);
            case "STRING":
            case "STRING_LITERAL":
            case "INTERPOLATION_BLOCK":
                return parseStringLiteral(pattern, ctx);
            case "IDENTIFIER_A":
                return parseIdentifierA(pattern, ctx); //should never have a string literal inside because of
                                                    // the order of the entries in the grammar but just to be safe...
            case "IDENTIFIER_D":
                return pattern.flatten(false);
            default:
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
        }
    }

    /**
     * CommonParsers should not be instantiated.
     * */
    private CommonParsers() {
    }

    public static TimeSpan parseTime(TokenPattern<?> time, ISymbolContext ctx) {
        try {
            String raw = time.flatten(false);
            TimeSpan.Units units = TimeSpan.Units.TICKS;

            for(TimeSpan.Units unitValue : TimeSpan.Units.values()) {
                if(raw.endsWith(unitValue.suffix)) {
                    units = unitValue;
                    raw = raw.substring(0, raw.length() - unitValue.suffix.length());
                    break;
                }
            }

            return new TimeSpan(Double.parseDouble(raw), units);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, time, ctx)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", time, ctx);
        }
    }

    public static Object parseAnything(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "INTERPOLATION_BLOCK":
            case "LINE_SAFE_INTERPOLATION_VALUE":
            case "INTERPOLATION_VALUE": return InterpolationManager.parse(pattern, ctx);
            case "INTEGER": return parseInt(pattern, ctx);
            case "REAL": return parseDouble(pattern, ctx);
            case "STRING_LITERAL": return CommandUtils.parseQuotedString(pattern.flatten(false));
            case "BOOLEAN": return pattern.flatten(false).equals("true");
            case "ENTITY": return EntityParser.parseEntity(pattern, ctx);
            case "BLOCK_TAGGED":
            case "BLOCK":
                return parseBlock(pattern, ctx);
            case "ITEM_TAGGED":
            case "ITEM":
                return parseItem(pattern, ctx, SETTING);
            case "COORDINATE_SET":
                return CoordinateParser.parse(pattern, ctx);
            case "NBT_COMPOUND":
                return NBTParser.parseCompound(pattern, ctx);
            case "NBT_PATH":
                return NBTParser.parsePath(pattern, ctx);
            case "TEXT_COMPONENT":
                return TextParser.parseTextComponent(pattern, ctx);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown value grammar name: '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static int parseInt(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_INTEGER": {
                try {
                    return Integer.parseInt(inner.flatten(false));
                } catch(NumberFormatException x) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Integer out of range", pattern, ctx);
                }
            }
            case "INTERPOLATION_BLOCK": {
                Integer result = InterpolationManager.parse(inner, ctx, Integer.class);
                EObject.assertNotNull(result, inner, ctx);
                return result;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    public static double parseDouble(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_REAL": return Double.parseDouble(inner.flatten(false));
            case "INTERPOLATION_BLOCK": {
                Double result = InterpolationManager.parse(inner, ctx, Double.class);
                EObject.assertNotNull(result, inner, ctx);
                return result;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    public static Type guessEntityType(Entity entity, ISymbolContext ctx) {
        TypeDictionary dict = ctx.getCompiler().getModule().minecraft.types.entity;
        if(entity instanceof PlayerName) return dict.get("player");
        if(entity instanceof Selector) {
            Selector selector = ((Selector) entity);
            List<SelectorArgument> typeArg = new ArrayList<>(selector.getArgumentsByKey("type"));
            if(typeArg.isEmpty() || ((TypeArgument) typeArg.get(0)).isNegated()) return null;
            return ((TypeArgument) typeArg.get(0)).getType();
        } else throw new IllegalArgumentException("entity");
    }

    public static NumericNBTType getNumericType(Object body, NBTPath path, ISymbolContext ctx, TokenPattern<?> pattern, boolean strict) {

        PathContext context = new PathContext().setIsSetting(true).setProtocol(body instanceof Entity ? PathProtocol.ENTITY : PathProtocol.BLOCK_ENTITY, body instanceof Entity ? guessEntityType((Entity) body, ctx) : null);

        DataTypeQueryResponse response = ctx.getCompiler().getTypeMap().collectTypeInformation(path, context);
        //Debug.log(response.getPossibleTypes());

        if(response.isEmpty()) {
            if(strict) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know the correct NBT data type for the path '" + path + "'", pattern, ctx);
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
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Exception while instantiating default " + dataType.getCorrespondingTagType().getSimpleName() + ": " + x, pattern, ctx);
                }
            } else if (strict) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected numeric NBT data type, instead got " + dataType.getShortTypeName(), pattern, ctx);
            }
            return null;
        }
    }

    public static TridentUtil.ResourceLocation parseResourceLocation(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;

        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

        switch(inner.getName()) {
            case "RAW_RESOURCE_LOCATION":
            case "RAW_RESOURCE_LOCATION_TAGGED": {
                return parseResourceLocation(inner.flatten(false), pattern, ctx);
            }
            case "INTERPOLATION_BLOCK": {
                return InterpolationManager.parse(inner, ctx, TridentUtil.ResourceLocation.class);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    public static String parseStringLiteral(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "STRING": return parseStringLiteral(((TokenStructure) pattern).getContents(), ctx);
            case "STRING_LITERAL": return CommandUtils.parseQuotedString(pattern.flatten(false));
            case "INTERPOLATION_BLOCK": {
                String result = InterpolationManager.parse(pattern, ctx, String.class);
                EObject.assertNotNull(result, pattern, ctx);
                return result;
            }
            case "RAW_STRING": return pattern.flatten(false);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static String parseIdentifierA(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "IDENTIFIER_A": return parseIdentifierA(((TokenStructure)pattern).getContents(), ctx);
            case "RAW_IDENTIFIER_A": return pattern.flatten(false);
            case "STRING": {
                String result = parseStringLiteral(pattern, ctx);
                if(TridentLexerProfile.IDENTIFIER_A_REGEX.matcher(result).matches()) {
                    return result;
                } else {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "The string '" + result + "' is not a valid argument here", pattern, ctx);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static String parseIdentifierB(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "IDENTIFIER_B": return parseIdentifierB(((TokenStructure)pattern).getContents(), ctx);
            case "RAW_IDENTIFIER_B": return pattern.flatten(false);
            case "STRING": {
                String result = parseStringLiteral(pattern, ctx);
                if(TridentLexerProfile.IDENTIFIER_B_REGEX.matcher(result).matches()) {
                    return result;
                } else {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "The string '" + result + "' is not a valid argument here", pattern, ctx);
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static Symbol.SymbolVisibility parseVisibility(TokenPattern<?> pattern, ISymbolContext ctx, Symbol.SymbolVisibility defaultValue) {
        if(pattern == null) return defaultValue;
        switch(pattern.flatten(false)) {
            case "global": return Symbol.SymbolVisibility.GLOBAL;
            case "local": return Symbol.SymbolVisibility.LOCAL;
            case "private": return Symbol.SymbolVisibility.PRIVATE;
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.flatten(false) + "'", pattern, ctx);
            }
        }
    }

    public static ArrayList<ExecuteModifier> parseModifierList(TokenList rawModifierList, ISymbolContext ctx) {
        return parseModifierList(rawModifierList, ctx, null);
    }

    public static ArrayList<ExecuteModifier> parseModifierList(TokenList rawModifierList, ISymbolContext ctx, ExceptionCollector collector) {
        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        if(rawModifierList != null) {
            for(TokenPattern<?> rawModifier : rawModifierList.getContents()) {
                ModifierParser parser = AnalyzerManager.getAnalyzer(ModifierParser.class, rawModifier.flattenTokens().get(0).value);
                if(parser != null) {
                    Collection<ExecuteModifier> modifier = parser.parse(rawModifier, ctx);
                    modifiers.addAll(modifier);
                } else {
                    TridentException x = new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown modifier analyzer for '" + rawModifier.flattenTokens().get(0).value + "'", rawModifier, ctx);
                    if(collector != null) collector.log(x);
                    else throw x;
                }
            }
        }
        return modifiers;
    }

    //Must always return a VALID pointer
    public static PointerObject parsePointer(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "POINTER": return parsePointer(((TokenStructure) pattern).getContents(), ctx);
            case "VARIABLE_POINTER": {
                if(pattern.find("POINTER_HEAD_WRAPPER") != null) {
                    Object target = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, Entity.class, CoordinateSet.class);
                    PointerObject pointer = new PointerObject(target, null);
                    parsePointerHead(pointer, pattern.find("POINTER_HEAD_WRAPPER.POINTER_HEAD"), ctx);

                    return pointer.validate(pattern, ctx);
                } else {
                    return InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, PointerObject.class).validate(pattern, ctx);
                }
            }
            case "ENTITY_POINTER": {
                Object target = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
                PointerObject pointer = new PointerObject(target, null);
                parsePointerHead(pointer, pattern.find("POINTER_HEAD"), ctx);

                return pointer.validate(pattern, ctx);
            }
            case "BLOCK_POINTER": {
                Object target = CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx);
                PointerObject pointer = new PointerObject(target, null);
                parsePointerHead(pointer, pattern.find("NBT_POINTER_HEAD"), ctx);

                return pointer.validate(pattern, ctx);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static LocalScore parseScore(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "SCORE": return parseScore(((TokenStructure) pattern).getContents(), ctx);
            case "POINTER_WRAPPER": {
                PointerObject pointer = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, PointerObject.class);
                if(!(pointer.getMember() instanceof String)) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected score pointer, instead got NBT pointer", pattern, ctx);
                }
                return new LocalScore((Entity) pointer.getTarget(), ctx.getCompiler().getModule().getObjectiveManager().get((String) pointer.getMember()));
            }
            case "POINTER": {
                PointerObject pointer = parsePointer(pattern, ctx);
                if(!(pointer.getMember() instanceof String)) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected score pointer, instead got NBT pointer", pattern, ctx);
                }
                return new LocalScore((Entity) pointer.getTarget(), ctx.getCompiler().getModule().getObjectiveManager().get((String) pointer.getMember()));
            }
            case "EXPLICIT_SCORE": {
                Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
                Objective objective = parseObjective(pattern.find("OBJECTIVE_NAME"), ctx);
                return new LocalScore(entity, objective);
            }
            case "VAR_SCORE": {
                Objective objective = parseObjective(pattern.find("OBJECTIVE_NAME"), ctx);
                Entity entity;
                if(objective != null) {
                    entity = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, Entity.class);
                } else {
                    PointerObject pointer = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx, PointerObject.class);
                    if(!(pointer.getMember() instanceof String)) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected score pointer, instead got NBT pointer", pattern, ctx);
                    }
                    objective = ctx.getCompiler().getModule().getObjectiveManager().get((String) pointer.getMember());
                    entity = (Entity) pointer.getTarget();
                }

                return new LocalScore(entity, objective);
            }
            case "SCORE_OPTIONAL_OBJECTIVE": {
                TokenPattern<?> entityClause = ((TokenStructure) pattern.find("TARGET_ENTITY")).getContents();

                Entity entity = entityClause.getName().equals("ENTITY") ? EntityParser.parseEntity(entityClause, ctx) : null;

                Objective objective = null;

                TokenPattern<?> objectiveClause = (pattern.find("OBJECTIVE_CLAUSE.OBJECTIVE_NAME_WRAPPER"));
                if(objectiveClause != null) {
                    objectiveClause = ((TokenStructure) objectiveClause).getContents();
                }
                if(objectiveClause != null && objectiveClause.getName().equals("OBJECTIVE_NAME")) {
                    objective = parseObjective(objectiveClause, ctx);
                }

                return new LocalScore(entity, objective);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    private static void parsePointerHead(PointerObject pointer, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (pattern.getName()) {
            case "POINTER_HEAD": {
                parsePointerHead(pointer, ((TokenStructure) pattern).getContents(), ctx);
                break;
            }
            case "SCORE_POINTER_HEAD": {
                String objectiveName = parseObjective(pattern.find("OBJECTIVE_NAME"), ctx).getName();
                pointer.setMember(objectiveName);
                TokenPattern<?> scalePattern = pattern.find("SCALE.REAL");
                if(scalePattern != null) {
                    pointer.setScale(parseDouble(scalePattern, ctx));
                }
                break;
            }
            case "NBT_POINTER_HEAD": {
                NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), ctx);
                pointer.setMember(path);
                TokenPattern<?> scalePattern = pattern.find("SCALE.REAL");
                if(scalePattern != null) {
                    pointer.setScale(parseDouble(scalePattern, ctx));
                }
                TokenPattern<?> numTypePattern = pattern.find("TYPE_CAST.NUMERIC_DATA_TYPE");
                if(numTypePattern != null) {
                    pointer.setNumericType(numTypePattern.flatten(false));
                }
                break;
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public interface TypeGroupPicker {
        TypeDictionary pick(TypeManager m);
    }

    public interface TagGroupPicker {
        TagGroup<?> pick(TagManager m);
    }
}
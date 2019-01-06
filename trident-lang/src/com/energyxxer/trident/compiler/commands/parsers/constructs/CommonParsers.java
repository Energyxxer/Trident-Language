package com.energyxxer.trident.compiler.commands.parsers.constructs;

import Trident.extensions.java.lang.Object.EObject;
import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTType;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
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
import com.energyxxer.commodore.util.NumberRange;
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
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import java.util.List;

import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CommonParsers {
    public static NumberRange<Integer> SAMPLE_INT_RANGE = new NumberRange<>(1, 2);
    public static NumberRange<Double> SAMPLE_REAL_RANGE = new NumberRange<>(1.0, 2.0);


    public static Type parseEntityType(TokenPattern<?> id, TridentFile file) {
        if(id.getName().equals("ENTITY_ID_TAGGED")) return parseEntityType((TokenPattern<?>) (id.getContents()), file);
        if(id.getName().equals("ABSTRACT_RESOURCE")) return parseTag(id, file, EntityType.CATEGORY, g -> g.entity, g -> g.entityTypeTags);
        return parseType(id, file, m -> m.entity);
    }
    public static Object parseEntityReference(TokenPattern<?> id, TridentFile file) {
        if(id.getName().equals("ENTITY_ID_TAGGED")) return parseEntityReference((TokenPattern<?>) (id.getContents()), file);
        if(id.getName().equals("ABSTRACT_RESOURCE")) return parseTag(id, file, EntityType.CATEGORY, g -> g.entity, g -> g.entityTypeTags);
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("INTERPOLATION_BLOCK")) {
            return InterpolationManager.parse(((TokenStructure) id).getContents(), file, Type.class, CustomEntity.class);
        } else return parseType(id, file, m -> m.entity);
    }
    public static Type parseItemType(TokenPattern<?> id,TridentFile file) {
        return parseType(id, file, m -> m.item);
    }
    public static Type parseBlockType(TokenPattern<?> id, TridentFile file) {
        return parseType(id, file, m -> m.block);
    }
    public static Type parseType(TokenPattern<?> id, TridentFile file, TypeGroupPicker picker) {
        if(id == null) return null;
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("INTERPOLATION_BLOCK")) {
            Type result = InterpolationManager.parse(((TokenStructure) id).getContents(), file, Type.class);
            EObject.assertNotNull(result, id, file);
            return result;
        }
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id);
        return picker.pick(file.getCompiler().getModule().getNamespace(typeLoc.namespace).types).get(typeLoc.body);
    }

    public static Type parseTag(TokenPattern<?> id, TridentFile file, String category, TypeGroupPicker typePicker, TagGroupPicker tagPicker) {
        if(id == null) return null;
        boolean isTag = id.find("") != null;
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id.find("RESOURCE_LOCATION").flatten(false));
        Namespace ns = file.getCompiler().getModule().getNamespace(typeLoc.namespace);

        Type type = null;

        if(isTag) {
            type = tagPicker.pick(ns.tags).get(typeLoc.body);
        } else {
            type = typePicker.pick(ns.types).get(typeLoc.body);
        }

        if(type == null) {
            if(isTag) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such " + category + " tag exists: #" + typeLoc, id));
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such " + category + " type exists: " + typeLoc, id));
            }
            throw new EntryParsingException();
        }

        return type;
    }

    public static String parseFunctionPath(TokenPattern<?> id, TridentFile file) {
        if(id == null) return null;
        String flat = id.find("RAW_RESOURCE_LOCATION").flatten(false);
        if(flat.startsWith('/')) {
            flat = file.getFunction().getFullName() + flat;
        }
        if(flat.isEmpty()) {
            return file.getFunction().getPath();
        }
        return flat;
    }

    public static Type parseFunctionTag(TokenStructure pattern, TridentFile file) {
        if(pattern == null) return null;

        TokenPattern<?> inner = pattern.getContents();

        TridentUtil.ResourceLocation typeLoc;

        switch(inner.getName()) {
            case "RAW_RESOURCE_LOCATION_TAGGED": {
                String flat = parseFunctionPath(inner, file);
                typeLoc = new TridentUtil.ResourceLocation(flat);
                typeLoc.isTag = inner.find("") != null;
                break;
            }
            case "INTERPOLATION_BLOCK": {
                typeLoc = InterpolationManager.parse(inner, file, TridentUtil.ResourceLocation.class);
                EObject.assertNotNull(typeLoc, inner, file);
                break;
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }
        Namespace ns = file.getCompiler().getModule().getNamespace(typeLoc.namespace);
        Type type = null;
        if(typeLoc.isTag) {
            type = ns.tags.functionTags.get(typeLoc.body);
        } else if(ns.functions.contains(typeLoc.body)) {
            type = new FunctionReference(ns.functions.get(typeLoc.body));
        }

        if(type == null) {
            if(typeLoc.isTag) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such function tag exists: #" + typeLoc, inner));
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such function exists: " + typeLoc, inner));
            }
            throw new EntryParsingException();
        }
        return type;
    }

    public static ItemTag parseItemTag(TokenPattern<?> id, TridentFile file) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        ItemTag returned = file.getCompiler().getModule().getNamespace(tagLoc.namespace).tags.itemTags.get(tagLoc.body);
        if(returned == null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such item tag exists: #" + tagLoc, id));
            throw new EntryParsingException();
        }
        return returned;
    }

    public static BlockTag parseBlockTag(TokenPattern<?> id, TridentFile file) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        BlockTag returned = file.getCompiler().getModule().getNamespace(tagLoc.namespace).tags.blockTags.get(tagLoc.body);
        if(returned == null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such block tag exists: #" + tagLoc, id));
            throw new EntryParsingException();
        }
        return returned;
    }

    public static Item parseItem(TokenPattern<?> pattern, TridentFile file, NBTMode mode) {
        if(pattern.getName().equals("ITEM_TAGGED") || pattern.getName().equals("ITEM")) return parseItem(((TokenStructure) pattern).getContents(), file, mode);

        if(pattern.getName().equals("ITEM_VARIABLE")) {
            Object reference = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), file, Item.class, CustomItem.class);
            Item item;
            if(reference == null) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unexpected null value for item", pattern.find("INTERPOLATION_BLOCK")));
                throw new EntryParsingException();
            } else if(reference instanceof Item) {
                item = (Item) reference;
            } else if(reference instanceof CustomItem) {
                item = ((CustomItem) reference).constructItem(mode);
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown item reference return type: " + reference.getClass().getSimpleName(), pattern));
                throw new EntryParsingException();
            }

            TokenPattern<?> appendedNBT = pattern.find("APPENDED_NBT.NBT_COMPOUND");
            if(appendedNBT != null) {
                TagCompound nbt = item.getNBT();
                if(nbt == null) nbt = new TagCompound();

                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, file));

                PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
                NBTParser.analyzeTag(nbt, context, appendedNBT, file);

                item = new Item(item.getItemType(), mergedNBT);
            }
            return item;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseItemType(pattern.find("RESOURCE_NAME.ITEM_ID"), file);
        } else {
            type = parseItemTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), file);
        }

        TagCompound tag = NBTParser.parseCompound(pattern.find(".NBT_COMPOUND"), file);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
            NBTParser.analyzeTag(tag, context, pattern.find(".NBT_COMPOUND"), file);
        }
        return new Item(type, tag);
    }

    public static Block parseBlock(TokenPattern<?> pattern, TridentFile file) {
        if(pattern.getName().equals("BLOCK_TAGGED") || pattern.getName().equals("BLOCK")) return parseBlock(((TokenStructure) pattern).getContents(), file);

        if(pattern.getName().equals("INTERPOLATION_BLOCK")) {
            Block result = InterpolationManager.parse(pattern, file, Block.class);
            EObject.assertNotNull(result, pattern, file);
            return result;
        }
        if(pattern.getName().equals("BLOCK_VARIABLE")) {
            Block block = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), file, Block.class);
            EObject.assertNotNull(block, pattern.find("INTERPOLATION_BLOCK"), file);
            TokenPattern<?> appendedState = pattern.find("APPENDED_BLOCKSTATE.BLOCKSTATE");
            if(appendedState != null) {
                Blockstate state = block.getBlockstate();
                if(state == null) state = new Blockstate();
                block = new Block(block.getBlockType(), state.merge(parseBlockstate(appendedState)), block.getNBT()); //TODO for Commodore: add Blockstate#merge
            }
            TokenPattern<?> appendedNBT = pattern.find("APPENDED_NBT.NBT_COMPOUND");
            if(appendedNBT != null) {
                TagCompound nbt = block.getNBT();
                if(nbt == null) nbt = new TagCompound();
                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, file));
                PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
                NBTParser.analyzeTag(mergedNBT, context, appendedNBT, file);
                block = new Block(block.getBlockType(), block.getBlockstate(), mergedNBT);
            }
            return block;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseBlockType(pattern.find("RESOURCE_NAME.BLOCK_ID"), file);
        } else {
            type = parseBlockTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), file);
        }


        Blockstate state = parseBlockstate(pattern.find("BLOCKSTATE_CLAUSE.BLOCKSTATE"));
        TagCompound tag = NBTParser.parseCompound(pattern.find("NBT_CLAUSE.NBT_COMPOUND"), file);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
            NBTParser.analyzeTag(tag, context, pattern.find("NBT_CLAUSE.NBT_COMPOUND"), file);
        }
        return new Block(type, state, tag);
    }

    public static Blockstate parseBlockstate(TokenPattern<?> pattern) {
        if(pattern == null) return null;
        TokenPattern<?> rawList = pattern.find("BLOCKSTATE_LIST");

        Blockstate blockstate = null;
        if(rawList instanceof TokenList) {
            TokenList list = (TokenList) rawList;
            for(TokenPattern<?> inner : list.getContents()) {
                if(inner.getName().equals("BLOCKSTATE_PROPERTY")) {
                    String key = inner.find("BLOCKSTATE_PROPERTY_KEY").flattenTokens().get(0).value;
                    String value = inner.find("BLOCKSTATE_PROPERTY_VALUE").flattenTokens().get(0).value;

                    if(blockstate == null) blockstate = new Blockstate();
                    blockstate.put(key, value);
                }
            }
        }
        return blockstate;
    }

    @SuppressWarnings("unchecked")
    public static NumberRange<Integer> parseIntRange(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> variable = pattern.find("INTERPOLATION_BLOCK");
        if(variable != null) {
            Object value = InterpolationManager.parse(variable, file, Integer.class, SAMPLE_INT_RANGE.getClass());
            if(value instanceof Integer) value = new NumberRange<>((int) value);
            return (NumberRange<Integer>) value;
        }
        TokenPattern<?> exact = pattern.find("EXACT");
        if(exact != null) return new NumberRange<>(parseInt(exact, file));
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
        return new NumberRange<>(min, max);
    }

    @SuppressWarnings("unchecked")
    public static NumberRange<Double> parseRealRange(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> variable = pattern.find("INTERPOLATION_BLOCK");
        if(variable != null) {
            Object value = InterpolationManager.parse(variable, file, Double.class, SAMPLE_REAL_RANGE.getClass());
            if(value instanceof Double) value = new NumberRange<>((double) value);
            return (NumberRange<Double>) value;
        }
        TokenPattern<?> exact = pattern.find("EXACT");
        if(exact != null) return new NumberRange<>(parseDouble(exact, file));
        List<TokenPattern<?>> minRaw = pattern.searchByName("MIN");
        List<TokenPattern<?>> maxRaw = pattern.deepSearchByName("MAX");
        Double min = null;
        Double max = null;
        if(!minRaw.isEmpty()) {
            min = CommonParsers.parseDouble(minRaw.get(0), file);
        }
        if(!maxRaw.isEmpty()) {
            max = CommonParsers.parseDouble(maxRaw.get(0), file);
        }
        return new NumberRange<>(min, max);
    }

    public static Objective parseObjective(TokenPattern<?> pattern, TridentFile file) {
        String name = pattern.flatten(true);
        if(!file.getCompiler().getModule().getObjectiveManager().contains(name)) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Undefined objective name '" + name + "'", pattern));
            return file.getCompiler().getModule().getObjectiveManager().create(name, true);
        } else {
            return file.getCompiler().getModule().getObjectiveManager().get(name);
        }
    }

    public static String parseStringLiteralOrIdentifierA(TokenPattern<?> pattern) {
        String str = "";
        if(pattern != null) {
            str = pattern.flatten(false);
            if(!pattern.deepSearchByName("STRING_LITERAL").isEmpty()) {
                str = CommandUtils.parseQuotedString(str);
            }
        }
        return str;
    }

    /**
     * CommonParsers should not be instantiated.
     * */
    private CommonParsers() {
    }

    public static TimeSpan parseTime(TokenPattern<?> time, TridentFile file) {
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
    }

    public static Object parseAnything(TokenPattern<?> pattern, TridentFile file) {
        switch(pattern.getName()) {
            case "INTERPOLATION_BLOCK": return InterpolationManager.parse(pattern, file);
            case "INTERPOLATION_VALUE": return InterpolationManager.parse(pattern, file);
            case "INTEGER": return parseInt(pattern, file);
            case "REAL": return parseDouble(pattern, file);
            case "STRING_LITERAL": return CommandUtils.parseQuotedString(pattern.flatten(false));
            case "BOOLEAN": return pattern.flatten(false).equals("true");
            case "ENTITY": return EntityParser.parseEntity(pattern, file);
            case "BLOCK_TAGGED":
            case "BLOCK":
                return parseBlock(pattern, file);
            case "ITEM_TAGGED":
            case "ITEM":
                return parseItem(pattern, file, SETTING);
            case "COORDINATE_SET":
                return CoordinateParser.parse(pattern, file);
            case "NBT_COMPOUND":
                return NBTParser.parseCompound(pattern, file);
            case "NBT_PATH":
                return NBTParser.parsePath(pattern, file);
            case "TEXT_COMPONENT":
                return TextParser.parseTextComponent(pattern, file);
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown value grammar name: '" + pattern.getName() + "'"));
                throw new EntryParsingException();
            }
        }
    }

    public static int parseInt(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_INTEGER": return Integer.parseInt(inner.flatten(false));
            case "INTERPOLATION_BLOCK": {
                Integer result = InterpolationManager.parse(inner, file, Integer.class);
                EObject.assertNotNull(result, inner, file);
                return result;
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }
    }

    public static double parseDouble(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_REAL": return Double.parseDouble(inner.flatten(false));
            case "INTERPOLATION_BLOCK": {
                Double result = InterpolationManager.parse(inner, file, Double.class);
                EObject.assertNotNull(result, inner, file);
                return result;
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }
    }

    public static Type guessEntityType(Entity entity, TridentFile file) {
        TypeDictionary dict = file.getCompiler().getModule().minecraft.types.entity;
        if(entity instanceof PlayerName) return dict.get("player");
        if(entity instanceof GenericEntity) {
            Selector selector = ((GenericEntity) entity).getSelector();
            List<SelectorArgument> typeArg = selector.getArgumentsByKey("type").toList();
            if(typeArg.isEmpty() || ((TypeArgument) typeArg.get(0)).isNegated()) return null;
            return ((TypeArgument) typeArg.get(0)).getType();
        } else throw new IllegalArgumentException("entity");
    }

    public static NumericNBTType getNumericType(Object body, NBTPath path, TridentFile file, TokenPattern<?> pattern, boolean strict) {

        PathContext context = new PathContext().setIsSetting(true).setProtocol(body instanceof Entity ? PathProtocol.ENTITY : PathProtocol.BLOCK_ENTITY, body instanceof Entity ? guessEntityType((Entity) body, file) : null);

        DataTypeQueryResponse response = file.getCompiler().getTypeMap().collectTypeInformation(path, context);
        //Debug.log(response.getPossibleTypes());

        if(response.isEmpty()) {
            if(strict) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Don't know the correct NBT data type for the path '" + path + "'", pattern));
                throw new EntryParsingException();
            } else return null;
        } else {
            if(response.getPossibleTypes().size() > 1 && strict) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Ambiguous NBT data type for the path '" + path + "': possible types include " + response.getPossibleTypes().map(DataType::getShortTypeName).toSet().join(", ") + ". Assuming " + response.getPossibleTypes().toList().get(0).getShortTypeName(), pattern));
            }
            DataType dataType = response.getPossibleTypes().toList().get(0);
            if(NumericNBTTag.class.isAssignableFrom(dataType.getCorrespondingTagType())) {
                try {
                    NBTTag sample = dataType.getCorrespondingTagType().newInstance();
                    return ((NumericNBTTag) sample).getNumericType();
                } catch (InstantiationException | IllegalAccessException x) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Exception while instantiating default " + dataType.getCorrespondingTagType().getSimpleName() + ": " + x, pattern));
                    throw new EntryParsingException();
                }
            } else if (strict) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Expected numeric NBT data type, instead got " + dataType.getShortTypeName(), pattern));
                throw new EntryParsingException();
            }
            return null;
        }
    }

    public static TridentUtil.ResourceLocation parseResourceLocation(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;

        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

        TridentUtil.ResourceLocation typeLoc;

        switch(inner.getName()) {
            case "RAW_RESOURCE_LOCATION":
            case "RAW_RESOURCE_LOCATION_TAGGED": {
                typeLoc = new TridentUtil.ResourceLocation(inner.flatten(false));
                break;
            }
            case "INTERPOLATION_BLOCK": {
                typeLoc = InterpolationManager.parse(inner, file, TridentUtil.ResourceLocation.class);
                EObject.assertNotNull(typeLoc, inner, file);
                break;
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }

        return typeLoc;
    }

    public interface TypeGroupPicker {
        TypeDictionary pick(TypeManager m);
    }

    public interface TagGroupPicker {
        TagGroup<?> pick(TagManager m);
    }
}
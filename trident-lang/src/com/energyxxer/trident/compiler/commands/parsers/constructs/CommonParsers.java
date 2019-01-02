package com.energyxxer.trident.compiler.commands.parsers.constructs;

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
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.variable_functions.VariableFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import java.util.Arrays;
import java.util.List;

import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CommonParsers {
    public static Type parseEntityType(TokenPattern<?> id, TridentCompiler compiler) {
        if(id.getName().equals("ENTITY_ID_TAGGED")) return parseEntityType((TokenPattern<?>) (id.getContents()), compiler);
        if(id.getName().equals("ABSTRACT_RESOURCE")) return parseTag(id, compiler, EntityType.CATEGORY, g -> g.entity, g -> g.entityTypeTags);
        return parseType(id, compiler, m -> m.entity);
    }
    public static Object parseEntityReference(TokenPattern<?> id, TridentCompiler compiler) {
        if(id.getName().equals("ENTITY_ID_TAGGED")) return parseEntityReference((TokenPattern<?>) (id.getContents()), compiler);
        if(id.getName().equals("ABSTRACT_RESOURCE")) return parseTag(id, compiler, EntityType.CATEGORY, g -> g.entity, g -> g.entityTypeTags);
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("VARIABLE_MARKER")) {
            return retrieveSymbol(((TokenStructure) id).getContents(), compiler, Type.class, CustomEntity.class);
        } else return parseType(id, compiler, m -> m.entity);
    }
    public static Type parseItemType(TokenPattern<?> id, TridentCompiler compiler) {
        return parseType(id, compiler, m -> m.item);
    }
    public static Type parseBlockType(TokenPattern<?> id, TridentCompiler compiler) {
        return parseType(id, compiler, m -> m.block);
    }
    public static Type parseType(TokenPattern<?> id, TridentCompiler compiler, TypeGroupPicker picker) {
        if(id == null) return null;
        if(id instanceof TokenStructure && ((TokenStructure) id).getContents().getName().equals("VARIABLE_MARKER")) {
            return retrieveSymbol(((TokenStructure) id).getContents(), compiler, Type.class);
        }
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id);
        return picker.pick(compiler.getModule().getNamespace(typeLoc.namespace).types).get(typeLoc.body);
    }

    public static Type parseTag(TokenPattern<?> id, TridentCompiler compiler, String category, TypeGroupPicker typePicker, TagGroupPicker tagPicker) {
        if(id == null) return null;
        boolean isTag = id.find("") != null;
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(id.find("RESOURCE_LOCATION").flatten(false));
        Namespace ns = compiler.getModule().getNamespace(typeLoc.namespace);

        Type type = null;

        if(isTag) {
            type = tagPicker.pick(ns.tags).get(typeLoc.body);
        } else {
            type = typePicker.pick(ns.types).get(typeLoc.body);
        }

        if(type == null) {
            if(isTag) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "No such " + category + " tag exists: #" + typeLoc, id));
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "No such " + category + " type exists: " + typeLoc, id));
            }
            throw new EntryParsingException();
        }

        return type;
    }

    public static Type parseFunctionTag(TokenPattern<?> id, TridentFile file) {
        if(id == null) return null;
        boolean isTag = id.find("") != null;
        String flat = id.find("RESOURCE_LOCATION").flatten(false);
        if(flat.startsWith('/')) {
            flat = file.getFunction().getFullName() + flat;
        }
        TridentUtil.ResourceLocation typeLoc = new TridentUtil.ResourceLocation(flat);
        Namespace ns = file.getCompiler().getModule().getNamespace(typeLoc.namespace);

        Type type = null;

        if(isTag) {
            type = ns.tags.functionTags.get(typeLoc.body);
        } else if(ns.functions.contains(typeLoc.body)) {
            type = new FunctionReference(ns.functions.get(typeLoc.body));
        }

        if(type == null) {
            if(isTag) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such function tag exists: #" + typeLoc, id));
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "No such function exists: " + typeLoc, id));
            }
            throw new EntryParsingException();
        }

        return type;
    }

    public static ItemTag parseItemTag(TokenPattern<?> id, TridentCompiler compiler) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        ItemTag returned = compiler.getModule().getNamespace(tagLoc.namespace).tags.itemTags.get(tagLoc.body);
        if(returned == null) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "No such item tag exists: #" + tagLoc, id));
            throw new EntryParsingException();
        }
        return returned;
    }

    public static BlockTag parseBlockTag(TokenPattern<?> id, TridentCompiler compiler) {
        TridentUtil.ResourceLocation tagLoc = new TridentUtil.ResourceLocation(id.flattenTokens().get(0).value);
        BlockTag returned = compiler.getModule().getNamespace(tagLoc.namespace).tags.blockTags.get(tagLoc.body);
        if(returned == null) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "No such block tag exists: #" + tagLoc, id));
            throw new EntryParsingException();
        }
        return returned;
    }

    public static Item parseItem(TokenPattern<?> pattern, TridentCompiler compiler, NBTMode mode) {
        if(pattern.getName().equals("ITEM_TAGGED") || pattern.getName().equals("ITEM")) return parseItem(((TokenStructure) pattern).getContents(), compiler, mode);

        if(pattern.getName().equals("ITEM_VARIABLE")) {
            Object reference = retrieveSymbol(pattern.find("VARIABLE_MARKER"), compiler, Item.class, CustomItem.class);
            Item item;
            if(reference instanceof Item) {
                item = (Item) reference;
            } else if(reference instanceof CustomItem) {
                item = ((CustomItem) reference).constructItem(mode);
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown item reference return type: " + reference.getClass().getSimpleName(), pattern));
                throw new EntryParsingException();
            }

            TokenPattern<?> appendedNBT = pattern.find("APPENDED_NBT.NBT_COMPOUND");
            if(appendedNBT != null) {
                TagCompound nbt = item.getNBT();
                if(nbt == null) nbt = new TagCompound();

                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, compiler));

                PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
                NBTParser.analyzeTag(nbt, context, appendedNBT, compiler);

                item = new Item(item.getItemType(), mergedNBT);
            }
            return item;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseItemType(pattern.find("RESOURCE_NAME.ITEM_ID"), compiler);
        } else {
            type = parseItemTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), compiler);
        }

        TagCompound tag = NBTParser.parseCompound(pattern.find(".NBT_COMPOUND"), compiler);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
            NBTParser.analyzeTag(tag, context, pattern.find(".NBT_COMPOUND"), compiler);
        }
        return new Item(type, tag);
    }

    public static Block parseBlock(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern.getName().equals("BLOCK_TAGGED") || pattern.getName().equals("BLOCK")) return parseBlock(((TokenStructure) pattern).getContents(), compiler);

        if(pattern.getName().equals("VARIABLE_MARKER")) {
            return retrieveSymbol(pattern, compiler, Block.class);
        }
        if(pattern.getName().equals("BLOCK_VARIABLE")) {
            Block block = retrieveSymbol(pattern.find("VARIABLE_MARKER"), compiler, Block.class);
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
                TagCompound mergedNBT = nbt.merge(NBTParser.parseCompound(appendedNBT, compiler));
                PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
                NBTParser.analyzeTag(mergedNBT, context, appendedNBT, compiler);
                block = new Block(block.getBlockType(), block.getBlockstate(), mergedNBT);
            }
            return block;
        }

        boolean isStandalone = pattern.getName().equals("CONCRETE_RESOURCE");

        Type type;

        if(isStandalone) {
            type = parseBlockType(pattern.find("RESOURCE_NAME.BLOCK_ID"), compiler);
        } else {
            type = parseBlockTag(pattern.find("RESOURCE_NAME.RESOURCE_LOCATION"), compiler);
        }


        Blockstate state = parseBlockstate(pattern.find("BLOCKSTATE_CLAUSE.BLOCKSTATE"));
        TagCompound tag = NBTParser.parseCompound(pattern.find("NBT_CLAUSE.NBT_COMPOUND"), compiler);
        if(tag != null) {
            PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
            NBTParser.analyzeTag(tag, context, pattern.find("NBT_CLAUSE.NBT_COMPOUND"), compiler);
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

    public static NumberRange<Integer> parseIntRange(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> exact = pattern.find("EXACT");
        if(exact != null) return new NumberRange<>(parseInt(exact, compiler));
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

    public static NumberRange<Double> parseRealRange(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> exact = pattern.find("EXACT");
        if(exact != null) return new NumberRange<>(parseDouble(exact, compiler));
        List<TokenPattern<?>> minRaw = pattern.searchByName("MIN");
        List<TokenPattern<?>> maxRaw = pattern.deepSearchByName("MAX");
        Double min = null;
        Double max = null;
        if(!minRaw.isEmpty()) {
            min = CommonParsers.parseDouble(minRaw.get(0), compiler);
        }
        if(!maxRaw.isEmpty()) {
            max = CommonParsers.parseDouble(maxRaw.get(0), compiler);
        }
        return new NumberRange<>(min, max);
    }

    public static Objective parseObjective(TokenPattern<?> pattern, TridentCompiler compiler) {
        String name = pattern.flatten(true);
        if(!compiler.getModule().getObjectiveManager().contains(name)) {
            compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Undefined objective name '" + name + "'", pattern));
            return compiler.getModule().getObjectiveManager().create(name, true);
        } else {
            return compiler.getModule().getObjectiveManager().get(name);
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

    public static TimeSpan parseTime(TokenPattern<?> time, TridentCompiler compiler) {
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

    public static Object parseAnything(TokenPattern<?> pattern, TridentCompiler compiler) {
        switch(pattern.getName()) {
            case "INTEGER": return parseInt(pattern, compiler);
            case "REAL": return parseDouble(pattern, compiler);
            case "STRING_LITERAL": return CommandUtils.parseQuotedString(pattern.flatten(false));
            case "BOOLEAN": return pattern.flatten(false).equals("true");
            case "ENTITY": return EntityParser.parseEntity(pattern, compiler);
            case "BLOCK_TAGGED":
            case "BLOCK":
                return parseBlock(pattern, compiler);
            case "ITEM_TAGGED":
            case "ITEM":
                return parseItem(pattern, compiler, SETTING);
            case "COORDINATE_SET":
                return CoordinateParser.parse(pattern, compiler);
            case "NBT_COMPOUND":
                return NBTParser.parseCompound(pattern, compiler);
            case "NBT_PATH":
                return NBTParser.parsePath(pattern, compiler);
            case "TEXT_COMPONENT":
                return TextParser.parseTextComponent(pattern, compiler);
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown value grammar name: '" + pattern.getName() + "'"));
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T retrieveSymbol(TokenPattern<?> pattern, TridentCompiler compiler, Class<T> expected) {
        Object obj = retrieveSymbol(pattern, compiler);
        if(expected.isInstance(obj)) {
            return (T) obj;
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.find("VARIABLE_NAME").flatten(false) + "' does not contain a value of type " + expected.getSimpleName(), pattern));
            throw new EntryParsingException();
        }
    }

    @SuppressWarnings("unchecked")
    public static Object retrieveSymbol(TokenPattern<?> pattern, TridentCompiler compiler, Class... expected) {
        Object obj = retrieveSymbol(pattern, compiler);
        for(Class cls : expected) {
            if(cls.isInstance(obj)) return obj;
        }
        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + pattern.find("VARIABLE_NAME").flatten(false) + "' does not contain a value of type " + Arrays.asList(expected).map((Class c) -> c.getSimpleName()).toSet().join(", "), pattern));
        throw new EntryParsingException();
    }

    public static Object retrieveSymbol(TokenPattern<?> pattern, TridentCompiler compiler) {
        String name = pattern.find("VARIABLE_NAME").flatten(false);
        TokenPattern<?> modPattern = pattern.find("VARIABLE_MODIFIER.VARIABLE_MODIFIER_FUNCTION");
        String mod = modPattern != null ? modPattern.flatten(false) : null;

        Symbol symbol = compiler.getStack().search(name);

        if(symbol != null) {

            if(mod != null) {
                VariableFunction varFunction = ParserManager.getParser(VariableFunction.class, mod);
                if (varFunction != null) return varFunction.process(symbol.getValue(), pattern, compiler);
            }

            return symbol.getValue();
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Symbol '" + name + "' is not defined", pattern));
            throw new EntryParsingException();
        }
    }

    public static int parseInt(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_INTEGER": return Integer.parseInt(inner.flatten(false));
            case "VARIABLE_MARKER": return retrieveSymbol(pattern, compiler, Integer.class);
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }
    }

    public static double parseDouble(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "RAW_REAL": return Double.parseDouble(inner.flatten(false));
            case "VARIABLE_MARKER": return retrieveSymbol(pattern, compiler, Double.class);
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                throw new EntryParsingException();
            }
        }
    }

    public static Type guessEntityType(Entity entity, TridentCompiler compiler) {
        TypeDictionary dict = compiler.getModule().minecraft.types.entity;
        if(entity instanceof PlayerName) return dict.get("player");
        if(entity instanceof GenericEntity) {
            Selector selector = ((GenericEntity) entity).getSelector();
            List<SelectorArgument> typeArg = selector.getArgumentsByKey("type").toList();
            if(typeArg.isEmpty() || ((TypeArgument) typeArg.get(0)).isNegated()) return null;
            return ((TypeArgument) typeArg.get(0)).getType();
        } else throw new IllegalArgumentException("entity");
    }

    public static NumericNBTType getNumericType(Object body, NBTPath path, TridentCompiler compiler, TokenPattern<?> pattern, boolean strict) {

        PathContext context = new PathContext().setIsSetting(true).setProtocol(body instanceof Entity ? PathProtocol.ENTITY : PathProtocol.BLOCK_ENTITY, body instanceof Entity ? guessEntityType((Entity) body, compiler) : null);

        DataTypeQueryResponse response = compiler.getTypeMap().collectTypeInformation(path, context);
        //Debug.log(response.getPossibleTypes());

        if(response.isEmpty()) {
            if(strict) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Don't know the correct NBT data type for the path '" + path + "'", pattern));
                throw new EntryParsingException();
            } else return null;
        } else {
            if(response.getPossibleTypes().size() > 1 && strict) {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Ambiguous NBT data type for the path '" + path + "': possible types include " + response.getPossibleTypes().map(DataType::getShortTypeName).toSet().join(", ") + ". Assuming " + response.getPossibleTypes().toList().get(0).getShortTypeName(), pattern));
            }
            DataType dataType = response.getPossibleTypes().toList().get(0);
            if(NumericNBTTag.class.isAssignableFrom(dataType.getCorrespondingTagType())) {
                try {
                    NBTTag sample = dataType.getCorrespondingTagType().newInstance();
                    return ((NumericNBTTag) sample).getNumericType();
                } catch (InstantiationException | IllegalAccessException x) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Exception while instantiating default " + dataType.getCorrespondingTagType().getSimpleName() + ": " + x, pattern));
                    throw new EntryParsingException();
                }
            } else if (strict) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected numeric NBT data type, instead got " + dataType.getShortTypeName(), pattern));
                throw new EntryParsingException();
            }
            return null;
        }
    }

    public interface TypeGroupPicker {
        TypeDictionary pick(TypeManager m);
    }

    public interface TagGroupPicker {
        TagGroup<?> pick(TagManager m);
    }
}
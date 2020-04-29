package com.energyxxer.trident.compiler.semantics.custom.items;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataType;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.TypeAwareNBTMerger;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.ArrayList;
import java.util.HashMap;

import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction.parseSymbolDeclaration;
import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfClass;
import static com.energyxxer.trident.compiler.semantics.custom.TypeAwareNBTMerger.REPLACE;
import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CustomItem implements TypeHandler<CustomItem> {
    public static final CustomItem STATIC_HANDLER = new CustomItem();
    private final String id;
    private final String namespace;
    private final Type baseType;
    private TagCompound defaultNBT;
    private boolean useModelData = false;
    private int customModelData = 0;
    private boolean fullyDeclared = false;
    private HashMap<String, Symbol> members = new HashMap<>();

    //EMPTY OBJECT FOR STATIC HANDLER
    private CustomItem() {
        id = null;
        namespace = null;
        baseType = null;
        defaultNBT = new TagCompound();
    }

    public CustomItem(String id, Type baseType, ISymbolContext ctx) {
        this.id = id;
        this.namespace = ctx.getStaticParentFile().getNamespace().getName();
        this.baseType = baseType;
        this.defaultNBT = new TagCompound(new TagInt("TridentCustomItem", getItemIdHash()));
    }

    public String getId() {
        return id;
    }

    public Type getBaseType() {
        return baseType;
    }

    public TagCompound getDefaultNBT() {
        return defaultNBT;
    }

    public void mergeNBT(TagCompound newNBT, ISymbolContext ctx) {
        PathContext context = new PathContext().setIsSetting(true).setProtocolMetadata(baseType);
        this.defaultNBT = ((TypeAwareNBTMerger) (path, cls) -> {
            DataTypeQueryResponse response = ctx.getCompiler().getTypeMap().collectTypeInformation(path, context);
            if (!response.isEmpty()) {
                for (DataType type : new ArrayList<>(response.getPossibleTypes())) {
                    if (type.getFlags() != null && type.getFlags().hasFlag("fixed") && cls.equals(type.getCorrespondingTagType()))
                        return REPLACE;
                }
            }
            return TypeAwareNBTMerger.MERGE;
        }).merge(this.defaultNBT, newNBT);
    }

    public void overrideNBT(TagCompound newNBT, ISymbolContext ctx) {
        this.defaultNBT = ((TypeAwareNBTMerger) (path, cls) -> REPLACE).merge(this.defaultNBT, newNBT);
    }

    public void setDefaultNBT(TagCompound defaultNBT) {
        this.defaultNBT = defaultNBT;
    }

    public boolean isUseModelData() {
        return useModelData;
    }

    public void setUseModelData(boolean useModelData) {
        this.useModelData = useModelData;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData, ISymbolContext ctx) {
        this.customModelData = customModelData;
        mergeNBT(new TagCompound(new TagInt("CustomModelData", customModelData)), ctx);
        setUseModelData(true);
    }

    public boolean isFullyDeclared() {
        return fullyDeclared;
    }

    public void endDeclaration() {
        fullyDeclared = true;
    }

    public Item constructItem(NBTMode mode) {
        return mode == SETTING ? new Item(baseType, getDefaultNBT()) : new Item(baseType, new TagCompound(new TagInt("TridentCustomItem", getItemIdHash())));
    }







    @Override
    public Object getMember(CustomItem object, String member, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        if(members.containsKey(member)) {
            Symbol sym = members.get(member);
            return keepSymbol ? sym : sym.getValue();
        }
        switch (member) {
            case "getSlotNBT":
                return (TridentMethod) (params, patterns, pattern1, file1) -> {
                    TagCompound nbt = new TagCompound(
                            new TagString("id", ((CustomItem) this).getBaseType().toString()),
                            new TagByte("Count", 1));
                    if (((CustomItem) this).getDefaultNBT() != null) {
                        TagCompound tag = ((CustomItem) this).getDefaultNBT().clone();
                        tag.setName("tag");
                        nbt = new TagCompound(tag).merge(nbt);
                    }
                    return nbt;
                };
            case "getItemTag":
                return (TridentMethod) (params, patterns, pattern1, file1) -> {
                    if (((CustomItem) this).getDefaultNBT() != null) {
                        return ((CustomItem) this).getDefaultNBT().clone();
                    }
                    return new TagCompound();
                };
            case "getMatchingNBT":
                return (TridentMethod) (params, patterns, pattern1, file1) -> new TagCompound(new TagInt("TridentCustomItem", getItemIdHash()));
            case "getItem":
                return (TridentMethod) (params, patterns, pattern1, file1) -> new Item(baseType, defaultNBT);
            case "baseType":
                return baseType != null ? new TridentUtil.ResourceLocation(baseType.toString()) : null;
            case "itemCode":
                return getItemIdHash();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomItem object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        String indexStr = TridentMethod.HelperMethods.assertOfClass(index, pattern, ctx, String.class);
        if(members.containsKey(indexStr)) {
            Symbol sym = members.get(indexStr);
            return keepSymbol ? sym : sym.getValue();
        } else if(keepSymbol) {
            Symbol sym;
            members.put(indexStr, sym = new Symbol(indexStr, Symbol.SymbolVisibility.LOCAL, null));
            return sym;
        } else return null;
    }

    @Override
    public Object cast(CustomItem object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext file) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return "[Custom Item: " + id + "]";
    }


















    public static void defineItem(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);

        String itemName = CommonParsers.parseIdentifierA(pattern.find("ITEM_NAME.IDENTIFIER_A"), ctx);
        if(itemName == null) { //Is not an IDENTIFIER_A
            itemName = pattern.find("ITEM_NAME").flatten(false);
        }

        Type defaultType = CommonParsers.parseItemType(pattern.find("ITEM_ID"), ctx);

        final CustomItem itemDecl;
        TokenPattern<?> rawCustomModelData = pattern.find("CUSTOM_MODEL_DATA.INTEGER");

        if(!itemName.equals("default")) {
            itemDecl = new CustomItem(itemName, defaultType, ctx);
            if(rawCustomModelData != null) itemDecl.setCustomModelData(CommonParsers.parseInt(rawCustomModelData, ctx), ctx);

            ctx.putInContextForVisibility(visibility, new Symbol(itemName, visibility, itemDecl));
        } else if(rawCustomModelData != null) {
            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default items don't support custom model data specifiers", rawCustomModelData, ctx);
        } else {
            itemDecl = null;
        }

        ctx = new SymbolContext(ctx);
        final ISymbolContext finalCtx = ctx;
        if(itemDecl != null) ctx.put(new Symbol("this", Symbol.SymbolVisibility.LOCAL, itemDecl));

        ExceptionCollector collector = new ExceptionCollector(ctx);
        collector.begin();

        try {
            TokenList bodyEntries = (TokenList) pattern.find("ITEM_DECLARATION_BODY.ITEM_BODY_ENTRIES");

            if (bodyEntries != null) {
                for (TokenPattern<?> rawEntry : bodyEntries.getContents()) {
                    TokenPattern<?> entry = ((TokenStructure) rawEntry).getContents();
                    switch (entry.getName()) {
                        case "DEFAULT_NBT": {
                            if (itemDecl == null) {
                                collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default items", entry, ctx));
                                break;
                            }
                            TagCompound newNBT = NBTParser.parseCompound(entry.find("NBT_COMPOUND"), ctx);
                            PathContext context = new PathContext().setIsSetting(true).setProtocol(DEFAULT, "ITEM_TAG");
                            NBTParser.analyzeTag(newNBT, context, entry.find("NBT_COMPOUND"), ctx);
                            itemDecl.mergeNBT(newNBT, ctx);
                            break;
                        }
                        case "ITEM_INNER_FUNCTION": {
                            TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), ctx,
                                    itemDecl != null ?
                                            ctx.getParent() instanceof TridentFile &&
                                                    ((TridentFile) ctx.getParent()).getPath().endsWith(itemDecl.id + ".tdn") ?
                                                    null :
                                                    itemDecl.id
                                            :
                                            defaultType != null ?
                                                    "default_" + defaultType.getName() :
                                                    null
                            , false);

                            TokenPattern<?> namePattern = entry.find("OPTIONAL_NAME_INNER_FUNCTION.INNER_FUNCTION_NAME.RESOURCE_LOCATION");
                            if(namePattern != null) {
                                String name = namePattern.flatten(false);
                                Symbol sym = new Symbol(name, Symbol.SymbolVisibility.LOCAL, innerFile.getResourceLocation());
                                if(itemDecl != null) {
                                    itemDecl.members.put(name, sym);
                                } else {
                                    ctx.put(sym);
                                }
                            }

                            ctx.getStaticParentFile().schedulePostResolutionAction(() -> {
                                innerFile.resolveEntries();
                                TokenPattern<?> rawFunctionModifiers = entry.find("INNER_FUNCTION_MODIFIERS");
                                if (rawFunctionModifiers != null) {
                                    TokenPattern<?> modifiers = ((TokenStructure) rawFunctionModifiers).getContents();
                                    switch (modifiers.getName()) {
                                        case "FUNCTION_ON": {

                                            TokenPattern<?> onWhat = ((TokenStructure) modifiers.find("FUNCTION_ON_INNER")).getContents();

                                            boolean pure = false;
                                            if (modifiers.find("LITERAL_PURE") != null) {
                                                if (itemDecl != null) {
                                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "The 'pure' modifier is only allowed for default items", modifiers.find("LITERAL_PURE"), finalCtx));
                                                } else {
                                                    pure = true;
                                                }
                                            }

                                            ArrayList<ExecuteModifier> eventModifiers = CommonParsers.parseModifierList((TokenList) modifiers.find("EVENT_MODIFIERS"), finalCtx);

                                            if (onWhat.getName().equals("ITEM_CRITERIA")) {
                                                finalCtx.assertLanguageLevel(3, "Item events are", entry, collector);

                                                ItemEvent.ItemScoreEventType eventType = ItemEvent.ItemScoreEventType.valueOf(onWhat.find("ITEM_CRITERIA_KEY").flatten(false).toUpperCase());

                                                if((itemDecl == null && eventType.supportsDefaultItems) || (itemDecl != null && eventType.supportsCustomItems)) {
                                                    ((ItemEventFile) finalCtx.getCompiler().getSpecialFileManager().get("item_events")).addCustomItem(
                                                            eventType,
                                                            defaultType,
                                                            itemDecl,
                                                            new ItemEvent(new FunctionReference(innerFile.getFunction()), pure, eventModifiers)
                                                    );
                                                } else {
                                                    collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Item events for stat '" + eventType.toString().toLowerCase() + "' are not supported for " + (itemDecl == null ? "default" : "custom") + " items", onWhat, finalCtx));
                                                }
                                            }
                                        }
                                    }
                                }
                            });

                            break;
                        }
                        case "DEFAULT_NAME": {
                            if (itemDecl == null) {
                                collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default items", entry, ctx));
                                break;
                            }

                            NBTCompoundBuilder builder = new NBTCompoundBuilder();
                            builder.put(new NBTPath("display", new NBTPath("Name")), new TagString("Name", TextParser.parseTextComponent(entry.find("TEXT_COMPONENT"), ctx).toString()));

                            itemDecl.mergeNBT(builder.getCompound(), ctx);
                            break;
                        }
                        case "DEFAULT_LORE": {
                            if (itemDecl == null) {
                                collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default items", entry, ctx));
                                break;
                            }
                            TagList loreList = new TagList("Lore");
                            TagCompound newNBT = new TagCompound("", new TagCompound("display", loreList));

                            TokenList rawLoreList = (TokenList) (entry.find("LORE_LIST"));
                            if (rawLoreList != null) {
                                for (TokenPattern<?> rawLine : rawLoreList.getContents()) {
                                    if (rawLine.getName().equals("TEXT_COMPONENT"))
                                        loreList.add(new TagString(TextParser.parseTextComponent(rawLine, ctx).toString()));
                                }
                            }

                            itemDecl.mergeNBT(newNBT, ctx);
                            break;
                        }
                        case "ITEM_FIELD": {
                            VariableInstruction.SymbolDeclaration decl = parseSymbolDeclaration(entry, ctx);

                            if (itemDecl != null) {
                                itemDecl.members.put(decl.getName(), decl.getSupplier().get());
                            } else {
                                ctx.put(decl.getSupplier().get());
                            }
                            break;
                        }
                        case "ITEM_EVAL": {
                            InterpolationManager.parse(((TokenStructure) entry.find("LINE_SAFE_INTERPOLATION_VALUE")).getContents(), ctx);
                            break;
                        }
                        case "COMMENT": {
                            break;
                        }
                        default: {
                            collector.log(new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx));
                            break;
                        }
                    }
                }
            }
        } catch(TridentException | TridentException.Grouped x) {
            collector.log(x);
        } finally {
            collector.end();
            if(itemDecl != null) itemDecl.endDeclaration();
        }
    }

    @Override
    public Class<CustomItem> getHandledClass() {
        return CustomItem.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "custom_item";
    }

    public int getItemIdHash() {
        return (namespace + ":" + id).hashCode();
    }
}

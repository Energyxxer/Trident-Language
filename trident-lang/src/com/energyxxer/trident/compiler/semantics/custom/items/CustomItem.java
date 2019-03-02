package com.energyxxer.trident.compiler.semantics.custom.items;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CustomItem implements VariableTypeHandler<CustomItem> {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;
    private boolean useModelData = false;
    private int customModelData = 0;
    private boolean fullyDeclared = false;
    private HashMap<String, TridentUtil.ResourceLocation> members = new HashMap<>();

    public CustomItem(String id, Type defaultType) {
        this.id = id;
        this.defaultType = defaultType;
        this.defaultNBT = new TagCompound(new TagInt("TridentCustomItem", getItemIdHash()));
    }

    public String getId() {
        return id;
    }

    public Type getDefaultType() {
        return defaultType;
    }

    public TagCompound getDefaultNBT() {
        return defaultNBT;
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

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        defaultNBT = defaultNBT.merge(new TagCompound(new TagInt("CustomModelData", customModelData)));
        setUseModelData(true);
    }

    public boolean isFullyDeclared() {
        return fullyDeclared;
    }

    public void endDeclaration() {
        fullyDeclared = true;
    }

    public Item constructItem(NBTMode mode) {
        return mode == SETTING ? new Item(defaultType, getDefaultNBT()) : new Item(defaultType, new TagCompound(new TagInt("TridentCustomItem", getItemIdHash())));
    }







    @Override
    public Object getMember(CustomItem object, String member, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        if(members.containsKey(member)) return members.get(member);
        if(member.equals("getSettingNBT")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                TagCompound nbt = new TagCompound(
                        new TagString("id", ((CustomItem) this).getDefaultType().toString()),
                        new TagByte("Count", 1));
                if(((CustomItem) this).getDefaultNBT() != null) {
                    TagCompound tag = ((CustomItem) this).getDefaultNBT().clone();
                    tag.setName("tag");
                    nbt = new TagCompound(tag).merge(nbt);
                }
                return nbt;
            };
        }
        else if(member.equals("getMatchingNBT")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> new TagCompound(new TagInt("TridentCustomItem", getItemIdHash()));
        } else if(member.equals("getItem")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> new Item(defaultType, defaultNBT);
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomItem object, Object index, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(CustomItem object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext file) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return "[Custom Item: " + id + "]";
    }


















    public static void defineItem(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);

        String entityName = pattern.find("ITEM_NAME").flatten(false);
        Type defaultType = CommonParsers.parseItemType(pattern.find("ITEM_ID"), ctx);

        CustomItem itemDecl = null;
        TokenPattern<?> rawCustomModelData = pattern.find("CUSTOM_MODEL_DATA.INTEGER");

        if(!entityName.equals("default")) {
            itemDecl = new CustomItem(entityName, defaultType);
            if(rawCustomModelData != null) itemDecl.setCustomModelData(CommonParsers.parseInt(rawCustomModelData, ctx));

            ctx.putInContextForVisibility(visibility, new Symbol(entityName, visibility, itemDecl));
        } else if(rawCustomModelData != null) {
            throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default items don't support custom model data specifiers", rawCustomModelData, ctx);
        }



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
                            itemDecl.defaultNBT = itemDecl.defaultNBT.merge(newNBT);
                            break;
                        }
                        case "ITEM_INNER_FUNCTION": {
                            TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), ctx);
                            TokenPattern<?> namePattern = entry.find("OPTIONAL_NAME_INNER_FUNCTION.INNER_FUNCTION_NAME.RESOURCE_LOCATION");
                            if(itemDecl != null && namePattern != null) {
                                itemDecl.members.put(namePattern.flatten(false), innerFile.getResourceLocation());
                            }

                            TokenPattern<?> rawFunctionModifiers = entry.find("INNER_FUNCTION_MODIFIERS");
                            if (rawFunctionModifiers != null) {
                                TokenPattern<?> modifiers = ((TokenStructure) rawFunctionModifiers).getContents();
                                switch (modifiers.getName()) {
                                    case "FUNCTION_ON": {

                                        TokenPattern<?> onWhat = ((TokenStructure) modifiers.find("FUNCTION_ON_INNER")).getContents();

                                        boolean pure = false;
                                        if (modifiers.find("LITERAL_PURE") != null) {
                                            if (itemDecl != null) {
                                                collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "The 'pure' modifier is only allowed for default items", modifiers.find("LITERAL_PURE"), ctx));
                                            } else {
                                                pure = true;
                                            }
                                        }

                                        if (onWhat.getName().equals("ITEM_CRITERIA")) {
                                            ctx.assertLanguageLevel(3, "Custom non-default item events are", entry, collector);

                                            ((ItemEventFile) ctx.getCompiler().getSpecialFileManager().get("item_events")).addCustomItem(ItemEvent.ItemScoreEventType.valueOf(onWhat.find("ITEM_CRITERIA_KEY").flatten(false).toUpperCase()), defaultType, itemDecl, new ItemEvent(new FunctionReference(innerFile.getFunction()), pure));

                                        }
                                    }
                                }
                            }

                            break;
                        }
                        case "DEFAULT_NAME": {
                            if (itemDecl == null) {
                                collector.log(new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Default NBT isn't allowed for default items", entry, ctx));
                                break;
                            }

                            NBTCompoundBuilder builder = new NBTCompoundBuilder();
                            builder.put(new NBTPath("display", new NBTPath("Name")), new TagString("Name", TextParser.parseTextComponent(entry.find("TEXT_COMPONENT"), ctx).toString()));

                            itemDecl.defaultNBT = itemDecl.defaultNBT.merge(builder.getCompound());
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

                            itemDecl.defaultNBT = itemDecl.defaultNBT.merge(newNBT);
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

    public int getItemIdHash() {
        return id.hashCode();
    }
}

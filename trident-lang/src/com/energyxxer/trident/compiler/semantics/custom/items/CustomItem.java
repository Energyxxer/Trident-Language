package com.energyxxer.trident.compiler.semantics.custom.items;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEvent;

import static com.energyxxer.trident.compiler.semantics.custom.items.NBTMode.SETTING;

public class CustomItem {
    private final String id;
    private final Type defaultType;
    private TagCompound defaultNBT;
    private boolean useModelData = false;
    private int customModelData = 0;
    private boolean fullyDeclared = false;

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

    public static void defineItem(TokenPattern<?> pattern, TridentFile file) {
        String entityName = pattern.find("ITEM_NAME").flatten(false);
        Type defaultType = CommonParsers.parseItemType(pattern.find("ITEM_ID"), file.getCompiler());

        CustomItem itemDecl = null;
        var rawCustomModelData = pattern.find("CUSTOM_MODEL_DATA.INTEGER");

        if(!entityName.equals("default")) {
            itemDecl = new CustomItem(entityName, defaultType);
            if(rawCustomModelData != null) itemDecl.setCustomModelData(CommonParsers.parseInt(rawCustomModelData, file.getCompiler()));

            SymbolTable table = file.getCompiler().getStack().getGlobal();
            table.put(new Symbol(entityName, Symbol.SymbolAccess.GLOBAL, itemDecl));
        } else if(rawCustomModelData != null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default items don't support custom model data specifiers", rawCustomModelData));
        }

        var bodyEntries = (TokenList) pattern.find("ITEM_DECLARATION_BODY.ITEM_BODY_ENTRIES");

        if(bodyEntries != null) {
            for(var rawEntry : bodyEntries.getContents()) {
                var entry = ((TokenStructure) rawEntry).getContents();
                switch(entry.getName()) {
                    case "DEFAULT_NBT": {
                        if(itemDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default NBT isn't allowed for default items", entry));
                            break;
                        }
                        itemDecl.defaultNBT = itemDecl.defaultNBT.merge(NBTParser.parseCompound(entry.find("NBT_COMPOUND"), file.getCompiler()));
                        break;
                    }
                    case "ITEM_INNER_FUNCTION": {
                        TridentFile innerFile = TridentFile.createInnerFile(entry.find("OPTIONAL_NAME_INNER_FUNCTION"), file);

                        var rawFunctionModifiers = entry.find("INNER_FUNCTION_MODIFIERS");
                        if(rawFunctionModifiers != null) {
                            var modifiers = ((TokenStructure)rawFunctionModifiers).getContents();
                            switch(modifiers.getName()) {
                                case "FUNCTION_ON": {

                                    var onWhat = ((TokenStructure)modifiers.find("FUNCTION_ON_INNER")).getContents();

                                    boolean pure = false;
                                    if(modifiers.find("LITERAL_PURE") != null) {
                                        if(itemDecl != null) {
                                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "The 'pure' modifier is only allowed for default items", modifiers.find("LITERAL_PURE")));
                                        } else {
                                            pure = true;
                                        }
                                    }

                                    if(onWhat.getName().equals("ITEM_CRITERIA")) {
                                        if(itemDecl != null && file.getLanguageLevel() < 2) {
                                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Custom non-default item events are only supported in language level 2 and up", entry));
                                            break;
                                        }

                                        file.getCompiler().getSpecialFileManager().itemEvents.addCustomItem(ItemEvent.ItemScoreEventType.valueOf(onWhat.find("ITEM_CRITERIA_KEY").flatten(false).toUpperCase()), defaultType, itemDecl, new ItemEvent(new FunctionReference(innerFile.getFunction()), pure));


                                        /*String criteriaKey = ;

                                        String criteria = "minecraft." + criteriaKey + ":" + defaultType.toString().replace(':','.');

                                        Objective objective = file.getCompiler().getModule().getObjectiveManager().create(criteriaKey.charAt(0) + "item." + new TridentUtil.ResourceLocation(defaultType.toString()).body.hashCode(), criteria, new StringTextComponent(criteriaKey + " item " + defaultType), true);

                                        ScoreArgument scores = new ScoreArgument();
                                        scores.put(objective, new NumberRange<>(1, null));

                                        file.getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(new GenericEntity(new Selector(Selector.BaseSelector.ALL_PLAYERS, scores))), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));
                                        file.getTickFunction().append(new ScoreReset(new GenericEntity(new Selector(Selector.BaseSelector.ALL_PLAYERS, scores)), objective));*/
                                    }
                                }
                            }
                        }

                        break;
                    }
                    case "DEFAULT_NAME": {
                        if(itemDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default NBT isn't allowed for default items", entry));
                            break;
                        }

                        NBTCompoundBuilder builder = new NBTCompoundBuilder();
                        builder.put(new NBTPath("display",new NBTPath("Name")), new TagString("Name", TextParser.parseTextComponent(entry.find("TEXT_COMPONENT"), file.getCompiler()).toString()));

                        itemDecl.defaultNBT = itemDecl.defaultNBT.merge(builder.getCompound());
                        break;
                    }
                    case "DEFAULT_LORE": {
                        if(itemDecl == null) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Default NBT isn't allowed for default items", entry));
                            break;
                        }
                        TagList loreList = new TagList("Lore");
                        TagCompound newNBT = new TagCompound("", new TagCompound("display", loreList));

                        TokenList rawLoreList = (TokenList)(entry.find("LORE_LIST"));
                        if(rawLoreList != null) {
                            for(TokenPattern<?> rawLine : rawLoreList.getContents()) {
                                if(rawLine.getName().equals("TEXT_COMPONENT")) loreList.add(new TagString(TextParser.parseTextComponent(rawLine, file.getCompiler()).toString()));
                            }
                        }

                        itemDecl.defaultNBT = itemDecl.defaultNBT.merge(newNBT);
                        break;
                    }
                    default:
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + entry.getName() + "'", entry));
                }
            }
        }

        if(itemDecl != null) itemDecl.endDeclaration();
    }

    public int getItemIdHash() {
        return id.hashCode();
    }
}

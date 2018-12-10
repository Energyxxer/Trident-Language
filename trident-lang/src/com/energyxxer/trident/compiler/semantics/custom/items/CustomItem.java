package com.energyxxer.trident.compiler.semantics.custom.items;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAsEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteAtEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreAdd;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.nio.file.Path;

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
        setUseModelData(true);
    }

    public boolean isFullyDeclared() {
        return fullyDeclared;
    }

    public void endDeclaration() {
        fullyDeclared = true;
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
                        itemDecl.setDefaultNBT(NBTParser.parseCompound(entry.find("NBT_COMPOUND"), file.getCompiler()));
                        break;
                    }
                    case "INNER_FUNCTION": {
                        String functionName = new TridentUtil.ResourceLocation(entry.find("INNER_FUNCTION_NAME").flatten(false)).body;

                        var innerFilePattern = entry.find("FILE_INNER");
                        String innerFilePathRaw = file.getPath().toString();
                        innerFilePathRaw = innerFilePathRaw.substring(0, innerFilePathRaw.length()-".tdn".length());

                        TridentFile innerFile = new TridentFile(file.getCompiler(), Path.of(innerFilePathRaw).resolve(functionName + ".tdn"), innerFilePattern);
                        innerFile.resolveEntries();

                        var rawFunctionModifiers = entry.find("INNER_FUNCTION_MODIFIERS");
                        if(rawFunctionModifiers != null) {
                            var modifiers = ((TokenStructure)rawFunctionModifiers).getContents();
                            switch(modifiers.getName()) {
                                case "FUNCTION_ON": {

                                    var onWhat = ((TokenStructure)modifiers.find("FUNCTION_ON_INNER")).getContents();

                                    if(onWhat.getName().equals("ITEM_CRITERIA")) {
                                        if(itemDecl != null && file.getLanguageLevel() < 2) {
                                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Custom non-default item events are only supported in language level 2 and up", entry));
                                            break;
                                        }

                                        String criteriaKey = onWhat.find("ITEM_CRITERIA_KEY").flatten(false);

                                        String criteria = "minecraft." + criteriaKey + ":" + defaultType.toString().replace(':','.');

                                        Objective objective = file.getCompiler().getModule().getObjectiveManager().create(criteriaKey.charAt(0) + "item." + new TridentUtil.ResourceLocation(defaultType.toString()).body.hashCode(), criteria, new StringTextComponent(criteriaKey + " item " + defaultType), true);

                                        ScoreArgument scores = new ScoreArgument();
                                        scores.put(objective, new NumberRange<>(1, null));

                                        file.getTickFunction().append(new ExecuteCommand(new FunctionCommand(innerFile.getFunction()), new ExecuteAsEntity(new GenericEntity(new Selector(Selector.BaseSelector.ALL_PLAYERS, scores))), new ExecuteAtEntity(new GenericEntity(new Selector(Selector.BaseSelector.SENDER)))));
                                        file.getTickFunction().append(new ScoreAdd(new LocalScore(new GenericEntity(new Selector(Selector.BaseSelector.ALL_PLAYERS, scores)), objective), -1));
                                    }
                                }
                            }
                        }

                        break;
                    }
                    default:
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + entry.getName() + "'", entry));
                }
            }
        }

        if(itemDecl != null) itemDecl.endDeclaration();
    }
}

package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionContext;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.plugins.PrismarinePluginUnit;
import com.energyxxer.prismarine.plugins.syntax.PrismarineMetaBuilder;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunctionBranch;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.trident.TridentLiteralSet;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.energyxxer.trident.compiler.plugin.TDNMetaBuilder.*;

public class PluginCommandParser {
    private static final ExecutionContext DEFAULT_EXEC_CONTEXT = new ExecutionContext();

    private HashSet<String> multiVars = new HashSet<>();

    public void handleCommand(PrismarinePluginUnit def, TokenPattern<?> pattern, List<ExecuteModifier> modifiers, ISymbolContext ctx, FunctionSection appendTo) {
        ISymbolContext subContext = new PluginSymbolContext(ctx, Paths.get(def.get(TridentPluginUnitConfiguration.CommandHandlerFile.INSTANCE).getPattern().getSource().getFullPath()));
        DictionaryObject argsObj = new DictionaryObject(ctx.getTypeSystem());
        ListObject modifiersList = new ListObject(ctx.getTypeSystem());
        for(ExecuteModifier modifier : modifiers) {
            modifiersList.add(modifier.getSubCommand(DEFAULT_EXEC_CONTEXT).getRaw());
        }
        subContext.put(new Symbol("args", TridentSymbolVisibility.LOCAL, argsObj));
        subContext.put(new Symbol("modifiers", TridentSymbolVisibility.LOCAL, modifiersList));

        scanPattern(pattern, argsObj, ctx);

        TridentFile.resolveFileIntoSection(def.get(TridentPluginUnitConfiguration.CommandHandlerFile.INSTANCE).getPattern(), subContext, appendTo);
    }

    private void scanPattern(TokenPattern<?> pattern, DictionaryObject argsObj, ISymbolContext ctx) {
        while(pattern.hasTag(PrismarineMetaBuilder.PLUGIN_CREATED_TAG)) {
            boolean storingInVar = false;
            if(pattern.getTags() != null) {
                for(String tag : pattern.getTags()) {
                    if(tag.startsWith(STORE_VAR_TAG_PREFIX)) {
                        storingInVar = true;
                        String storeVar = tag.substring(STORE_VAR_TAG_PREFIX.length());
                        TokenPattern<?>[] contents = ((TokenGroup) pattern).getContents();
                        if(contents.length == 0) continue;
                        TokenPattern<?> argPattern = contents[0];

                        Object value = null;
                        for(String tag2 : pattern.getTags()) {
                            if(tag2.startsWith(STORE_FLAT_TAG_PREFIX)) {
                                value = pattern.flatten(tag2.substring(STORE_FLAT_TAG_PREFIX.length()));
                                break;
                            }
                        }
                        if(value == null) {
                            value = parseVar(argPattern, ctx, pattern);
                        }

                        if(!argsObj.containsKey(storeVar)) {
                            argsObj.put(storeVar, value);
                        } else if(multiVars.contains(storeVar)) {
                            ((ListObject) argsObj.get(storeVar)).add(value);
                        } else {
                            ListObject newList = new ListObject(ctx.getTypeSystem());
                            newList.add(argsObj.get(storeVar));
                            newList.add(value);
                            argsObj.put(storeVar, newList);
                            multiVars.add(storeVar);
                        }
                    }
                }
            }
            if(pattern instanceof TokenStructure) {
                pattern = ((TokenStructure) pattern).getContents();
                continue;
            }
            if(pattern instanceof TokenGroup || pattern instanceof TokenList) {
                TokenPattern<?>[] contents = ((TokenPattern<?>[]) pattern.getContents());
                for(TokenPattern<?> subPattern : contents) {
                    scanPattern(subPattern, argsObj, ctx);
                }
                break;
            }
            break;
        }
    }

    private static Object parseVar(TokenPattern<?> pattern, ISymbolContext ctx, TokenPattern<?> parentPattern) {
        switch(pattern.getName()) {
            case "INNER_FUNCTION":
            case "OPTIONAL_NAME_INNER_FUNCTION": {
                TridentFile file = TridentFile.createInnerFile(pattern, ctx);
                return file.getResourceLocation();
            }
            case "ANONYMOUS_INNER_FUNCTION": {
                PrismarineFunction function = new PrismarineFunction(null, new TridentUserFunctionBranch(ctx.getTypeSystem(), Collections.emptyList(), pattern, null), ctx);
                if(parentPattern.hasTag(STORE_METADATA_TAG_PREFIX + "STATS")) {
                    DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());

                    getInnerFunctionStats(dict, pattern);

                    dict.put("call", function);
                    return dict;
                }
                return function;
            }
            case "MODIFIER": {
                Object returnedModifiers = pattern.evaluate(ctx, null);
                StringBuilder sb = new StringBuilder();

                if(returnedModifiers instanceof ExecuteModifier) {
                    sb.append(((ExecuteModifier) returnedModifiers).getSubCommand(DEFAULT_EXEC_CONTEXT).getRaw());
                } else {
                    boolean first = true;
                    for(ExecuteModifier modifier : ((Collection<ExecuteModifier>) returnedModifiers)) {
                        if(!first) sb.append(' ');
                        sb.append(modifier.getSubCommand(DEFAULT_EXEC_CONTEXT).getRaw());
                        first = false;
                    }
                }

                return sb.toString();
            }
            case "ENTITY":
            case "SELECTOR":
            case "IDENTIFIER_B":
            case "IDENTIFIER_A":
            case "POINTER":
            case "REAL_NUMBER_RANGE":
            case "INTEGER_NUMBER_RANGE":
            case "INTEGER":
            case "REAL":
            case "NBT_COMPOUND":
            case "NBT_VALUE":
            case "NBT_LIST":
            case "NBT_PATH":
            case "TEXT_COMPONENT":
            case "RESOURCE_LOCATION":
            case "RESOURCE_LOCATION_TAGGED":
            case "BLOCK":
            case "BLOCK_TAGGED":
            case "BOOLEAN":
            case "ROTATION":
            case "UUID":
            case "STRING_LITERAL_OR_IDENTIFIER_A":
            case "STRING":
            case "DICTIONARY":
            case "LIST":
            case "INTERPOLATION_BLOCK":
            case "INTERPOLATION_VALUE":
            case "COORDINATE_SET":
            case "TWO_COORDINATE_SET":
                return pattern.evaluate(ctx, null);
            case "ITEM":
            case "ITEM_TAGGED": {
                NBTMode mode = NBTMode.SETTING;
                if(parentPattern.hasTag(STORE_METADATA_TAG_PREFIX + "TESTING")) mode = NBTMode.TESTING;
                return pattern.evaluate(ctx, new Object[] {mode});
            }
            case "NEW_ENTITY_LITERAL": {
                TridentLiteralSet.SummonData summonData = (TridentLiteralSet.SummonData) pattern.evaluate(ctx, null);
                DictionaryObject newEntityDict = new DictionaryObject(ctx.getTypeSystem());
                newEntityDict.put("type", new ResourceLocation(summonData.type.toString()));
                newEntityDict.put("components", new ListObject(ctx.getTypeSystem(), summonData.components));
                newEntityDict.put("fullNBT", summonData.nbt);
                return newEntityDict;
            }
            case "BLOCK_ID":
            case "ITEM_ID":
            case "EFFECT_ID":
            case "PARTICLE_ID":
            case "ENCHANTMENT_ID":
            case "DIMENSION_ID":
            case "BIOME_ID":
            case "ATTRIBUTE_ID":
                return new ResourceLocation((Type) pattern.evaluate(ctx, null));
            case "TRIDENT_ENTITY_ID_NBT":
            case "TRIDENT_ENTITY_ID_TAGGED":
            case "ENTITY_ID":
            case "ENTITY_ID_TAGGED": {
                Object ref = pattern.evaluate(ctx, null);
                if(ref instanceof Type) ref = new ResourceLocation((Type) ref);
                return ref;
            }
            case "SLOT_ID":
            case "GAMEMODE":
            case "GAMEMODE_ID":
            case "GAMERULE":
            case "GAMERULE_ID":
            case "STRUCTURE":
            case "STRUCTURE_ID":
            case "DIFFICULTY": return ((Type) pattern.evaluate(ctx, null)).toString();
            case "OBJECTIVE_NAME": return pattern.evaluate(ctx, new Object[] {String.class});
            case "SCORE":
            case "SCORE_OPTIONAL_OBJECTIVE": {
                LocalScore score = (LocalScore) pattern.evaluate(ctx, null);
                String objectiveName = null;
                if(score.getObjective() != null) objectiveName = score.getObjective().getName();
                return new PointerObject(ctx.getTypeSystem(), score.getHolder(), objectiveName);
            }
            case "IDENTIFIER_C":
            case "IDENTIFIER_D":
            case "IDENTIFIER":
            case "TEXT_COLOR":
            case "NUMERIC_DATA_TYPE":
            case "TRAILING_STRING":
            case "ANCHOR": return pattern.flatten(false);
            default: throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Don't know how to store this into a variable: '" + pattern.getName() + "'", pattern, ctx);
        }
    }

    public static class PluginSymbolContext extends SymbolContext {

        private Path declaringPath;

        public PluginSymbolContext(ISymbolContext parentScope, Path declaringPath) {
            super(parentScope);

            this.declaringPath = declaringPath;
        }

        @Override
        public Path getPathFromRoot() {
            return declaringPath;
        }
    }

    private static void getInnerFunctionStats(DictionaryObject dict, TokenPattern<?> pattern) {
        pattern = pattern.find("FILE_INNER.ENTRIES");
        int entryCount = 0;
        int commandCount = 0;
        int instructionCount = 0;
        int commentCount = 0;
        if(pattern != null) {
            for(TokenPattern<?> entry : ((TokenList) pattern).getContents()) {
                TokenPattern<?> inner = ((TokenStructure) entry).getContents();

                switch(inner.getName()) {
                    case "COMMAND_WRAPPER": {
                        commandCount++;
                        break;
                    }
                    case "INSTRUCTION": {
                        instructionCount++;
                        break;
                    }
                    case "COMMENT": {
                        commentCount++;
                        break;
                    }
                }
                entryCount++;
            }
        }
        dict.put("entryCount", entryCount);
        dict.put("commandCount", commandCount);
        dict.put("instructionCount", instructionCount);
        dict.put("commentCount", commentCount);
    }
}

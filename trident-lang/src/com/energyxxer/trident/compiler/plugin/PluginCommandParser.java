package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.commodore.functionlogic.inspection.ExecutionContext;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.FunctionMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.PointerObject;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class PluginCommandParser {
    private static final ExecutionContext DEFAULT_EXEC_CONTEXT = new ExecutionContext(new Selector(Selector.BaseSelector.SENDER));

    private HashSet<String> multiVars = new HashSet<>();

    public void handleCommand(CommandDefinition def, TokenPattern<?> pattern, List<ExecuteModifier> modifiers, ISymbolContext ctx, FunctionSection appendTo) {
        ISymbolContext subContext = new SymbolContext(ctx);
        DictionaryObject argsObj = new DictionaryObject();
        ListObject modifiersList = new ListObject();
        for(ExecuteModifier modifier : modifiers) {
            modifiersList.add(modifier.getSubCommand(DEFAULT_EXEC_CONTEXT).getRaw());
        }
        subContext.put(new Symbol("args", Symbol.SymbolVisibility.LOCAL, argsObj));
        subContext.put(new Symbol("modifiers", Symbol.SymbolVisibility.LOCAL, modifiersList));
        Debug.log("Handling command '" + def.getCommandName() + "'");

        scanPattern(((TokenGroup) ((TokenStructure) pattern).getContents()).getContents()[1], argsObj, subContext);

        TridentFile.resolveFileIntoSection(def.getRawHandlerPattern(), subContext, appendTo);
    }

    private void scanPattern(TokenPattern<?> pattern, DictionaryObject argsObj, ISymbolContext ctx) {
        while(pattern.hasTag(TDNMetaBuilder.PLUGIN_CREATED_TAG)) {
            boolean storingInVar = false;
            for(String tag : pattern.getTags()) {
                if(tag.startsWith(TDNMetaBuilder.STORE_VAR_TAG_PREFIX)) {
                    storingInVar = true;
                    String storeVar = tag.substring(TDNMetaBuilder.STORE_VAR_TAG_PREFIX.length());
                    TokenPattern<?> argPattern = ((TokenGroup) pattern).getContents()[0];
                    Debug.log("STORE IN VAR '" + storeVar + "': PARSE: " + argPattern);

                    Object value;
                    if(pattern.hasTag(TDNMetaBuilder.STORE_FLAT_TAG)) {
                        value = pattern.flatten(true);
                    } else {
                        value = parseVar(argPattern, ctx);
                    }

                    if(!argsObj.containsKey(storeVar)) {
                        argsObj.put(storeVar, value);
                    } else if(multiVars.contains(storeVar)) {
                        ((ListObject) argsObj.get(storeVar)).add(value);
                    } else {
                        ListObject newList = new ListObject();
                        newList.add(argsObj.get(storeVar));
                        newList.add(value);
                        argsObj.put(storeVar, newList);
                        multiVars.add(storeVar);
                    }
                }
            }
            if(!storingInVar) {
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
            }
            break;
        }
    }

    private static Object parseVar(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "INNER_FUNCTION":
            case "OPTIONAL_NAME_INNER_FUNCTION": {
                TridentFile file = TridentFile.createInnerFile(pattern, ctx);
                return file.getResourceLocation();
            }
            case "ANONYMOUS_INNER_FUNCTION": {
                return new FunctionMethod(pattern, ctx, Collections.emptyList(), null, null);
            }
            case "MODIFIER": {
                Collection<ExecuteModifier> modifiers = CommonParsers.parseModifier(pattern, ctx, null);
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(ExecuteModifier modifier : modifiers) {
                    if(!first) sb.append(' ');
                    sb.append(modifier.getSubCommand(DEFAULT_EXEC_CONTEXT).getRaw());
                    first = false;
                }
                return sb.toString();
            }
            case "RESOURCE_LOCATION":
            case "RESOURCE_LOCATION_TAGGED": return CommonParsers.parseResourceLocation(pattern, ctx);
            case "ENTITY":
            case "SELECTOR": return EntityParser.parseEntity(pattern, ctx);
            case "TEXT_COMPONENT": return TextParser.parseTextComponent(pattern, ctx);
            case "NBT_COMPOUND":
            case "NBT_VALUE":
            case "NBT_LIST": return NBTParser.parseValue(pattern, ctx);
            case "NBT_PATH": return NBTParser.parsePath(pattern, ctx);
            case "COORDINATE_SET":
            case "TWO_COORDINATE_SET": return CoordinateParser.parse(pattern, ctx);
            case "BLOCK_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseBlockType(pattern, ctx).toString());
            case "ITEM_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseItemType(pattern, ctx).toString());
            case "ENTITY_ID":
            case "ENTITY_ID_TAGGED": {
                Object ref = CommonParsers.parseEntityReference(pattern, ctx);
                if(ref instanceof Type) ref = new TridentUtil.ResourceLocation(ref.toString());
                return ref;
            }
            case "EFFECT_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseType(pattern, ctx, EffectType.CATEGORY).toString());
            case "PARTICLE_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseType(pattern, ctx, ParticleType.CATEGORY).toString());
            case "ENCHANTMENT_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseType(pattern, ctx, EnchantmentType.CATEGORY).toString());
            case "DIMENSION_ID": return new TridentUtil.ResourceLocation(CommonParsers.parseType(pattern, ctx, DimensionType.CATEGORY).toString());
            case "SLOT_ID": return CommonParsers.parseType(pattern, ctx, ItemSlot.CATEGORY).toString();
            case "GAMEMODE": return CommonParsers.parseType(pattern, ctx, GamemodeType.CATEGORY).toString();
            case "GAMERULE": return CommonParsers.parseType(pattern, ctx, GameruleType.CATEGORY).toString();
            case "STRUCTURE": return CommonParsers.parseType(pattern, ctx, StructureType.CATEGORY).toString();
            case "DIFFICULTY": return CommonParsers.parseType(pattern, ctx, DifficultyType.CATEGORY).toString();
            case "STRING_LITERAL_OR_IDENTIFIER_A": return CommonParsers.parseStringLiteralOrIdentifierA(pattern, ctx);
            case "DICTIONARY":
            case "LIST":
            case "INTERPOLATION_BLOCK":
            case "INTERPOLATION_VALUE":
            case "LINE_SAFE_INTERPOLATION_VALUE": return InterpolationManager.parse(pattern, ctx);
            case "POINTER": return CommonParsers.parsePointer(pattern, ctx);
            case "INTEGER": return CommonParsers.parseInt(pattern, ctx);
            case "BOOLEAN": return pattern.flatten(false).equals("true");
            case "REAL": return CommonParsers.parseDouble(pattern, ctx);
            case "INTEGER_NUMBER_RANGE": return CommonParsers.parseIntRange(pattern, ctx);
            case "REAL_NUMBER_RANGE": return CommonParsers.parseRealRange(pattern, ctx);
            case "OBJECTIVE_NAME": return CommonParsers.parseObjective(pattern, ctx).getName();
            case "SCORE":
            case "SCORE_OPTIONAL_OBJECTIVE": {
                LocalScore score = CommonParsers.parseScore(pattern, ctx);
                String objectiveName = null;
                if(score.getObjective() != null) objectiveName = score.getObjective().getName();
                return new PointerObject(score.getHolder(), objectiveName);
            }
            case "STRING": return CommonParsers.parseStringLiteral(pattern, ctx);
            case "IDENTIFIER_A": return CommonParsers.parseIdentifierA(pattern, ctx);
            case "IDENTIFIER_B": return CommonParsers.parseIdentifierB(pattern, ctx);
            case "IDENTIFIER_C":
            case "IDENTIFIER_D":
            case "IDENTIFIER":
            case "TEXT_COLOR":
            case "NUMERIC_DATA_TYPE":
            case "ANCHOR": return pattern.flatten(false);
            default: return null;
        }
    }
}

package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.commodore.types.defaults.*;
import com.energyxxer.commodore.versioning.ThreeNumberVersion;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.StandardTags;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenItem;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.ComplexSuggestion;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.lexer.summaries.SummarySymbol;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.plugin.TDNMetaBuilder;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

@SuppressWarnings("FieldCanBeLocal")
public class TridentProductions {


    public final LazyTokenStructureMatch FILE;
    private final LazyTokenStructureMatch FILE_INNER;
    private final LazyTokenStructureMatch INNER_FUNCTION;
    private final LazyTokenStructureMatch ANONYMOUS_INNER_FUNCTION;
    private final LazyTokenStructureMatch OPTIONAL_NAME_INNER_FUNCTION;
    private final LazyTokenStructureMatch ENTRY;

    private final LazyTokenItemMatch COMMENT_S;
    private final LazyTokenGroupMatch DIRECTIVE;
    private final LazyTokenStructureMatch INSTRUCTION;
    private final LazyTokenStructureMatch COMMAND;
    private final LazyTokenStructureMatch MODIFIER;

    private final LazyTokenStructureMatch RESOURCE_LOCATION_S;
    private final LazyTokenStructureMatch RESOURCE_LOCATION_TAGGED;

    private final LazyTokenStructureMatch SELECTOR;
    private final LazyTokenStructureMatch SELECTOR_ARGUMENT;
    public final LazyTokenStructureMatch TEXT_COMPONENT;

    private final LazyTokenStructureMatch PLAYER_NAME;

    private final LazyTokenStructureMatch TEXT_COLOR;

    private final LazyTokenStructureMatch INTEGER_NUMBER_RANGE = new LazyTokenStructureMatch("INTEGER_NUMBER_RANGE");
    private final LazyTokenStructureMatch REAL_NUMBER_RANGE = new LazyTokenStructureMatch("REAL_NUMBER_RANGE");

    public final LazyTokenStructureMatch NBT_COMPOUND = new LazyTokenStructureMatch("NBT_COMPOUND");
    private final LazyTokenStructureMatch NBT_LIST = new LazyTokenStructureMatch("NBT_LIST");
    private final LazyTokenStructureMatch NBT_VALUE = new LazyTokenStructureMatch("NBT_VALUE");

    private final LazyTokenStructureMatch NBT_PATH = new LazyTokenStructureMatch("NBT_PATH");

    private final LazyTokenStructureMatch SINGLE_COORDINATE = new LazyTokenStructureMatch("SINGLE_COORDINATE");
    private final LazyTokenStructureMatch ABSOLUTE_COORDINATE = new LazyTokenStructureMatch("ABSOLUTE_COORDINATE");
    private final LazyTokenStructureMatch RELATIVE_COORDINATE = new LazyTokenStructureMatch("RELATIVE_COORDINATE");
    private final LazyTokenStructureMatch LOCAL_COORDINATE = new LazyTokenStructureMatch("LOCAL_COORDINATE");
    private final LazyTokenStructureMatch MIXABLE_COORDINATE = new LazyTokenStructureMatch("MIXABLE_COORDINATE");
    private final LazyTokenStructureMatch COORDINATE_SET = new LazyTokenStructureMatch("COORDINATE_SET");
    private final LazyTokenStructureMatch TWO_COORDINATE_SET = new LazyTokenStructureMatch("TWO_COORDINATE_SET");
    private final LazyTokenStructureMatch ROTATION = new LazyTokenStructureMatch("ROTATION");

    private final LazyTokenStructureMatch BLOCKSTATE = new LazyTokenStructureMatch("BLOCKSTATE");
    private final LazyTokenStructureMatch BLOCK = new LazyTokenStructureMatch("BLOCK");
    private final LazyTokenStructureMatch BLOCK_TAGGED = new LazyTokenStructureMatch("BLOCK_TAGGED");
    private final LazyTokenStructureMatch ITEM = new LazyTokenStructureMatch("ITEM");
    private final LazyTokenStructureMatch ITEM_TAGGED = new LazyTokenStructureMatch("ITEM_TAGGED");

    private final LazyTokenStructureMatch PARTICLE = new LazyTokenStructureMatch("PARTICLE");

    private final LazyTokenStructureMatch UUID = new LazyTokenStructureMatch("UUID");

    private final LazyTokenGroupMatch NEW_ENTITY_LITERAL;

    private final LazyTokenStructureMatch BLOCK_ID = new LazyTokenStructureMatch("BLOCK_ID");
    private final LazyTokenStructureMatch ITEM_ID = new LazyTokenStructureMatch("ITEM_ID");
    private final LazyTokenStructureMatch ENTITY_ID = new LazyTokenStructureMatch("ENTITY_ID");
    private final LazyTokenStructureMatch ENTITY_ID_TAGGED = new LazyTokenStructureMatch("ENTITY_ID_TAGGED");
    private final LazyTokenStructureMatch EFFECT_ID = new LazyTokenStructureMatch("EFFECT_ID");
    private final LazyTokenStructureMatch PARTICLE_ID = new LazyTokenStructureMatch("PARTICLE_ID");
    private final LazyTokenStructureMatch ENCHANTMENT_ID = new LazyTokenStructureMatch("ENCHANTMENT_ID");
    private final LazyTokenStructureMatch DIMENSION_ID = new LazyTokenStructureMatch("DIMENSION_ID");
    private final LazyTokenStructureMatch ATTRIBUTE_ID = new LazyTokenStructureMatch("ATTRIBUTE_ID");
    private final LazyTokenStructureMatch BIOME_ID = new LazyTokenStructureMatch("BIOME_ID");
    private final LazyTokenStructureMatch SLOT_ID = new LazyTokenStructureMatch("SLOT_ID");

    private final LazyTokenStructureMatch GAMEMODE = new LazyTokenStructureMatch("GAMEMODE");
    private final LazyTokenStructureMatch GAMERULE = new LazyTokenStructureMatch("GAMERULE");
    private final LazyTokenStructureMatch GAMERULE_SETTER = new LazyTokenStructureMatch("GAMERULE_SETTER");
    private final LazyTokenStructureMatch STRUCTURE = new LazyTokenStructureMatch("STRUCTURE");
    private final LazyTokenStructureMatch DIFFICULTY = new LazyTokenStructureMatch("DIFFICULTY");

    private final LazyTokenStructureMatch STRING_LITERAL_OR_IDENTIFIER_A = new LazyTokenStructureMatch("STRING_LITERAL_OR_IDENTIFIER_A");



    private final LazyTokenStructureMatch DICTIONARY = new LazyTokenStructureMatch("DICTIONARY");
    private final LazyTokenStructureMatch LIST = new LazyTokenStructureMatch("LIST");


    public final LazyTokenStructureMatch ENTITY = new LazyTokenStructureMatch("ENTITY");
    public final LazyTokenStructureMatch LIMITED_ENTITY = new LazyTokenStructureMatch("ENTITY");
    public final LazyTokenStructureMatch INTERPOLATION_BLOCK;
    private final LazyTokenStructureMatch INTERPOLATION_VALUE;
    private final LazyTokenStructureMatch ROOT_INTERPOLATION_VALUE;
    private final LazyTokenStructureMatch LINE_SAFE_INTERPOLATION_VALUE;

    private final LazyTokenStructureMatch ROOT_INTERPOLATION_TYPE;
    private final LazyTokenStructureMatch INTERPOLATION_TYPE;

    private final LazyTokenGroupMatch FORMAL_PARAMETER;
    private final LazyTokenPatternMatch FORMAL_PARAMETERS;
    private final LazyTokenPatternMatch DYNAMIC_FUNCTION;

    private final LazyTokenPatternMatch TYPE_CONSTRAINTS;
    private final LazyTokenPatternMatch INFERRABLE_TYPE_CONSTRAINTS;

    private final LazyTokenStructureMatch PLUGIN_NAME = struct("PLUGIN_NAME");

    private final HashSet<String> duplicateCheck = new HashSet<>();

    private final LazyTokenStructureMatch POINTER;
    private static final LazyTokenPatternMatch resourceLocationFixer = ofType(NO_TOKEN).setName("_RLCF").setOptional().addFailProcessor((p, l) -> {
        if(l.getSuggestionModule() != null) {
            if(((LazyLexer) l).getCurrentIndex() <= l.getSuggestionModule().getSuggestionIndex()+1) {
                int targetIndex = ((LazyLexer) l).getLookingIndexTrimmed();
                String str = ((LazyLexer) l).getCurrentReadingString();
                int index = l.getSuggestionModule().getSuggestionIndex();

                if(index > 0) {
                    while (true) {
                        char c = str.charAt(index-1);
                        if (!(Character.isJavaIdentifierPart(c) || "#:/.-".contains(c+"")) || --index <= 1)
                            break;
                    }
                }

                if(index == targetIndex) {
                    l.getSuggestionModule().setSuggestionIndex(index);
                }
            }
        }
    });

    public TridentProductions(CommandModule module) {
        FILE = new LazyTokenStructureMatch("FILE");
        FILE_INNER = new LazyTokenStructureMatch("FILE_INNER");
        INNER_FUNCTION = new LazyTokenStructureMatch("INNER_FUNCTION");
        ANONYMOUS_INNER_FUNCTION = new LazyTokenStructureMatch("ANONYMOUS_INNER_FUNCTION");
        OPTIONAL_NAME_INNER_FUNCTION = new LazyTokenStructureMatch("OPTIONAL_NAME_INNER_FUNCTION");
        ENTRY = new LazyTokenStructureMatch("ENTRY");
        ENTRY.addTags(SuggestionTags.ENABLED);
        ENTRY.addTags(SuggestionTags.DISABLED_INDEX);
        ENTRY.addTags(TridentSuggestionTags.CONTEXT_ENTRY);
        COMMAND = new LazyTokenStructureMatch("COMMAND");
        COMMAND.addTags(SuggestionTags.ENABLED);
        COMMAND.addTags(TridentSuggestionTags.CONTEXT_COMMAND);
        INSTRUCTION = new LazyTokenStructureMatch("INSTRUCTION");
        MODIFIER = new LazyTokenStructureMatch("MODIFIER");
        MODIFIER.addTags(SuggestionTags.ENABLED);
        MODIFIER.addTags(TridentSuggestionTags.CONTEXT_MODIFIER);
        TEXT_COMPONENT = new LazyTokenStructureMatch("TEXT_COMPONENT");
        TEXT_COMPONENT.addTags("cspn:Text Component");
        SELECTOR = new LazyTokenStructureMatch("SELECTOR");
        SELECTOR_ARGUMENT = new LazyTokenStructureMatch("SELECTOR_ARGUMENT");
        SELECTOR_ARGUMENT.addTags(SuggestionTags.ENABLED);
        PLAYER_NAME = struct("PLAYER_NAME");

        LazyTokenPatternMatch SOUND_CHANNEL = choice("ambient", "block", "hostile", "master", "music", "neutral", "player", "record", "voice", "weather").setName("CHANNEL").addTags(SuggestionTags.ENABLED).addTags("cspn:Sound Channel");

        POINTER = struct("POINTER");

        RESOURCE_LOCATION_S = struct("RESOURCE_LOCATION");
        RESOURCE_LOCATION_TAGGED = struct("RESOURCE_LOCATION_TAGGED");

        COMMENT_S = ((LazyTokenItemMatch) new LazyTokenItemMatch(COMMENT).setName("COMMENT").addProcessor(
                (p, l) -> {
                    if (l.getSummaryModule() != null) {
                        Token token = ((TokenItem) p).getContents();
                        if(token.getSubSections() != null) {
                            for (Map.Entry<TokenSection, String> entry : token.getSubSections().entrySet()) {
                                if(entry.getValue().equals("comment.todo")) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTodo(token, token.value.substring(entry.getKey().start, entry.getKey().start + entry.getKey().length));
                                }
                            }
                        }
                    }
                }
        ));

        LazyTokenPatternMatch COMMAND_WRAPPER = group(list(MODIFIER).setOptional().setName("MODIFIERS"), literal("run").setOptional(), COMMAND).setName("COMMAND_WRAPPER");

        ENTRY.add(COMMENT_S);
        ENTRY.add(INSTRUCTION);
        ENTRY.add(COMMAND_WRAPPER);

        BiConsumer<TokenPattern<?>, Lexer> clearMemberListProcessor = (p, l) -> {
            if(l.getSummaryModule() != null) {
                ((TridentSummaryModule) l.getSummaryModule()).clearTempMemberAccessList();
            }
        };

        BiConsumer<TokenPattern<?>, Lexer> startComplexValue = (p, l) -> {
            if(l.getSummaryModule() != null) {
                TridentSummaryModule summaryModule = ((TridentSummaryModule) l.getSummaryModule());
                SummarySymbol topSym = null;
                if(!summaryModule.isSubSymbolStackEmpty()) {
                    topSym = summaryModule.peekSubSymbol();
                }
                summaryModule.pushSubSymbol(topSym);
            }
        };

        BiConsumer<TokenPattern<?>, Lexer> startClosure = (p, l) -> {
            if(l.getSummaryModule() != null) {
                TridentSummaryModule summaryModule = ((TridentSummaryModule) l.getSummaryModule());
                summaryModule.pushSubSymbol(null);
            }
        };

        BiConsumer<TokenPattern<?>, Lexer> endComplexValue = (p, l) -> {
            if(l.getSummaryModule() != null) {
                TridentSummaryModule summaryModule = ((TridentSummaryModule) l.getSummaryModule());
                summaryModule.popSubSymbol();
            }
        };

        BiConsumer<TokenPattern<?>, Lexer> claimTopSymbol = (p, l) -> {
            if(l.getSummaryModule() != null) {
                TridentSummaryModule summaryModule = ((TridentSummaryModule) l.getSummaryModule());
                SummarySymbol topSym = null;
                if(!summaryModule.isSubSymbolStackEmpty()) {
                    topSym = summaryModule.peekSubSymbol();
                }
                if(topSym != null) {
                    summaryModule.peek().surroundBlock(p.getStringBounds().start.index, p.getStringBounds().end.index, topSym);
                }
            }
        };

        BiConsumer<TokenPattern<?>, Lexer> surroundBlock = (p, lx) -> {
            if(lx.getSummaryModule() != null) {
                StringBounds bounds = p.getStringBounds();
                if(bounds != null) {
                    ((TridentSummaryModule) lx.getSummaryModule()).peek().surroundBlock(bounds.start.index, bounds.end.index);
                }
            }
        };

        {
            INTERPOLATION_BLOCK = choice(
                    group(symbol("$").setName("INTERPOLATION_HEADER").addTags(SuggestionTags.DISABLED).addProcessor(clearMemberListProcessor), glue().addTags(SuggestionTags.ENABLED, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE).addTags("cspn:Variable"), choice(identifierX(),literal("this")).setName("VARIABLE_NAME")).setName("VARIABLE")
            ).setName("INTERPOLATION_BLOCK");

            INTERPOLATION_VALUE = new LazyTokenStructureMatch("INTERPOLATION_VALUE");
            INTERPOLATION_VALUE.addTags(SuggestionTags.ENABLED, SuggestionTags.DISABLED_INDEX);
            INTERPOLATION_VALUE.addTags(TridentSuggestionTags.CONTEXT_INTERPOLATION_VALUE);
            INTERPOLATION_VALUE.addProcessor(clearMemberListProcessor);
            ROOT_INTERPOLATION_VALUE = new LazyTokenStructureMatch("ROOT_INTERPOLATION_VALUE");
            LINE_SAFE_INTERPOLATION_VALUE = new LazyTokenStructureMatch("LINE_SAFE_INTERPOLATION_VALUE");
            LINE_SAFE_INTERPOLATION_VALUE.addProcessor(clearMemberListProcessor);

            ROOT_INTERPOLATION_TYPE = new LazyTokenStructureMatch("ROOT_INTERPOLATION_VALUE");
            INTERPOLATION_TYPE = new LazyTokenStructureMatch("INTERPOLATION_TYPE");

            TYPE_CONSTRAINTS = optional(colon(), group(INTERPOLATION_TYPE, symbol("?").setOptional().setName("VARIABLE_NULLABLE")).setName("TYPE_CONSTRAINTS_INNER")).setName("TYPE_CONSTRAINTS");
            INFERRABLE_TYPE_CONSTRAINTS = optional(colon(), optional(INTERPOLATION_TYPE, symbol("?").setOptional().setName("VARIABLE_NULLABLE")).setName("TYPE_CONSTRAINTS_INNER")).setName("TYPE_CONSTRAINTS");

            ROOT_INTERPOLATION_VALUE.add(
                    identifierX()
                            .setName("VARIABLE_NAME")
                            .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                            .addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTempMemberAccess(p.flatten(false));
                                }
                            }));
            ROOT_INTERPOLATION_VALUE.add(
                    literal("this")
                            .setName("VARIABLE_NAME")
                            .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                            .addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTempMemberAccess(p.flatten(false));
                                }
                            }));
            ROOT_INTERPOLATION_VALUE.add(ofType(REAL_NUMBER).setName("RAW_REAL"));
            ROOT_INTERPOLATION_VALUE.add(ofType(INTEGER_NUMBER).setName("RAW_INTEGER"));
            ROOT_INTERPOLATION_VALUE.add(rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED));
            ROOT_INTERPOLATION_VALUE.add(ofType(STRING_LITERAL).setName("STRING_LITERAL"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("entity").setName("VALUE_WRAPPER_KEY"), brace("<"), LIMITED_ENTITY, brace(">")).setName("WRAPPED_ENTITY"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("block").setName("VALUE_WRAPPER_KEY"), brace("<"), BLOCK_TAGGED, brace(">")).setName("WRAPPED_BLOCK"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("item").setName("VALUE_WRAPPER_KEY"), brace("<"), ITEM_TAGGED, brace(">")).setName("WRAPPED_ITEM"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("text_component").setName("VALUE_WRAPPER_KEY"), brace("<"), TEXT_COMPONENT, brace(">")).setName("WRAPPED_TEXT_COMPONENT"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("nbt").setName("VALUE_WRAPPER_KEY"), brace("<"), NBT_COMPOUND, brace(">")).setName("WRAPPED_NBT"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("nbt_value").setName("VALUE_WRAPPER_KEY"), brace("<"), NBT_VALUE, brace(">")).setName("WRAPPED_NBT_VALUE"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("nbt_path").setName("VALUE_WRAPPER_KEY"), brace("<"), NBT_PATH, brace(">")).setName("WRAPPED_NBT_PATH"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("coordinates").setName("VALUE_WRAPPER_KEY"), brace("<"), COORDINATE_SET, brace(">")).setName("WRAPPED_COORDINATE"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("rotation").setName("VALUE_WRAPPER_KEY"), brace("<"), ROTATION, brace(">")).setName("WRAPPED_ROTATION"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("uuid").setName("VALUE_WRAPPER_KEY"), brace("<"), UUID, brace(">")).setName("WRAPPED_UUID"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("int_range").setName("VALUE_WRAPPER_KEY"), brace("<"), INTEGER_NUMBER_RANGE, brace(">")).setName("WRAPPED_INT_RANGE"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("real_range").setName("VALUE_WRAPPER_KEY"), brace("<"), REAL_NUMBER_RANGE, brace(">")).setName("WRAPPED_REAL_RANGE"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("resource").setName("VALUE_WRAPPER_KEY"), brace("<"), RESOURCE_LOCATION_TAGGED, brace(">")).setName("WRAPPED_RESOURCE"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("pointer").setName("VALUE_WRAPPER_KEY"), brace("<"), POINTER, brace(">")).setName("WRAPPED_POINTER"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("type_definition").setName("VALUE_WRAPPER_KEY"), brace("<"), INTERPOLATION_TYPE, brace(">")).setName("WRAPPED_TYPE"));
            ROOT_INTERPOLATION_VALUE.add(DICTIONARY);
            ROOT_INTERPOLATION_VALUE.add(LIST);
            ROOT_INTERPOLATION_VALUE.add(group(brace("("), INTERPOLATION_VALUE, brace(")").addProcessor(clearMemberListProcessor)).setName("PARENTHESIZED_VALUE"));
            ROOT_INTERPOLATION_VALUE.add(group(ofType(NULL)).setName("NULL_VALUE"));

            FORMAL_PARAMETER = group(identifierX().setName("FORMAL_PARAMETER_NAME"), TYPE_CONSTRAINTS).setName("FORMAL_PARAMETER");

            FORMAL_PARAMETERS = group(
                    brace("("),
                    list(FORMAL_PARAMETER, comma()).setOptional().setName("FORMAL_PARAMETER_LIST"),
                    brace(")")
            ).setName("FORMAL_PARAMETERS");

            DYNAMIC_FUNCTION = group(noToken().addFailProcessor((a, l) -> startClosure.accept(null, l)), FORMAL_PARAMETERS, TYPE_CONSTRAINTS, ANONYMOUS_INNER_FUNCTION).setName("DYNAMIC_FUNCTION").addProcessor(endComplexValue).addFailProcessor((n, l) -> endComplexValue.accept(null, l)).addProcessor((p, l) -> {
                if(l.getSummaryModule() != null) {
                    TokenList paramList = (TokenList) p.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
                    if(paramList != null) {
                        for(TokenPattern<?> paramPattern : paramList.searchByName("FORMAL_PARAMETER")) {
                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), paramPattern.find("FORMAL_PARAMETER_NAME").flatten(false), p.find("ANONYMOUS_INNER_FUNCTION").getStringLocation().index+1);
                            sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                            sym.setVisibility(Symbol.SymbolVisibility.LOCAL);
                            ((TridentSummaryModule) l.getSummaryModule()).peek().putLateElement(sym);
                        }
                    }
                }
            });

            ROOT_INTERPOLATION_VALUE.add(
                    group(
                            literal("function").setName("VALUE_WRAPPER_KEY"),
                            choice(
                                    ANONYMOUS_INNER_FUNCTION,
                                    DYNAMIC_FUNCTION
                            ).setName("NEW_FUNCTION_SPLIT").setGreedy(true)
                    ).setName("NEW_FUNCTION"));
            ROOT_INTERPOLATION_VALUE.add(group(literal("new").setName("VALUE_WRAPPER_KEY"), INTERPOLATION_TYPE, brace("("), list(INTERPOLATION_VALUE, comma()).setOptional().setName("PARAMETERS"), brace(")")).setName("CONSTRUCTOR_CALL"));

            ROOT_INTERPOLATION_TYPE.add(
                    identifierX()
                            .setName("VARIABLE_NAME")
                            .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                            .addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTempMemberAccess(p.flatten(false));
                                }
                            }));

            LazyTokenStructureMatch MEMBER_ACCESS = choice(
                    group(dot().addProcessor((p, l) -> {
                        if(l.getSuggestionModule() != null && l.getSuggestionModule().getSuggestionIndex() == p.getStringBounds().end.index) {
                            if(l.getSummaryModule() != null) {
                                ArrayList<String> memberPath = ((TridentSummaryModule) l.getSummaryModule()).getTempMemberAccessList();
                                l.getSuggestionModule().setLookingAtMemberPath(memberPath.toArray(new String[0]));
                                l.getSuggestionModule().addSuggestion(new ComplexSuggestion(TridentSuggestionTags.IDENTIFIER_MEMBER));
                            }
                        }
                    }), identifierY()
                            .setName("SYMBOL_NAME")
                            .addTags(SuggestionTags.ENABLED)
                            .addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTempMemberAccess(p.flatten(false));
                                }
                            })).setName("MEMBER_KEY"),
                    group(brace("[").addProcessor(clearMemberListProcessor), group(INTERPOLATION_VALUE).setName("INDEX"), brace("]")).setName("MEMBER_INDEX").addProcessor(clearMemberListProcessor),
                    group(brace("(").addProcessor(clearMemberListProcessor).addProcessor(startClosure), list(INTERPOLATION_VALUE, comma()).setOptional().setName("PARAMETERS"), brace(")")).setName("METHOD_CALL").addProcessor(clearMemberListProcessor).addProcessor(endComplexValue)
            ).setName("MEMBER_ACCESS");

            LazyTokenGroupMatch INTERPOLATION_CHAIN = group(ROOT_INTERPOLATION_VALUE, list(MEMBER_ACCESS).setOptional().setName("MEMBER_ACCESSES"), choice(group(sameLine(), keyword("is"), INTERPOLATION_TYPE).setName("INTERPOLATION_CHAIN_TAIL_IS"), group(sameLine(), keyword("as"), INTERPOLATION_TYPE).setName("INTERPOLATION_CHAIN_TAIL_AS")).setOptional().setName("INTERPOLATION_CHAIN_TAIL")).setName("INTERPOLATION_CHAIN");


            LazyTokenStructureMatch MEMBER_TYPE_ACCESS = choice(
                    group(dot().addProcessor((p, l) -> {
                        if(l.getSuggestionModule() != null && l.getSuggestionModule().getSuggestionIndex() == p.getStringBounds().end.index) {
                            if(l.getSummaryModule() != null) {
                                ArrayList<String> memberPath = ((TridentSummaryModule) l.getSummaryModule()).getTempMemberAccessList();
                                l.getSuggestionModule().setLookingAtMemberPath(memberPath.toArray(new String[0]));
                                l.getSuggestionModule().addSuggestion(new ComplexSuggestion(TridentSuggestionTags.IDENTIFIER_MEMBER));
                            }
                        }
                    }), identifierY()
                            .setName("SYMBOL_NAME")
                            .addTags(SuggestionTags.ENABLED)
                            .addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTempMemberAccess(p.flatten(false));
                                }
                            })).setName("MEMBER_KEY")
            ).setName("MEMBER_ACCESS");
            LazyTokenGroupMatch INTERPOLATION_TYPE_CHAIN = group(ROOT_INTERPOLATION_TYPE, list(MEMBER_TYPE_ACCESS).setOptional().setName("MEMBER_ACCESSES")).setName("INTERPOLATION_CHAIN");

            LazyTokenStructureMatch MID_INTERPOLATION_VALUE = struct("MID_INTERPOLATION_VALUE");
            MID_INTERPOLATION_VALUE.add(group(list(ofType(COMPILER_PREFIX_OPERATOR).addProcessor(clearMemberListProcessor)).setOptional().setName("PREFIX_OPERATORS"), INTERPOLATION_CHAIN, list(ofType(COMPILER_POSTFIX_OPERATOR).addProcessor(clearMemberListProcessor)).setOptional().setName("POSTFIX_OPERATORS")).setName("SURROUNDED_INTERPOLATION_VALUE"));
            MID_INTERPOLATION_VALUE.add(group(brace("("), INTERPOLATION_TYPE, brace(")"), MID_INTERPOLATION_VALUE).setName("CAST"));

            INTERPOLATION_VALUE.add(list(MID_INTERPOLATION_VALUE, ofType(COMPILER_OPERATOR).addProcessor(clearMemberListProcessor)).setName("EXPRESSION"));
            LINE_SAFE_INTERPOLATION_VALUE.add(list(MID_INTERPOLATION_VALUE, group(sameLine(), ofType(COMPILER_OPERATOR).addProcessor(clearMemberListProcessor))).setName("EXPRESSION"));

            INTERPOLATION_TYPE.add(INTERPOLATION_TYPE_CHAIN);
            INTERPOLATION_TYPE.add(ofType(PRIMITIVE_TYPE).setName("PRIMITIVE_ROOT_TYPE"));

            INTERPOLATION_BLOCK.add(group(symbol("$").setName("INTERPOLATION_HEADER").addTags(SuggestionTags.DISABLED).addProcessor(clearMemberListProcessor), glue(), brace("{").setName("INTERPOLATION_BRACE").addTags(SuggestionTags.DISABLED), INTERPOLATION_VALUE, brace("}").setName("INTERPOLATION_BRACE").addTags(SuggestionTags.DISABLED).addProcessor(clearMemberListProcessor)).setName("INTERPOLATION_WRAPPER"));

            DICTIONARY.add(group(
                    brace("{").addProcessor(startComplexValue),
                    list(
                            group(
                                    choice(identifierY(), ofType(STRING_LITERAL))
                                            .setName("DICTIONARY_KEY")
                                            .addProcessor((p, l) -> {
                                                if(l.getSummaryModule() != null) {
                                                    String key = p.flatten(false);
                                                    if(key.startsWith("\"")) {
                                                        try {
                                                            key = CommandUtils.parseQuotedString(key);
                                                        } catch(CommodoreException ignore) {
                                                        }
                                                    }
                                                    SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), key, p.getStringLocation().index);
                                                    sym.setMember(true);
                                                    ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                                                }
                                            }),
                                    colon(),
                                    INTERPOLATION_VALUE
                            ).setName("DICTIONARY_ENTRY").addProcessor((p, l) -> {
                                if(l.getSummaryModule() != null) {
                                    SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                                    if(!sym.hasSubBlock()) {
                                        ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                    }
                                }
                            }).addFailProcessor((n, l) -> {if(n > 0) endComplexValue.accept(null, l);}),
                            comma()
                    ).setOptional().setName("DICTIONARY_ENTRY_LIST"),
                    brace("}"))).addProcessor(claimTopSymbol).addProcessor(endComplexValue).addFailProcessor((n, l) -> {if(n > 0) endComplexValue.accept(null, l);});
            LIST.add(group(brace("[").addProcessor(startClosure), list(INTERPOLATION_VALUE, comma()).setOptional().setName("LIST_ENTRIES"), brace("]")).addProcessor(surroundBlock).addProcessor(endComplexValue).addFailProcessor((n, l) -> {if(n > 0) endComplexValue.accept(null, l);}));
        }

        PLAYER_NAME.add(identifierB());

        STRING_LITERAL_OR_IDENTIFIER_A.add(identifierA());
        STRING_LITERAL_OR_IDENTIFIER_A.add(string());

        RESOURCE_LOCATION_S.add(ofType(RESOURCE_LOCATION).setName("RAW_RESOURCE_LOCATION"));
        RESOURCE_LOCATION_S.add(INTERPOLATION_BLOCK);

        RESOURCE_LOCATION_TAGGED.add(group(resourceLocationFixer, optional(hash().setName("TAG_HEADER"), ofType(GLUE)).addTags(SuggestionTags.ENABLED, TridentSuggestionTags.FUNCTION_TAG).setName("TAG_HEADER_WRAPPER"), ofType(RESOURCE_LOCATION).setName("RAW_RESOURCE_LOCATION")).setName("RAW_RESOURCE_LOCATION_TAGGED"));
        RESOURCE_LOCATION_TAGGED.add(INTERPOLATION_BLOCK);

        {
            LazyTokenStructureMatch directiveBody = new LazyTokenStructureMatch("DIRECTIVE_BODY");

            DIRECTIVE = group(ofType(DIRECTIVE_HEADER), directiveBody).setName("DIRECTIVE");
            DIRECTIVE.addTags(SuggestionTags.ENABLED);

            directiveBody.add(group(literal("on").setName("DIRECTIVE_LABEL"), literal("compile")
                    .addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            ((TridentSummaryModule) l.getSummaryModule()).setCompileOnly();
                        }
                    })).setName("ON_DIRECTIVE"));
            directiveBody.add(group(literal("tag").setName("DIRECTIVE_LABEL"), resourceLocationFixer, ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE)
                    .addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            ((TridentSummaryModule) l.getSummaryModule()).addFunctionTag(new TridentUtil.ResourceLocation(p.flatten(false)));
                        }
                    })).setName("TAG_DIRECTIVE"));
            directiveBody.add(group(literal("require").setName("DIRECTIVE_LABEL"), resourceLocationFixer, ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.TRIDENT_FUNCTION)
                    .addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            ((TridentSummaryModule) l.getSummaryModule()).addRequires(new TridentUtil.ResourceLocation(p.flatten(false)));
                        }
                    })).setName("REQUIRE_DIRECTIVE"));
            directiveBody.add(group(literal("meta_tag").setName("DIRECTIVE_LABEL"), ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE)).setName("META_TAG_DIRECTIVE"));
            directiveBody.add(group(literal("priority").setName("DIRECTIVE_LABEL"), real()).setName("PRIORITY_DIRECTIVE"));
            directiveBody.add(group(literal("breaking").setName("DIRECTIVE_LABEL")).setName("BREAKING_DIRECTIVE"));
            directiveBody.add(group(literal("language_level").setName("DIRECTIVE_LABEL"), integer()).setName("LANGUAGE_LEVEL_DIRECTIVE"));
            directiveBody.add(group(literal("metadata").setName("DIRECTIVE_LABEL"), DICTIONARY).setName("METADATA_DIRECTIVE"));
            directiveBody.add(group(literal("using_plugin").setName("DIRECTIVE_LABEL"), group(PLUGIN_NAME).addProcessor((p, lx) -> {
                importPlugin(p.flatten(false), p, lx);
            })).setName("USING_PLUGIN_DIRECTIVE"));
        }

        {
            LazyTokenListMatch l = new LazyTokenListMatch(ENTRY, true).setName("ENTRIES");
            FILE_INNER.add(group(optional(list(DIRECTIVE).setOptional(true).setName("DIRECTIVES"))
                    .addProcessor((p, lx) -> {
                        if(lx.getSummaryModule() != null) {
                            ((TridentSummaryModule) lx.getSummaryModule()).lockDirectives();
                        }
                    }), ofType(EMPTY_TOKEN).setName("FILE_START_MARKER"), l).addProcessor(surroundBlock));

            FILE.add(group(optional(list(DIRECTIVE).setOptional(true).setName("DIRECTIVES")),l,ofType(TokenType.END_OF_FILE)).addProcessor((p, lx) -> {uninstallCommands();}).addFailProcessor((len, lx) -> {uninstallCommands();}));
        }

        TEXT_COLOR = choice("black", "dark_blue", "dark_aqua", "dark_green", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "reset").setName("TEXT_COLOR");
        TEXT_COLOR.addTags("cspn:Text Color");

        UUID.add(ofType(TridentTokens.UUID).setName("RAW_UUID"));
        UUID.add(INTERPOLATION_BLOCK);
        UUID.addTags("cspn:UUID");

        ENTITY.add(PLAYER_NAME);
        ENTITY.add(SELECTOR);
        ENTITY.add(group(INTERPOLATION_BLOCK, optional(glue(), brace("["), list(SELECTOR_ARGUMENT, comma()).setOptional().setName("SELECTOR_ARGUMENT_LIST"), brace("]")).setName("APPENDED_ARGUMENTS")).setName("ENTITY_VARIABLE"));
        ENTITY.addTags("cspn:Entity");

        LIMITED_ENTITY.add(struct("PLAYER_NAME").add(identifierBLimited()));
        LIMITED_ENTITY.add(SELECTOR);
        LIMITED_ENTITY.add(group(INTERPOLATION_BLOCK, optional(glue(), brace("["), list(SELECTOR_ARGUMENT, comma()).setOptional().setName("SELECTOR_ARGUMENT_LIST"), brace("]")).setName("APPENDED_ARGUMENTS")).setName("ENTITY_VARIABLE"));
        LIMITED_ENTITY.addTags("cspn:Entity");

        NEW_ENTITY_LITERAL = group(resourceLocationFixer, ENTITY_ID, optional(glue(), brace("["), list(INTERPOLATION_VALUE, comma()).setName("COMPONENT_LIST"), brace("]")).setName("IMPLEMENTED_COMPONENTS"), optional(glue(), NBT_COMPOUND).setName("NEW_ENTITY_NBT")).setName("NEW_ENTITY_LITERAL");
        NEW_ENTITY_LITERAL.addTags("cspn:New Entity");

        INNER_FUNCTION.add(group(group(RESOURCE_LOCATION_S).setName("INNER_FUNCTION_NAME"), brace("{"), FILE_INNER, brace("}")));
        ANONYMOUS_INNER_FUNCTION.add(group(brace("{"), FILE_INNER, brace("}")));
        OPTIONAL_NAME_INNER_FUNCTION.add(group(group(RESOURCE_LOCATION_S).setOptional().setName("INNER_FUNCTION_NAME"), brace("{"), FILE_INNER, brace("}")));

        //region Commands
        //region verbatim
        COMMAND.add(
                group(ofType(VERBATIM_COMMAND_HEADER), ofType(VERBATIM_COMMAND))
        );
        COMMAND.add(
                group(ofType(VERBATIM_COMMAND_HEADER), INTERPOLATION_BLOCK)
        );
        //endregion
        //region say
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "say"));
            g.append(ofType(WHITESPACE));
            g.append(list(choice(ofType(SAY_STRING), group(sameLine(), SELECTOR).setName("SAY_SELECTOR")).setName("SAY_PART")).setName("SAY_MESSAGE").addTags("cspn:Message"));
            COMMAND.add(g);
        }
        //endregion
        //region tellraw
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "tellraw"),
                    ENTITY,
                    TEXT_COMPONENT
            ));
        }
        //endregion
        //region defaultgamemode
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "defaultgamemode"));
            g.append(GAMEMODE);
            COMMAND.add(g);
        }
        //endregion
        //region gamemode
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "gamemode"));
            g.append(GAMEMODE);
            g.append(new LazyTokenGroupMatch(true).append(sameLine()).append(ENTITY).setName("PLAYER"));
            COMMAND.add(g);
        }
        //endregion
        //region tag
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "tag"));
            g.append(ENTITY);
            g.append(choice(
                    literal("list"),
                    group(literal("add"), noToken().addTags("cspn:Tag"), identifierA()),
                    group(literal("remove"), noToken().addTags("cspn:Tag"), identifierA()),
                    group(matchItem(TridentTokens.CUSTOM_COMMAND_KEYWORD, "update"), noToken().addTags("cspn:Tag"), identifierA())
            ));
            COMMAND.add(g);
        }
        //endregion
        //region component
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "component"));
            g.append(ENTITY);
            g.append(choice("add", "remove").setName("COMPONENT_ACTION"));
            g.append(noToken().addTags("cspn:Component"));
            g.append(LINE_SAFE_INTERPOLATION_VALUE);
            COMMAND.add(g);
        }
        //endregion
        //region event
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "event"));
            g.append(ENTITY);
            g.append(LINE_SAFE_INTERPOLATION_VALUE);
            COMMAND.add(g);
        }
        //endregion
        //region experience
        {
            LazyTokenPatternMatch unit = choice("points", "levels").setName("UNIT").setOptional();

            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(choice(matchItem(COMMAND_HEADER, "experience"), matchItem(COMMAND_HEADER, "xp")));
            g.append(choice(
                    group(literal("add"), ENTITY, integer().addTags("cspn:Amount"), unit).setName("ADD"),
                    group(literal("set"), ENTITY, integer().addTags("cspn:Amount"), unit).setName("SET"),
                    group(literal("query"), ENTITY, unit).setName("QUERY")
            ).setName("SUBCOMMAND"));
            COMMAND.add(g);
        }
        //endregion
        //region seed
        {
            COMMAND.add(matchItem(COMMAND_HEADER, "seed"));
        }
        //endregion
        //region attribute
        {
            if(((ThreeNumberVersion) module.getSettingsManager().getTargetVersion()).getMinor() >= 16) {
                COMMAND.add(group(
                        matchItem(COMMAND_HEADER, "attribute"),
                        ENTITY,
                        ATTRIBUTE_ID,
                        choice(
                                group(literal("get"), real().setOptional().setName("SCALE").addTags("cspn:Scale")).setName("ATTRIBUTE_GET"),
                                group(literal("base"),
                                        choice(
                                                group(literal("get"), real().setOptional().setName("SCALE").addTags("cspn:Scale")).setName("ATTRIBUTE_BASE_GET"),
                                                group(literal("set"), real().setName("VALUE").addTags("cspn:Value")).setName("ATTRIBUTE_BASE_SET")
                                        ).setName("SUBCOMMAND")
                                ).setName("ATTRIBUTE_BASE"),
                                group(literal("modifier"),
                                        choice(
                                                group(literal("add"), UUID, group(STRING_LITERAL_OR_IDENTIFIER_A).setName("ATTRIBUTE_MODIFIER_NAME").addTags("cspn:Attribute Modifier Name"), real().setName("VALUE").addTags("cspn:Attribute Modifier Value"), choice("add", "multiply", "multiply_base").setName("ATTRIBUTE_MODIFIER_OPERATION")).setName("ATTRIBUTE_MODIFIER_ADD"),
                                                group(literal("value"), literal("get"), UUID, real().setOptional().setName("SCALE").addTags("cspn:Scale")).setName("ATTRIBUTE_MODIFIER_GET"),
                                                group(literal("remove"), UUID).setName("ATTRIBUTE_BASE_SET")
                                        ).setName("SUBCOMMAND")
                                ).setName("ATTRIBUTE_MODIFIER")
                        ).setName("SUBCOMMAND")
                ));
            }
        }
        //endregion
        //region give
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "give"),
                    ENTITY,
                    ITEM,
                    integer().setOptional().setName("AMOUNT").addTags("cspn:Amount")
            ));
        }
        //endregion
        //region clear
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "clear"),
                    group(
                            ENTITY,
                            group(
                                    ofType(LINE_GLUE),
                                    ITEM_TAGGED,
                                    integer().setOptional().setName("AMOUNT").addTags("cspn:Amount")
                            ).setOptional()
                    ).setOptional()
            ));
        }
        //endregion
        //region difficulty
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "difficulty"),
                    group(DIFFICULTY).setOptional()
            ));
        }
        //endregion
        //region effect
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "effect"),
                    choice(
                            group(literal("clear"), optional(sameLine(), ENTITY, optional(EFFECT_ID))).setName("CLEAR"),
                            group(literal("give"), ENTITY, EFFECT_ID, optional(integer().setName("DURATION").addTags("cspn:Duration (seconds)"), optional(integer().setName("AMPLIFIER").addTags("cspn:Amplifier"), rawBoolean().setOptional().setName("HIDE_PARTICLES").addTags(SuggestionTags.ENABLED, "cspn:Hide Particles?")))).setName("GIVE")
                    )
            ));
        }
        //endregion
        //region enchant
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "enchant"),
                    ENTITY,
                    ENCHANTMENT_ID,
                    integer().setOptional().setName("LEVEL").addTags("cspn:Level")
            ));
        }
        //endregion
        //region function
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "function"),
                    choice(group(resourceLocationFixer, group(RESOURCE_LOCATION_TAGGED).setName("FUNCTION_REFERENCE_WRAPPER").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION)).setName("FUNCTION_REFERENCE"), OPTIONAL_NAME_INNER_FUNCTION).setGreedy(true).addTags(SuggestionTags.ENABLED)
            ));
        }
        //endregion
        //region help
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "help"),
                    ofType(TRAILING_STRING).setOptional()
            ));
        }
        //endregion
        //region kill
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "kill"),
                    optional(ENTITY)
            ));
        }
        //endregion
        //region spectate
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "spectate"),
                    optional(sameLine(), noToken().addTags("cspn:Target"), ENTITY, optional(sameLine(), noToken().addTags("cspn:Spectator"), ENTITY).setName("INNER")).setName("INNER")
            ));
        }
        //endregion
        //region kick
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "kick"),
                    optional(ENTITY)
            ));
        }
        //endregion
        //region list
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "list"),
                    literal("uuids").setOptional()
            ));
        }
        //endregion
        //region locate
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "locate"),
                    STRUCTURE
            ));
        }
        //endregion
        //region locatebiome
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "locatebiome"),
                    BIOME_ID
            ));
        }
        //endregion
        //region me
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "me"),
                    ofType(TRAILING_STRING).addTags("cspn:Message")
            ));
        }
        //endregion
        //region teammsg
        {
            COMMAND.add(group(
                    choice(matchItem(COMMAND_HEADER, "teammsg"), matchItem(COMMAND_HEADER, "tm")),
                    ofType(TRAILING_STRING).addTags("cspn:Message")
            ));
        }
        //endregion
        //region gamerule
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "gamerule"),
                    choice(
                            GAMERULE,
                            GAMERULE_SETTER
                    )
            ));
        }
        //endregion
        //region playsound
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "playsound"),
                    resourceLocationFixer,
                    group(RESOURCE_LOCATION_S).setName("SOUND_EVENT").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound"),
                    SOUND_CHANNEL,
                    ENTITY,
                    optional(
                            COORDINATE_SET,
                            real().setOptional().addTags("cspn:Max Volume"),
                            real().setOptional().addTags("cspn:Pitch"),
                            real().setOptional().addTags("cspn:Min Volume")
                    )
            ));
        }
        //endregion
        //region clone
        {
            LazyTokenPatternMatch mode = choice("force", "move", "normal").setOptional().setName("CLONE_MODE").addTags("cspn:Clone Mode");

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "clone"),
                    group(COORDINATE_SET).setName("FROM").addTags("cspn:Source From"),
                    group(COORDINATE_SET).setName("TO").addTags("cspn:To").addTags("cspn:Source To"),
                    group(COORDINATE_SET).setName("DESTINATION").addTags("cspn:Destination"),
                    choice(
                            group(literal("filtered"), ofType(LINE_GLUE), BLOCK_TAGGED, mode).setName("FILTERED"),
                            group(literal("masked"), mode).setName("MASKED"),
                            group(literal("replace"), mode).setName("REPLACE")
                    ).setOptional()
            ));
        }
        //endregion
        //region fill
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "fill"),
                    group(COORDINATE_SET).setName("FROM").addTags("cspn:From"),
                    group(COORDINATE_SET).setName("TO").addTags("cspn:To"),
                    BLOCK,
                    choice(
                            literal("destroy"),
                            literal("hollow"),
                            literal("keep"),
                            literal("outline"),
                            group(literal("replace"), optional(BLOCK_TAGGED)).setName("REPLACE")
                    ).setOptional()
            ));
        }
        //endregion
        //region particle
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "particle"),
                    PARTICLE,
                    optional(
                            COORDINATE_SET,
                            optional(
                                    group(
                                            real().setName("DX").addTags("cspn:dx"),
                                            real().setName("DY").addTags("cspn:dy"),
                                            real().setName("DZ").addTags("cspn:dz")
                                    ).setName("DELTA"),
                                    real().setName("SPEED").addTags("cspn:Speed"),
                                    integer().setName("COUNT").addTags("cspn:Count"),
                                    optional(
                                            choice("force", "normal"),
                                            optional(sameLine(), ENTITY).addTags("cspn:Viewers")
                                    )
                            )
                    )
            ));
        }
        //endregion
        //region recipe
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "recipe"),
                    choice("give", "take").setName("ACTION"),
                    ENTITY,
                    choice(
                            matchItem(SYMBOL, "*"),
                            RESOURCE_LOCATION_S
                    ).addTags("cspn:Recipe")
            ));
        }
        //endregion
        //region replaceitem
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "replaceitem"),
                    choice(
                            group(literal("block"), COORDINATE_SET),
                            group(literal("entity"), ENTITY)
                    ).setName("TARGET"),
                    SLOT_ID,
                    ITEM,
                    integer().setOptional().setName("COUNT").addTags("cspn:Count")
            ));
        }
        //endregion
        //region schedule
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "schedule"),
                    choice(
                            group(
                                    literal("clear"),
                                    resourceLocationFixer,
                                    group(RESOURCE_LOCATION_TAGGED).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION).addTags("cspn:Function")
                            ).setName("SCHEDULE_CLEAR"),
                            group(
                                    literal("function"),
                                    resourceLocationFixer,
                                    group(RESOURCE_LOCATION_TAGGED).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION).addTags("cspn:Function"),
                                    ofType(TIME).setName("TIME").addTags("cspn:Time"),
                                    choice("append", "replace").setOptional().setName("SCHEDULE_MODE")
                            ).setName("SCHEDULE_FUNCTION")
                    )
            ));
        }
        //endregion
        //region setblock
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "setblock"),
                    COORDINATE_SET,
                    BLOCK,
                    choice("destroy", "keep", "replace").setOptional().setName("OLD_BLOCK_HANDLING")
            ));
        }
        //endregion
        //region setworldspawn
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "setworldspawn"),
                    optional(COORDINATE_SET)
            ));
        }
        //endregion
        //region spawnpoint
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "spawnpoint"),
                    optional(
                            sameLine(),
                            ENTITY,
                            optional(COORDINATE_SET)
                    )
            ));
        }
        //endregion
        //region spreadplayers
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "spreadplayers"),
                    noToken().addTags("cspn:XZ Position"), TWO_COORDINATE_SET,
                    real().setName("SPREAD_DISTANCE").addTags("cspn:Spread Distance"),
                    real().setName("MAX_RANGE").addTags("cspn:Max Range"),
                    versionLimited(module, "command.spreadplayers.under", false, optional(literal("under"), integer().setName("MAX_HEIGHT").addTags("cspn:Max Height")).setName("UNDER_CLAUSE")),
                    rawBoolean().setName("RESPECT_TEAMS").addTags(SuggestionTags.ENABLED, "cspn:Respect Teams?"),
                    ENTITY
            ));
        }
        //endregion
        //region stopsound
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "stopsound"),
                    ENTITY,
                    choice(
                            group(SOUND_CHANNEL, resourceLocationFixer, optional(sameLine(), RESOURCE_LOCATION_S).setName("SOUND_RESOURCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound")).setName("STOP_BY_CHANNEL"),
                            group(matchItem(SYMBOL, "*"), sameLine(), resourceLocationFixer, group(RESOURCE_LOCATION_S).setName("SOUND_RESOURCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.SOUND_RESOURCE).addTags("cspn:Sound")).setName("STOP_BY_EVENT")
                    ).setOptional()
            ));
        }
        //endregion
        //region summon
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "summon"),
                    NEW_ENTITY_LITERAL,
                    optional(
                            COORDINATE_SET,
                            optional(NBT_COMPOUND).addTags("cspn:NBT")
                    )
            ));
        }
        //endregion
        //region teleport
        {
            COMMAND.add(group(
                    choice(matchItem(COMMAND_HEADER, "teleport"), matchItem(COMMAND_HEADER, "tp")),
                    choice(
                            group(
                                    ENTITY,
                                    choice(
                                            group(
                                                    COORDINATE_SET,
                                                    choice(
                                                            group(
                                                                    literal("facing"),
                                                                    choice(
                                                                            noToken(), COORDINATE_SET,
                                                                            group(noToken().addTags("cspn:Facing Entity"), literal("entity"), ENTITY, anchor().setOptional())
                                                                    )
                                                            ).setName("FACING_CLAUSE"),
                                                            ROTATION
                                                    ).setOptional().setName("ROTATION_OPTION").addTags("cspn:Rotation")
                                            ),
                                            ENTITY,
                                            INTERPOLATION_BLOCK
                                    ).setOptional().addTags("cspn:Destination")
                            ),
                            COORDINATE_SET
                    ).setName("SUBCOMMAND")
            ));
        }
        //endregion
        //region time
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "time"),
                    choice(
                            group(literal("query"), choice("day", "daytime", "gametime")).setName("QUERY"),
                            group(literal("set"), choice(ofType(TIME).setName("TIME").addTags("cspn:Time"), choice("day", "midnight", "night", "noon"))).setName("SET"),
                            group(literal("add"), ofType(TIME).setName("TIME").addTags("cspn:Time")).setName("ADD")
                    )
            ));
        }
        //endregion
        //region title
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "title"),
                    ENTITY,
                    choice(
                            group(choice("title", "subtitle", "actionbar").setName("DISPLAY"), TEXT_COMPONENT).setName("SHOW"),
                            choice("clear", "reset").setName("CLEAR_RESET"),
                            group(literal("times"), integer().setName("FADEIN").addTags("cspn:Fade In"), integer().setName("STAY").addTags("cspn:Stay"), integer().setName("FADEOUT").addTags("cspn:Fade Out")).setName("TIMES")
                    )
            ));
        }
        //endregion
        //region trigger
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "trigger"),
                    objectiveName(),
                    optional(choice("add", "set"), integer()).setName("INNER")
            ));
        }
        //endregion
        //region weather
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "weather"),
                    choice("clear", "rain", "thunder"),
                    integer().setOptional().addTags("cspn:Duration")
            ));
        }
        //endregion
        //region worldborder
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "worldborder"),
                    choice(
                            literal("get").setName("GET"),
                            group(choice("add", "set"), real().setName("DISTANCE").addTags("cspn:Distance"), integer().setOptional().setName("TIME").addTags("cspn:Transition time")).setName("CHANGE"),
                            group(literal("center"), noToken().addTags("cspn:XZ Position"), TWO_COORDINATE_SET).setName("CENTER"),
                            group(literal("damage"), choice("amount", "buffer"), real().setName("DAMAGE_OR_DISTANCE").addTags("cspn:Damage Per Block/Distance")).setName("DAMAGE"),
                            group(literal("warning"), choice("distance", "time"), integer().setName("DISTANCE_OR_TIME").addTags("cspn:Distance/Time")).setName("WARNING")
                    )
            ));
        }
        //endregion
        //region forceload
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "forceload"),
                    choice(
                            group(literal("add"), noToken().addTags("cspn:XZ Position 1"), TWO_COORDINATE_SET, optional(TWO_COORDINATE_SET).setName("CHUNK_TO").addTags("cspn:XZ Position 2")).setName("FORCELOAD_ADD"),
                            group(literal("query"), optional(TWO_COORDINATE_SET).setName("FORCELOAD_QUERY_COLUMN").addTags("cspn:XZ Position")).setName("FORCELOAD_QUERY"),
                            group(literal("remove"), choice(group(noToken().addTags("cspn:XZ Position 1"), TWO_COORDINATE_SET, optional(TWO_COORDINATE_SET).setName("CHUNK_TO").addTags("cspn:XZ Position 2")).setName("FORCELOAD_REMOVE_ONE"), group(literal("all")).setName("FORCELOAD_REMOVE_ALL"))).setName("FORCELOAD_REMOVE")
                    )
            ));
        }
        //endregion
        //region team
        {
            LazyTokenStructureMatch teamOptions = choice(
                    group(literal("collisionRule"), choice("always", "never", "pushOtherTeams", "pushOwnTeam")).setName("TEAM_COMPARISON_ARG"),
                    group(literal("color"), choice(TEXT_COLOR).setName("TEAM_COLOR")).setName("COLOR_ARG"),
                    group(literal("deathMessageVisibility"), choice("always", "hideForOtherTeams", "hideForOwnTeam", "never")).setName("TEAM_COMPARISON_ARG"),
                    group(literal("displayName"), TEXT_COMPONENT).setName("TEXT_COMPONENT_ARG"),
                    group(literal("friendlyFire"), rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Friendly Fire?")).setName("BOOLEAN_ARG"),
                    group(literal("nametagVisibility"), choice("always", "hideForOtherTeams", "hideForOwnTeam", "never")).setName("TEAM_COMPARISON_ARG"),
                    group(literal("prefix"), TEXT_COMPONENT).setName("TEXT_COMPONENT_ARG"),
                    group(literal("suffix"), TEXT_COMPONENT).setName("TEXT_COMPONENT_ARG"),
                    group(literal("seeFriendlyInvisibles"), rawBoolean().setName("BOOLEAN").addTags("cspn:See Friendly Invisibles?")).setName("BOOLEAN_ARG")
            ).setName("TEAM_OPTIONS");

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "team"),
                    choice(
                            group(literal("add"), group(identifierA()).setName("TEAM").addTags("cspn:Team"), optional(TEXT_COMPONENT).setName("DISPLAY_NAME").addTags("cspn:Display Name")).setName("ADD"),
                            group(literal("empty"), group(identifierA()).setName("TEAM").addTags("cspn:Team")).setName("EMPTY"),
                            group(literal("join"), group(identifierA()).setName("TEAM").addTags("cspn:Team"), optional(sameLine(), ENTITY).setName("SUBJECT")).setName("JOIN"),
                            group(literal("leave"), ENTITY).setName("LEAVE"),
                            group(literal("list"), optional(sameLine(), group(identifierA()).setName("TEAM").addTags("cspn:Team"))).setName("LIST"),
                            group(literal("modify"), group(identifierA()).setName("TEAM").addTags("cspn:Team"), teamOptions).setName("MODIFY"),
                            group(literal("remove"), group(identifierA()).setName("TEAM").addTags("cspn:Team")).setName("REMOVE")
                    )
            ));
        }
        //endregion
        //region scoreboard
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "scoreboard"),
                    choice(
                            group(literal("objectives"), choice(
                                    group(literal("add"), group(identifierA()).setName("OBJECTIVE_NAME").addTags("cspn:Objective"), group(identifierB()).setName("CRITERIA").addTags("cspn:Criteria"), optional(TEXT_COMPONENT).addTags("cspn:Display Name")).setName("ADD"),
                                    literal("list").setName("LIST"),
                                    group(literal("modify"), objectiveName(), choice(
                                            group(literal("displayname"), noToken().addTags("cspn:Display Name"), TEXT_COMPONENT).setName("DISPLAYNAME"),
                                            group(literal("rendertype"), choice("integer", "hearts")).setName("RENDERTYPE")
                                    )).setName("MODIFY"),
                                    group(literal("remove"), objectiveName()).setName("REMOVE"),
                                    group(literal("setdisplay"), group(identifierA()).setName("DISPLAY_SLOT").addTags("cspn:Objective Display"), optional(sameLine(), objectiveName()).setName("OBJECTIVE_CLAUSE")).setName("SETDISPLAY")
                            )).setName("OBJECTIVES"),
                            group(literal("players"), choice(
                                    group(choice("add", "remove", "set"), score(), integer().addTags("cspn:Value")).setName("CHANGE"),
                                    group(literal("enable"), score()).setName("ENABLE"),
                                    group(literal("get"), score()).setName("GET"),
                                    group(literal("list"), optional(sameLine(), ENTITY)).setName("LIST"),
                                    group(literal("operation"), group(score()).setName("TARGET_SCORE").addTags("cspn:Target"), ofType(SCOREBOARD_OPERATOR).setName("OPERATOR"), group(score()).setName("SOURCE_SCORE").addTags("cspn:Source")).setName("OPERATION"),
                                    group(literal("reset"), scoreOptionalObjective()).setName("RESET")
                            )).setName("PLAYERS")
                    )
            ));
        }
        //endregion
        //region advancement
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "advancement"),
                    choice("grant", "revoke").setName("ACTION"),
                    ENTITY,
                    choice(
                            literal("everything").setName("EVERYTHING"),
                            group(choice("from", "through", "until").setName("LIMIT"), noToken().addTags("cspn:Advancement"), RESOURCE_LOCATION_S).setName("FROM_THROUGH_UNTIL"),
                            group(literal("only"), noToken().addTags("cspn:Advancement"), RESOURCE_LOCATION_S, group(sameLine(), list(identifierC().addTags("cspn:Criterion"), sameLine()).setName("CRITERIA_LIST")).setOptional().setName("CRITERIA")).setName("ONLY")
                    ).setName("INNER")
            ));
        }
        //endregion
        //region bossbar
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "bossbar"),
                    choice(
                            literal("list").setName("LIST"),
                            group(literal("add"), noToken().addTags("cspn:Bossbar"), RESOURCE_LOCATION_S, TEXT_COMPONENT).setName("ADD"),
                            group(literal("get"), noToken().addTags("cspn:Bossbar"), RESOURCE_LOCATION_S, choice("max", "players", "value", "visible")).setName("GET"),
                            group(literal("remove"), noToken().addTags("cspn:Bossbar"), RESOURCE_LOCATION_S).setName("REMOVE"),
                            group(literal("set"), noToken().addTags("cspn:Bossbar"), RESOURCE_LOCATION_S, choice(
                                    group(literal("color"), choice("blue", "green", "pink", "purple", "red", "white", "yellow")).setName("SET_COLOR").addTags("cspn:Bossbar Color"),
                                    group(literal("max"), integer().addTags("cspn:Max Value")).setName("SET_MAX"),
                                    group(literal("name"), noToken().addTags("cspn:Display Name"), TEXT_COMPONENT).setName("SET_NAME"),
                                    group(literal("players"), optional(sameLine(), ENTITY).setName("OPTIONAL_ENTITY")).setName("SET_PLAYERS"),
                                    group(literal("style"), choice("progress", "notched_6", "notched_10", "notched_12", "notched_20")).setName("SET_STYLE"),
                                    group(literal("value"), integer().addTags("cspn:Value")).setName("SET_VALUE"),
                                    group(literal("visible"), rawBoolean().addTags(SuggestionTags.ENABLED, "cspn:Visible?")).setName("SET_VISIBLE")
                            )).setName("SET")
                    )
            ));
        }
        //endregion
        //region data
        {

            LazyTokenStructureMatch target = choice(
                    group(literal("block"), COORDINATE_SET).setName("BLOCK_TARGET"),
                    group(literal("entity"), ENTITY).setName("ENTITY_TARGET"),
                    group(literal("storage"), noToken().addTags("cspn:Storage Location"), RESOURCE_LOCATION_S).setName("STORAGE_TARGET")
            ).setName("DATA_TARGET");
            target.addTags("cspn:Data Target");

            LazyTokenStructureMatch source = choice(
                    group(literal("from"), target, optional(sameLine(), NBT_PATH).setName("PATH_CLAUSE")).setName("TARGET_SOURCE"),
                    group(literal("value"), NBT_VALUE).setName("LITERAL_SOURCE")
            ).setName("DATA_SOURCE");
            target.addTags("cspn:Data Source");

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "data"),
                    choice(
                            group(literal("get"), target, optional(sameLine(), NBT_PATH, real().setOptional().setName("SCALE").addTags("cspn:Scale")).setName("PATH_CLAUSE")).setName("GET"),
                            group(literal("merge"), target, NBT_COMPOUND).setName("MERGE"),
                            group(literal("modify"), target, NBT_PATH, choice(
                                    group(literal("append"), source).setName("MODIFY_APPEND"),
                                    group(literal("insert"), integer().addTags("cspn:Insert Index"), source).setName("MODIFY_INSERT"),
                                    group(literal("merge"), source).setName("MODIFY_MERGE"),
                                    group(literal("prepend"), source).setName("MODIFY_PREPEND"),
                                    group(literal("set"), source).setName("MODIFY_SET")
                            )).setName("MODIFY"),
                            group(literal("remove"), target, NBT_PATH).setName("REMOVE")
                    )
            ));
        }
        //endregion
        //region datapack
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "datapack"),
                    choice(
                            group(literal("list"), choice("available", "enabled").setOptional().setName("DATAPACK_FILTER")).setName("DATAPACK_LIST"),
                            group(literal("enable"), noToken().addTags("cspn:Data Pack"), STRING_LITERAL_OR_IDENTIFIER_A, choice(
                                    group(literal("first")).setName("DATAPACK_ENABLE_FIRST"),
                                    group(literal("last")).setName("DATAPACK_ENABLE_LAST"),
                                    group(literal("before"), noToken().addTags("cspn:Before Data Pack"), STRING_LITERAL_OR_IDENTIFIER_A).setName("DATAPACK_ENABLE_BEFORE"),
                                    group(literal("after"), noToken().addTags("cspn:After Data Pack"), STRING_LITERAL_OR_IDENTIFIER_A).setName("DATAPACK_ENABLE_AFTER")
                            ).setOptional()).setName("DATAPACK_ENABLE"),
                            group(literal("disable"), noToken().addTags("cspn:Data Pack"), STRING_LITERAL_OR_IDENTIFIER_A, choice("available", "enabled").setOptional().setName("DATAPACK_FILTER")).setName("DATAPACK_DISABLE")
                    )
            ));
        }
        //endregion
        //region reload
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "reload")
            ));
        }
        //endregion
        //region stop
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "stop")
            ));
        }
        //endregion
        //region save-all
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "save-all"),
                    literal("flush").setOptional().setName("SAVE_ALL_FLUSH")
            ));
        }
        //endregion
        //region save-on
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "save-on")
            ));
        }
        //endregion
        //region save-off
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "save-off")
            ));
        }
        //endregion
        //region op
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "op"),
                    ENTITY
            ));
        }
        //endregion
        //region deop
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "deop"),
                    ENTITY
            ));
        }
        //endregion
        //region ban
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "ban"),
                    ENTITY,
                    group(sameLine(), ofType(TRAILING_STRING)).setOptional().setName("REASON").addTags("cspn:Reason")
            ));
        }
        //endregion
        //region ban-ip
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "ban-ip"),
                    identifierA().addTags("cspn:IP"),
                    group(sameLine(), ofType(TRAILING_STRING)).setOptional().setName("REASON").addTags("cspn:Reason")
            ));
        }
        //endregion
        //region pardon
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "pardon"),
                    ENTITY
            ));
        }
        //endregion
        //region pardon-ip
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "pardon-ip"),
                    identifierA().addTags("cspn:IP")
            ));
        }
        //endregion
        //region banlist
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "banlist"),
                    choice("players", "ips").setOptional().setName("BANLIST_QUERY_TYPE")
            ));
        }
        //endregion
        //region debug
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "debug"),
                    choice("report", "start", "stop")
            ));
        }
        //endregion
        //region whitelist
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "whitelist"),
                    choice(
                            group(literal("on")).setName("WHITELIST_ON"),
                            group(literal("off")).setName("WHITELIST_OFF"),
                            group(literal("list")).setName("WHITELIST_LIST"),
                            group(literal("reload")).setName("WHITELIST_LIST"),
                            group(literal("add"), ENTITY).setName("WHITELIST_ADD"),
                            group(literal("remove"), ENTITY).setName("WHITELIST_REMOVE")
                    )
            ));
        }
        //endregion
        //region loot
        {

            LazyTokenPatternMatch tool = choice(literal("mainhand"), literal("offhand"), ITEM).setOptional().setName("TOOL").addTags("cspn:Tool");

            LazyTokenStructureMatch destination = choice(
                    group(literal("give"), ENTITY).setName("GIVE"),
                    group(literal("insert"), COORDINATE_SET).setName("INSERT"),
                    group(literal("replace"),
                            choice(
                                    group(literal("block"), COORDINATE_SET),
                                    group(literal("entity"), ENTITY)
                            ),
                            SLOT_ID,
                            integer().setOptional().setName("COUNT")
                    ).setName("REPLACE"),
                    group(literal("spawn"), COORDINATE_SET).setName("SPAWN")
            ).setName("LOOT_DESTINATION");
            destination.addTags("cspn:Loot Destination");

            LazyTokenStructureMatch source = choice(
                    group(literal("fish"), noToken().addTags("cspn:Loot Table"), RESOURCE_LOCATION_S, COORDINATE_SET, tool).setName("FISH"),
                    group(literal("kill"), ENTITY).setName("KILL"),
                    group(literal("loot"), noToken().addTags("cspn:Loot Table"), RESOURCE_LOCATION_S).setName("LOOT"),
                    group(literal("mine"), COORDINATE_SET, tool).setName("MINE")
            ).setName("LOOT_SOURCE");
            source.addTags("cspn:Loot Source");

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "loot"),
                    destination, source
            ));
        }
        //endregion
        //region expand
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "expand"),
                    ANONYMOUS_INNER_FUNCTION
            ));
        }
        //endregion
        //region execute
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "execute"),
                    list(MODIFIER).setOptional(true).setName("MODIFIER_LIST"),
                    choice(
                            literal("noop"),
                            group(
                                    literal("run"),
                                    COMMAND
                            ).setName("CHAINED_COMMAND").addTags("cspn:Chained Command")
                    ).setName("EXECUTE_END").setOptional()
            ));
        }
        //endregion
        //endregion

        //region Execute Modifiers
        //region align
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "align"),
                    ofType(SWIZZLE).addTags("cspn:Axes")
            ));
        }
        //endregion
        //region anchored
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "anchored"),
                    anchor()
            ));
        }
        //endregion
        //region as
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "as"),
                    ENTITY
            ));
        }
        //endregion
        //region at
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "at"),
                    ENTITY
            ));
        }
        //endregion
        //region facing
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "facing"),
                    choice(
                            group(literal("entity").setOptional(), ENTITY, anchor().setOptional()).setName("ENTITY_BRANCH"),
                            group(COORDINATE_SET).setName("BLOCK_BRANCH")
                    )
            ));
        }
        //endregion
        //region in
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "in"),
                    DIMENSION_ID
            ));
        }
        //endregion
        //region if/unless
        {
            MODIFIER.add(group(
                    choice(matchItem(MODIFIER_HEADER, "if"), matchItem(MODIFIER_HEADER, "unless")).setName("HEADER"),
                    choice(
                            group(SELECTOR).setName("SELECTOR_CONDITION"),
                            group(literal("entity"), ENTITY).setName("ENTITY_CONDITION"),
                            group(literal("predicate"), noToken().addTags("cspn:Predicate"), RESOURCE_LOCATION_S).setName("PREDICATE_CONDITION"),
                            group(literal("block"), COORDINATE_SET, BLOCK_TAGGED).setName("BLOCK_CONDITION"),
                            group(literal("score"), score(), choice(
                                    matchItem(TridentTokens.CUSTOM_COMMAND_KEYWORD, "isset").setName("ISSET"),
                                    group(choice(symbol("<"), symbol("<="), symbol("="), symbol(">="), symbol(">")).setName("OPERATOR"), score()).setName("COMPARISON"),
                                    group(literal("matches"), INTEGER_NUMBER_RANGE).setName("MATCHES"))
                            ).setName("SCORE_CONDITION"),
                            group(literal("blocks"),
                                    group(COORDINATE_SET).setName("FROM").addTags("cspn:From"),
                                    group(COORDINATE_SET).setName("TO").addTags("cspn:To"),
                                    group(COORDINATE_SET).setName("TEMPLATE").addTags("cspn:Template"),
                                    choice("all", "masked").setName("AIR_POLICY")
                            ).setName("REGION_CONDITION"),
                            group(literal("data"),
                                    choice(
                                            group(literal("block"), COORDINATE_SET).setName("BLOCK_SUBJECT"),
                                            group(literal("storage"), noToken().addTags("cspn:Storage Location"), RESOURCE_LOCATION_S).setName("STORAGE_SUBJECT"),
                                            group(literal("entity"), ENTITY).setName("ENTITY_SUBJECT")
                                    ),
                                    NBT_PATH
                            ).setName("DATA_CONDITION")
                    ).setName("SUBJECT")
            ));
        }
        //endregion
        //region positioned
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "positioned"),
                    choice(
                            group(literal("as").setOptional(), ENTITY).setName("ENTITY_BRANCH"),
                            group(COORDINATE_SET).setName("BLOCK_BRANCH")
                    )
            ));
        }
        //endregion
        //region rotated
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "rotated"),
                    choice(
                            group(literal("as").setOptional(), ENTITY).setName("ENTITY_BRANCH"),
                            ROTATION
                    ).addTags("cspn:Rotation")
            ));
        }
        //endregion
        //region store
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "store"),
                    choice("result", "success").setName("STORE_VALUE"),
                    choice(
                            group(literal("storage"), noToken().addTags("cspn:Storage Location"), RESOURCE_LOCATION_S, NBT_PATH, numericDataType().setOptional().setName("NUMERIC_TYPE"), real().setName("SCALE").addTags("cspn:Scale")).setName("STORE_STORAGE"),
                            group(literal("block"), COORDINATE_SET, NBT_PATH, numericDataType().setOptional().setName("NUMERIC_TYPE"), real().setName("SCALE").addTags("cspn:Scale")).setName("STORE_BLOCK"),
                            group(literal("bossbar"), noToken().addTags("cspn:Bossbar"), noToken().addTags("cspn:Bossbar"), RESOURCE_LOCATION_S, choice("max", "value").setName("BOSSBAR_VARIABLE")).setName("STORE_BOSSBAR"),
                            group(literal("entity"), ENTITY, NBT_PATH, numericDataType().setOptional().setName("NUMERIC_TYPE"), real().setName("SCALE").addTags("cspn:Scale")).setName("STORE_ENTITY"),
                            group(literal("score"), score()).setName("STORE_SCORE")
                    )
            ));
        }
        //endregion
        //region raw
        {
            MODIFIER.add(group(
                    matchItem(MODIFIER_HEADER, "raw"),
                    choice(string(), INTERPOLATION_BLOCK).setName("RAW_MODIFIER_VALUE")
            ));
        }
        //endregion
        //endregion



        //region msg
        {
            COMMAND.add(group(
                    choice(matchItem(COMMAND_HEADER, "msg"), matchItem(COMMAND_HEADER, "tell"), matchItem(COMMAND_HEADER, "w")),
                    ENTITY,
                    ofType(TRAILING_STRING).addTags("cspn:Message")
            ));
        }
        //endregion

        //region Constructs
        //region Blockstate
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(brace("["));
            {
                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch().setName("BLOCKSTATE_PROPERTY");
                g2.append(group(identifierA()).setName("BLOCKSTATE_PROPERTY_KEY").addTags("cspn:Blockstate Key"));
                g2.append(equals());
                {
                    g2.append(group(identifierA()).setName("BLOCKSTATE_PROPERTY_VALUE").addTags("cspn:Blockstate Value"));
                }
                g.append(new LazyTokenListMatch(g2, comma(), true).setName("BLOCKSTATE_LIST"));
            }
            g.append(brace("]"));

            BLOCKSTATE.add(g);
        }
        //endregion
        //region Block
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("CONCRETE_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(resourceLocationFixer).append(BLOCK_ID).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(BLOCKSTATE).setName("BLOCKSTATE_CLAUSE"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND).setName("NBT_CLAUSE"));
            BLOCK.add(g);
            BLOCK.add(group(INTERPOLATION_BLOCK, optional(glue(), BLOCKSTATE).setName("APPENDED_BLOCKSTATE"), optional(glue(), NBT_COMPOUND).setName("APPENDED_NBT")).setName("BLOCK_VARIABLE"));
            BLOCK_TAGGED.add(BLOCK);

            BLOCK.addTags("cspn:Block");
            BLOCK_TAGGED.addTags("cspn:Block Tag");
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ABSTRACT_RESOURCE");
            g.append(resourceLocationFixer);
            g.append(new LazyTokenGroupMatch().append(hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.BLOCK_TAG)).append(ofType(GLUE)).append(RESOURCE_LOCATION_S).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(BLOCKSTATE).setName("BLOCKSTATE_CLAUSE"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND).setName("NBT_CLAUSE"));
            BLOCK_TAGGED.add(g);
        }
        //endregion
        //region Item
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("CONCRETE_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(resourceLocationFixer).append(ITEM_ID).setName("RESOURCE_NAME"));
            g.append(optional(glue(), hash(), integer().addTags("cspn:Model Index")).setName("APPENDED_MODEL_DATA"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            ITEM.add(g);
            ITEM.add(group(INTERPOLATION_BLOCK, optional(glue(), hash(), integer()).setName("APPENDED_MODEL_DATA"), optional(glue(), NBT_COMPOUND).setName("APPENDED_NBT")).setName("ITEM_VARIABLE"));
            ITEM_TAGGED.add(ITEM);

            ITEM.addTags("cspn:Item");
            ITEM_TAGGED.addTags("cspn:Item Tag");
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ABSTRACT_RESOURCE");
            g.append(resourceLocationFixer);
            g.append(new LazyTokenGroupMatch().append(hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.ITEM_TAG)).append(ofType(GLUE)).append(RESOURCE_LOCATION_S).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            ITEM_TAGGED.add(g);
        }
        //endregion

        //region Text Components
        {
            LazyTokenStructureMatch JSON_ROOT = new LazyTokenStructureMatch("JSON_ROOT");
            LazyTokenStructureMatch JSON_ELEMENT = new LazyTokenStructureMatch("JSON_ELEMENT");

            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("JSON_OBJECT");
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(group(string()).setName("JSON_OBJECT_KEY"));
                    g2.append(colon());
                    g2.append(JSON_ELEMENT);
                    g.append(new LazyTokenListMatch(g2, comma(), true).setName("JSON_OBJECT_ENTRIES"));
                }
                g.append(brace("}"));
                JSON_ELEMENT.add(g);
                JSON_ROOT.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("JSON_ARRAY");
                g.append(brace("["));
                g.append(new LazyTokenListMatch(JSON_ELEMENT, comma(), true).setName("JSON_ARRAY_ENTRIES"));
                g.append(brace("]"));
                JSON_ELEMENT.add(g);
                JSON_ROOT.add(g);
            }
            JSON_ELEMENT.add(string());
            JSON_ROOT.add(string());
            JSON_ELEMENT.add(real().setName("NUMBER"));
            JSON_ELEMENT.add(INTERPOLATION_BLOCK);
            JSON_ELEMENT.add(rawBoolean().setName("BOOLEAN"));
            JSON_ELEMENT.add(ofType(JSON_NUMBER).setName("JSON_NUMBER"));

            TEXT_COMPONENT.add(JSON_ELEMENT);
            TEXT_COMPONENT.add(INTERPOLATION_BLOCK);
        }
        //endregion
        //region NBT
        {
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("NBT_COMPOUND_GROUP");
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(group(STRING_LITERAL_OR_IDENTIFIER_A).setName("NBT_KEY"));
                    g2.append(colon());
                    g2.append(NBT_VALUE);
                    g.append(new LazyTokenListMatch(g2, comma(), true).setName("NBT_COMPOUND_ENTRIES"));
                }
                g.append(brace("}"));
                NBT_COMPOUND.add(g);
                NBT_COMPOUND.add(INTERPOLATION_BLOCK);
                NBT_VALUE.add(NBT_COMPOUND);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(optional().append(new LazyTokenListMatch(NBT_VALUE, comma(), true).setName("NBT_LIST_ENTRIES")));
                g.append(brace("]"));
                NBT_LIST.add(g);
                NBT_VALUE.add(NBT_LIST);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(literal("B"));
                g.append(symbol(";"));
                g.append(optional().append(new LazyTokenListMatch(NBT_VALUE, comma(), true).setName("NBT_ARRAY_ENTRIES")));
                g.append(brace("]"));
                LazyTokenStructureMatch NBT_BYTE_ARRAY = struct("NBT_BYTE_ARRAY").add(g);
                NBT_VALUE.add(NBT_BYTE_ARRAY);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(literal("I"));
                g.append(symbol(";"));
                g.append(optional().append(new LazyTokenListMatch(NBT_VALUE, comma(), true).setName("NBT_ARRAY_ENTRIES")));
                g.append(brace("]"));
                LazyTokenStructureMatch NBT_INT_ARRAY = struct("NBT_INT_ARRAY").add(g);
                NBT_VALUE.add(NBT_INT_ARRAY);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(literal("L"));
                g.append(symbol(";"));
                g.append(optional().append(new LazyTokenListMatch(NBT_VALUE, comma(), true).setName("NBT_ARRAY_ENTRIES")));
                g.append(brace("]"));
                LazyTokenStructureMatch NBT_LONG_ARRAY = struct("NBT_LONG_ARRAY").add(g);
                NBT_VALUE.add(NBT_LONG_ARRAY);
            }
            NBT_VALUE.add(string());
            NBT_VALUE.add(ofType(IDENTIFIER_TYPE_A).setName("RAW_STRING"));
            NBT_VALUE.add(ofType(TYPED_NUMBER).setName("NBT_NUMBER"));
            NBT_VALUE.add(rawBoolean().setName("BOOLEAN"));
            NBT_VALUE.add(INTERPOLATION_BLOCK);
            NBT_VALUE.addTags("cspn:NBT Value");
        }


        {
            LazyTokenStructureMatch NBT_PATH_NODE = new LazyTokenStructureMatch("NBT_PATH_NODE");
            NBT_PATH_NODE.setGreedy(true);

            LazyTokenStructureMatch STRING_LITERAL_OR_IDENTIFIER_D = choice(string(), ofType(IDENTIFIER_TYPE_D).setName("IDENTIFIER_D")).setName("STRING_LITERAL_OR_IDENTIFIER_D");

            NBT_PATH_NODE.add(
                    dot().addTags(StandardTags.LIST_TERMINATOR).setName("NBT_PATH_TRAILING_DOT")
            );

            NBT_PATH_NODE.add(
                    group(
                            dot().setName("NBT_PATH_SEPARATOR"),
                            glue(),
                            group(STRING_LITERAL_OR_IDENTIFIER_D).setName("NBT_PATH_KEY_LABEL"),
                            optional(NBT_COMPOUND).setName("NBT_PATH_COMPOUND_MATCH")
                    ).setName("NBT_PATH_KEY"));

            NBT_PATH_NODE.add(
                    group(
                            dot().setOptional(),
                            glue(),
                            brace("["),
                            choice(integer(), NBT_COMPOUND, INTERPOLATION_BLOCK).setOptional().setName("NBT_PATH_LIST_CONTENT"),
                            brace("]")
                    ).setName("NBT_PATH_LIST_ACCESS")
            );

            NBT_PATH.add(
                    group(
                            choice(
                                    group(
                                            group(STRING_LITERAL_OR_IDENTIFIER_D).setName("NBT_PATH_KEY_LABEL"),
                                            optional(NBT_COMPOUND).setName("NBT_PATH_COMPOUND_MATCH")
                                    ).setName("NBT_PATH_KEY"),
                                    group(NBT_COMPOUND).setName("NBT_PATH_COMPOUND_ROOT")
                            ).setName("NBT_PATH_ROOT"),
                            list(NBT_PATH_NODE).setOptional().setName("NBT_PATH_NODE_SEQUENCE")
                    ).setName("NBT_PATH_ROOT_WRAPPER")
            );

            NBT_PATH.add(INTERPOLATION_BLOCK);
            NBT_PATH.addTags("cspn:NBT Path");
        }
        //endregion
        //region Selector
        {
            SELECTOR.add(
                    group(
                            ofType(SELECTOR_HEADER).setName("SELECTOR_HEADER"),
                            optional(
                                    glue(),
                                    brace("["),
                                    list(SELECTOR_ARGUMENT, comma()).setOptional().setName("SELECTOR_ARGUMENT_LIST"),
                                    brace("]")
                            )
                    )
            );
        }
        //endregion
        //region Number Ranges
        {
            INTEGER_NUMBER_RANGE.add(INTERPOLATION_BLOCK);
            INTEGER_NUMBER_RANGE.add(integer().setName("EXACT"));
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(integer().setName("MIN"));
                g.append(glue());
                g.append(dot());
                g.append(glue());
                g.append(dot());
                g.append(optional(glue(), integer().setName("MAX")));
                INTEGER_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(glue());
                g.append(dot());
                g.append(glue());
                g.append(integer().setName("MAX"));
                INTEGER_NUMBER_RANGE.add(g);
            }

            INTEGER_NUMBER_RANGE.addTags("cspn:Integer Range");

            REAL_NUMBER_RANGE.add(INTERPOLATION_BLOCK);
            REAL_NUMBER_RANGE.add(real().setName("EXACT"));
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(real().setName("MIN"));
                g.append(glue());
                g.append(dot());
                g.append(glue());
                g.append(dot());
                g.append(optional(glue(), real().setName("MAX")));
                REAL_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(glue());
                g.append(dot());
                g.append(glue());
                g.append(real().setName("MAX"));
                REAL_NUMBER_RANGE.add(g);
            }

            REAL_NUMBER_RANGE.addTags("cspn:Real Range");
        }
        //endregion
        //region Selector Arguments
        {
            //Integer Range Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("level").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(INTEGER_NUMBER_RANGE).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Real Number Range Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("distance", "x_rotation", "y_rotation").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(REAL_NUMBER_RANGE).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Integer Number Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("limit").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(integer()).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Real Number Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("x", "y", "z", "dx", "dy", "dz").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(real()).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Identifier Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("tag", "team").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(
                            group(not().setOptional(), identifierA().setOptional())
                    ).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Identifier Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("component").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(
                            group(not().setOptional(), INTERPOLATION_VALUE)
                    ).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //String Arguments
            SELECTOR_ARGUMENT.add(group(
                    choice("name").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(
                            group(not().setOptional(), group(STRING_LITERAL_OR_IDENTIFIER_A).setOptional())
                    ).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Gamemode argument
            SELECTOR_ARGUMENT.add(group(
                    choice("gamemode").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(group(not().setOptional(), GAMEMODE)).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Type argument
            SELECTOR_ARGUMENT.add(group(
                    choice("type").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(group(not().setOptional(), ENTITY_ID_TAGGED)).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Predicate argument
            SELECTOR_ARGUMENT.add(group(
                    choice("predicate").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(group(not().setOptional(), noToken().addTags("cspn:Predicate"), RESOURCE_LOCATION_S)).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Sort argument
            SELECTOR_ARGUMENT.add(group(
                    choice("sort").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice("nearest", "furthest", "arbitrary", "random").setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Advancements argument

            LazyTokenPatternMatch advancementArgumentBlock = group(
                    brace("{"),
                    list(group(
                            group(RESOURCE_LOCATION_S).setName("ADVANCEMENT_ENTRY_KEY"),
                            equals(),
                            choice(
                                    rawBoolean().setName("BOOLEAN"),
                                    group(
                                            brace("{"),
                                            list(group(group(identifierA()).setName("CRITERION_NAME"), equals(), rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED)).setName("CRITERION_ENTRY"), comma()).setOptional().setName("CRITERION_LIST"),
                                            brace("}")
                                    ).setName("CRITERION_GROUP")
                            ).setName("ADVANCEMENT_ENTRY_VALUE")
                    ).setName("ADVANCEMENT_ENTRY"), comma()).setOptional().setName("ADVANCEMENT_LIST"),
                    brace("}")
            ).setName("ADVANCEMENT_ARGUMENT_BLOCK");

            SELECTOR_ARGUMENT.add(group(
                    choice("advancements").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(advancementArgumentBlock).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //Scores argument

            LazyTokenPatternMatch scoreArgumentBlock = group(
                    brace("{"),
                    list(group(
                            objectiveName(),
                            equals(),
                            choice(matchItem(CUSTOM_COMMAND_KEYWORD, "isset").setName("ISSET"), INTEGER_NUMBER_RANGE).setName("SCORE_VALUE")
                    ).setName("SCORE_ENTRY"), comma()).setOptional().setName("SCORE_LIST"),
                    brace("}")
            ).setName("SCORE_ARGUMENT_BLOCK");

            SELECTOR_ARGUMENT.add(group(
                    choice("scores").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(scoreArgumentBlock).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }

        {
            //NBT argument

            SELECTOR_ARGUMENT.add(group(
                    choice("nbt").setName("SELECTOR_ARGUMENT_KEY"),
                    equals(),
                    choice(group(not().setOptional(), NBT_COMPOUND)).setName("SELECTOR_ARGUMENT_VALUE")
            ));
        }
        //endregion
        //region Coordinates
        {
            COORDINATE_SET.addTags("cspn:Position");

            LOCAL_COORDINATE.add(new LazyTokenGroupMatch().append(caret()).append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(ofType(SHORT_REAL_NUMBER))));

            ABSOLUTE_COORDINATE.add(ofType(SHORT_REAL_NUMBER));

            RELATIVE_COORDINATE.add(new LazyTokenGroupMatch().append(tilde()).append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(ofType(SHORT_REAL_NUMBER))));

            MIXABLE_COORDINATE.add(ABSOLUTE_COORDINATE);
            MIXABLE_COORDINATE.add(RELATIVE_COORDINATE);

            SINGLE_COORDINATE.add(MIXABLE_COORDINATE);
            SINGLE_COORDINATE.add(LOCAL_COORDINATE);

            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("MIXED_COORDINATE_SET");
                g.append(MIXABLE_COORDINATE);
                g.append(MIXABLE_COORDINATE);
                g.append(MIXABLE_COORDINATE);
                COORDINATE_SET.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("LOCAL_COORDINATE_SET");
                g.append(LOCAL_COORDINATE);
                g.append(LOCAL_COORDINATE);
                g.append(LOCAL_COORDINATE);
                COORDINATE_SET.add(g);
            }

            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("MIXED_TWO_COORDINATE_SET");
                g.append(MIXABLE_COORDINATE);
                g.append(MIXABLE_COORDINATE);
                TWO_COORDINATE_SET.add(g);
                ROTATION.add(g);
            }

            COORDINATE_SET.add(INTERPOLATION_BLOCK);
            TWO_COORDINATE_SET.add(INTERPOLATION_BLOCK);
            ROTATION.add(INTERPOLATION_BLOCK);
        }
        //endregion
        //endregion

        //region Definition Pack grammar
        try {

            HashMap<String, LazyTokenPatternMatch> namespaces = new HashMap<>();

            for(Namespace namespace : module.getAllNamespaces()) {
                LazyTokenPatternMatch g = group(literal(namespace.getName()), colon()).setOptional(namespace.getName().equals("minecraft")).setName("NAMESPACE");
                namespaces.put(namespace.getName(), g);
            }

            HashMap<String, LazyTokenStructureMatch> categoryMap = new HashMap<>();
            for(Namespace namespace : module.getAllNamespaces()) {
                LazyTokenPatternMatch namespaceGroup = namespaces.get(namespace.getName());
                Boolean usesNamespace = null;
                for(TypeDictionary dict : namespace.types.getAllDictionaries()) {
                    LazyTokenStructureMatch typeName = struct("TYPE_NAME");
                    boolean any = false;
                    String category = dict.getCategory();
                    if(!categoryMap.containsKey(category)) {
                        categoryMap.put(category, struct(category.toUpperCase() + "_ID"));
                        categoryMap.get(category).add(INTERPOLATION_BLOCK).add(ofType(STRING_LITERAL).setName("STRING_LITERAL"));
                    }
                    for(Type type : dict.list()) {
                        String name = type.getName();
                        if(type instanceof AliasType) {
                            name = ((AliasType)type).getAliasName();
                        }
                        typeName.add(identifierA(name).addTags(SuggestionTags.LITERAL_SORT, (category.equals(BlockType.CATEGORY) || category.equals(ItemType.CATEGORY) || category.equals(EntityType.CATEGORY)) ? SuggestionTags.DISABLED : SuggestionTags.ENABLED));
                        any = true;
                        usesNamespace = type.useNamespace();
                    }
                    if(any) {
                        categoryMap.get(category).add((usesNamespace ? group(resourceLocationFixer, namespaceGroup, typeName) : group(typeName)).setName(category.toUpperCase() + "_ID_DEFAULT"));
                    }
                }
            }

            STRUCTURE.add(categoryMap.get(StructureType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Structure");
            DIFFICULTY.add(categoryMap.get(DifficultyType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Difficulty");
            GAMEMODE.add(categoryMap.get(GamemodeType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Gamemode");
            DIMENSION_ID.add(categoryMap.get(DimensionType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Dimension");
            if(checkVersionFeature(module, "custom_dimensions", false)) {
                DIMENSION_ID.add(RESOURCE_LOCATION_S);
            }
            ATTRIBUTE_ID.add(categoryMap.get(AttributeType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Attribute");
            BIOME_ID.add(categoryMap.get(BiomeType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Biome");

            BLOCK_ID.add(categoryMap.get(BlockType.CATEGORY).addTags(SuggestionTags.DISABLED)).addTags(SuggestionTags.ENABLED, TridentSuggestionTags.BLOCK);
            ITEM_ID.add(categoryMap.get(ItemType.CATEGORY).addTags(SuggestionTags.DISABLED)).addTags(SuggestionTags.ENABLED, TridentSuggestionTags.ITEM);
            ENTITY_ID.add(categoryMap.get(EntityType.CATEGORY).addTags(SuggestionTags.DISABLED)).addTags(SuggestionTags.ENABLED, TridentSuggestionTags.ENTITY_TYPE);

            EFFECT_ID.add(categoryMap.get(EffectType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Effect");
            ENCHANTMENT_ID.add(categoryMap.get(EnchantmentType.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags("cspn:Enchantment");
            SLOT_ID.add(categoryMap.get(ItemSlot.CATEGORY)).addTags(SuggestionTags.ENABLED).addTags(SuggestionTags.ENABLED).addTags("cspn:Item Slot");

            LazyTokenGroupMatch COLOR = new LazyTokenGroupMatch().setName("COLOR")
                    .append(real().setName("RED_COMPONENT").addTags("cspn:Red Component"))
                    .append(real().setName("GREEN_COMPONENT").addTags("cspn:Green Component"))
                    .append(real().setName("BLUE_COMPONENT").addTags("cspn:Blue Component"));
            COLOR.addTags("cspn:RGB Color");


            //particles have to be different
            PARTICLE.addTags("cspn:Particle");

            {
                for(Namespace namespace : module.getAllNamespaces()) {
                    LazyTokenPatternMatch namespaceGroup = namespaces.get(namespace.getName());
                    for(Type type : namespace.types.particle.list()) {
                        LazyTokenGroupMatch g = group(namespaceGroup, literal(type.getName()).setName("TYPE_NAME")).setName("PARTICLE_ID");

                        PARTICLE_ID.add(g);

                        LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();

                        g2.append(g);

                        LazyTokenGroupMatch argsGroup = new LazyTokenGroupMatch().setName("PARTICLE_ARGUMENTS");

                        String allArgs = type.getProperty("argument");
                        if (allArgs != null && !allArgs.equals("none")) {
                            String[] args = allArgs.split("-");
                            for (String arg : args) {
                                switch (arg) {
                                    case "int": {
                                        argsGroup.append(integer());
                                        break;
                                    }
                                    case "double": {
                                        argsGroup.append(real());
                                        break;
                                    }
                                    case "color": {
                                        argsGroup.append(COLOR);
                                        break;
                                    }
                                    case "block": {
                                        argsGroup.append(BLOCK);
                                        break;
                                    }
                                    case "item": {
                                        argsGroup.append(ITEM);
                                        break;
                                    }
                                    default: {
                                        Debug.log("Invalid particle argument type '" + arg + "', could not be added to .tdn particle production", Debug.MessageType.ERROR);
                                    }
                                }
                            }
                        }

                        g2.append(argsGroup);

                        PARTICLE.add(g2);
                    }
                }
            }

            GAMERULE.addTags("cspn:Gamerule");
            {
                for(Type type : module.minecraft.types.gamerule.list()) {
                    LazyTokenGroupMatch g = group(literal(type.getName()).setName("TYPE_NAME")).setName("GAMERULE_ID");

                    GAMERULE.add(g);

                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();

                    g2.append(g);

                    LazyTokenGroupMatch argsGroup = new LazyTokenGroupMatch().setName("GAMERULE_ARGUMENT");

                    String arg = type.getProperty("argument");

                    switch (arg) {
                        case "boolean": {
                            argsGroup.append(rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Boolean"));
                            break;
                        }
                        case "int": {
                            argsGroup.append(integer().addTags("cspn:Integer"));
                            break;
                        }
                        case "double": {
                            argsGroup.append(real().addTags("cspn:Double"));
                            break;
                        }
                        case "color": {
                            argsGroup.append(COLOR);
                            break;
                        }
                        case "block": {
                            argsGroup.append(BLOCK);
                            break;
                        }
                        case "item": {
                            argsGroup.append(ITEM);
                            break;
                        }
                        default: {
                            Debug.log("Invalid gamerule argument type '" + arg + "', could not be added to .mcfunction gamerule setter production", Debug.MessageType.ERROR);
                        }
                    }

                    g2.append(sameLine());
                    g2.append(argsGroup);

                    GAMERULE_SETTER.add(g2);
                }
            }

            {
                ENTITY_ID_TAGGED.add(group(resourceLocationFixer, ENTITY_ID).setName("ENTITY_ID_WRAPPER"));
                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch().setName("ABSTRACT_RESOURCE");
                g2.append(resourceLocationFixer);
                g2.append(new LazyTokenGroupMatch().append(hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.ENTITY_TYPE_TAG)).append(ofType(GLUE)).append(RESOURCE_LOCATION_S).setName("RESOURCE_NAME"));
                g2.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
                ENTITY_ID_TAGGED.add(g2);

                ENTITY_ID.add(INTERPOLATION_BLOCK);
                ENTITY_ID_TAGGED.addTags("cspn:Entity Type Tag");
            }

        } catch (Exception x) {
            throw new RuntimeException("Error in creating version-specific syntax: " + x.getClass() + ": " + x.getMessage());
        }
        //endregion

        //region Instructions


        LazyTokenPatternMatch scale = group(matchItem(COMPILER_OPERATOR, "*"), real()).setOptional().setName("SCALE");
        LazyTokenPatternMatch typeCast = group(brace("("), numericDataType().setName("NUMERIC_DATA_TYPE"), brace(")")).setOptional().setName("TYPE_CAST");

        LazyTokenGroupMatch scoreHead = group(ofType(ARROW), objectiveName()).setName("SCORE_POINTER_HEAD");
        LazyTokenGroupMatch nbtHead = group(dot(), NBT_PATH, scale, typeCast).setName("NBT_POINTER_HEAD");
        LazyTokenGroupMatch storageHead = group(tilde(), NBT_PATH, scale, typeCast).setName("STORAGE_POINTER_HEAD");

        LazyTokenStructureMatch anyHead = choice(scoreHead, nbtHead, storageHead).setName("POINTER_HEAD");

        LazyTokenGroupMatch varPointer = group(INTERPOLATION_BLOCK, optional(anyHead).setName("POINTER_HEAD_WRAPPER")).setName("VARIABLE_POINTER");
        LazyTokenGroupMatch entityPointer = group(LIMITED_ENTITY, anyHead).setName("ENTITY_POINTER");
        LazyTokenGroupMatch blockPointer = group(brace("("), COORDINATE_SET, brace(")"), nbtHead).setName("BLOCK_POINTER");
        LazyTokenGroupMatch storagePointer = group(resourceLocationFixer, RESOURCE_LOCATION_S, storageHead).setName("STORAGE_POINTER");

        //LazyTokenGroupMatch nbtPointer = group(choice(ENTITY, group(brace("("), COORDINATE_SET, brace(")"))), nbtHead);

        POINTER.add(entityPointer);
        POINTER.add(varPointer);
        POINTER.add(blockPointer);
        POINTER.add(storagePointer);
        POINTER.addTags("cspn:Pointer", SuggestionTags.ENABLED);

        COMMAND.add(
                group(matchItem(COMMAND_HEADER, "set"), POINTER, ofType(SCOREBOARD_OPERATOR).setName("OPERATOR"), choice(POINTER, NBT_VALUE, INTERPOLATION_BLOCK, ofType(NULL).setName("NULL")).setName("VALUE"))
        );

        {
            COMMAND.add(
                    group(matchItem(COMMAND_HEADER, "gamelog"), choice("info", "debug", "warning", "error", "fatal").setName("DEBUG_GROUP"), LINE_SAFE_INTERPOLATION_VALUE)
            );
        }

        LazyTokenPatternMatch SYMBOL_MODIFIER_LIST = list(choice("static", "final")).setOptional().setName("SYMBOL_MODIFIER_LIST").addProcessor(
                (p, lx) -> checkDuplicates(((TokenList) p), "Duplicate modifier", lx)
        );

        {
            LazyTokenStructureMatch entityBodyEntry = choice(
                    group(literal("default"), literal("nbt"), NBT_COMPOUND).setName("DEFAULT_NBT"),
                    group(literal("default"), literal("passengers"), brace("["), list(NEW_ENTITY_LITERAL, comma()).setName("PASSENGER_LIST"), brace("]")).setName("DEFAULT_PASSENGERS"),
                    group(literal("default"), literal("health"), real().setName("HEALTH").addTags("cspn:Health")).setName("DEFAULT_HEALTH"),
                    group(literal("default"), literal("name"), TEXT_COMPONENT).setName("DEFAULT_NAME"),
                    group(literal("var"), SYMBOL_MODIFIER_LIST, identifierX().setName("SYMBOL_NAME").addTags("cspn:Field Name"), INFERRABLE_TYPE_CONSTRAINTS, optional(equals(), choice(LINE_SAFE_INTERPOLATION_VALUE, INTERPOLATION_BLOCK).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")).setName("ENTITY_FIELD"),
                    group(literal("eval"), LINE_SAFE_INTERPOLATION_VALUE).setName("ENTITY_EVAL"),
                    group(literal("on"), group(INTERPOLATION_VALUE).setName("EVENT_NAME"), list(MODIFIER).setOptional().setName("EVENT_MODIFIERS"), literal("function"),
                            OPTIONAL_NAME_INNER_FUNCTION).setName("ENTITY_EVENT_IMPLEMENTATION"),
                    COMMENT_S,
                    group(choice(group(literal("ticking"), ofType(TIME).setOptional().setName("TICKING_INTERVAL"), list(MODIFIER).setOptional().setName("TICKING_MODIFIERS")).setName("TICKING_ENTITY_FUNCTION")).setOptional().setName("ENTITY_FUNCTION_MODIFIER"), literal("function"), OPTIONAL_NAME_INNER_FUNCTION).setName("ENTITY_INNER_FUNCTION")
            );
            entityBodyEntry.addTags(TridentSuggestionTags.CONTEXT_ENTITY_BODY);

            LazyTokenPatternMatch entityBody = group(
                    brace("{"),
                    list(entityBodyEntry).setOptional().setName("ENTITY_BODY_ENTRIES"),
                    brace("}")
            ).setOptional().setName("ENTITY_DECLARATION_BODY");


            LazyTokenStructureMatch itemBodyEntry = choice(
                    group(literal("default"), literal("nbt"), NBT_COMPOUND).setName("DEFAULT_NBT"),
                    group(
                            choice(
                                    group(literal("on"), choice(
                                            group(choice("used", "broken", "crafted", "dropped", "picked_up").setName("ITEM_CRITERIA_KEY")).setName("ITEM_CRITERIA")
                                    ).setName("FUNCTION_ON_INNER"), literal("pure").setOptional(), list(MODIFIER).setOptional().setName("EVENT_MODIFIERS")).setName("FUNCTION_ON")
                            ).setOptional().setName("INNER_FUNCTION_MODIFIERS"),
                            literal("function"),
                            OPTIONAL_NAME_INNER_FUNCTION).setName("ITEM_INNER_FUNCTION"),
                    group(literal("default"), literal("name"), TEXT_COMPONENT).setName("DEFAULT_NAME"),
                    group(literal("default"), literal("lore"), brace("["), list(TEXT_COMPONENT, comma()).setOptional().setName("LORE_LIST"), brace("]")).setName("DEFAULT_LORE"),
                    COMMENT_S,
                    group(literal("var"), SYMBOL_MODIFIER_LIST, identifierX().setName("SYMBOL_NAME").addTags("cspn:Field Name"), INFERRABLE_TYPE_CONSTRAINTS, optional(equals(), choice(LINE_SAFE_INTERPOLATION_VALUE, INTERPOLATION_BLOCK).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")).setName("ITEM_FIELD"),
                    group(literal("eval"), LINE_SAFE_INTERPOLATION_VALUE).setName("ITEM_EVAL")
            );
            itemBodyEntry.addTags(TridentSuggestionTags.CONTEXT_ITEM_BODY);

            LazyTokenPatternMatch itemBody = group(
                    brace("{"),
                    list(itemBodyEntry).setOptional().setName("ITEM_BODY_ENTRIES"),
                    brace("}")
            ).setOptional().setName("ITEM_DECLARATION_BODY");

            LazyTokenPatternMatch classGetter = group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("get"), TYPE_CONSTRAINTS, ANONYMOUS_INNER_FUNCTION).setName("CLASS_GETTER");
            LazyTokenPatternMatch classSetter = group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("set"), brace("("), FORMAL_PARAMETER, brace(")"), ANONYMOUS_INNER_FUNCTION).setName("CLASS_SETTER").setOptional();

            LazyTokenStructureMatch classBodyEntry = choice(
                    group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), SYMBOL_MODIFIER_LIST, literal("override").setOptional().setName("MEMBER_PARENT_MODE"), literal("var"), identifierX().setName("SYMBOL_NAME").addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), p.getStringLocation().index);
                            ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                        }
                    }), INFERRABLE_TYPE_CONSTRAINTS, optional(equals(), choice(INTERPOLATION_VALUE).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")).setName("CLASS_MEMBER")
                    .addProcessor(
                            (p, lx) -> {
                                if(p.find("MEMBER_PARENT_MODE") != null && p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                                    lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a static member", p.find("MEMBER_PARENT_MODE")));
                                }
                            }
                    ).addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                            sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.LOCAL));
                            sym.addTag(TridentSuggestionTags.TAG_FIELD);

                            if(p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                                sym.setStaticField(true);
                            } else {
                                sym.setInstanceField(true);
                            }
                            if(!sym.hasSubBlock()) {
                                ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                            }
                        }
                    }),
                    group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), SYMBOL_MODIFIER_LIST, literal("override").setOptional().setName("MEMBER_PARENT_MODE"), choice(literal("new").setName("CONSTRUCTOR_LABEL"), identifierX()).setName("SYMBOL_NAME"), DYNAMIC_FUNCTION).setName("CLASS_FUNCTION").addProcessor(
                            (p, lx) -> {
                                boolean overriding = p.find("MEMBER_PARENT_MODE") != null;
                                boolean _static = p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static");
                                if(overriding && _static) {
                                    lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a static member", p.find("MEMBER_PARENT_MODE")));
                                }
                                if(p.find("SYMBOL_NAME").flatten(false).equals("new")) {
                                    if(overriding) {
                                        lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a constructor", p.find("MEMBER_PARENT_MODE")));
                                    }
                                    if(_static) {
                                        lx.getNotices().add(new Notice(NoticeType.ERROR, "'static' modifier not allowed here", p.find("SYMBOL_MODIFIER_LIST")));
                                    }
                                }
                            }
                    ).addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            String methodName = p.find("SYMBOL_NAME").flatten(false);
                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), methodName, p.find("SYMBOL_NAME").getStringLocation().index);
                            sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.LOCAL));
                            sym.addTag(TridentSuggestionTags.TAG_METHOD);
                            if(p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                                sym.setStaticField(true);
                            } else {
                                sym.setInstanceField(true);
                            }
                            if(!sym.hasSubBlock()) {
                                ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                            }
                        }
                    }),
                    group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), list(choice("final")).setOptional().setName("SYMBOL_MODIFIER_LIST"), literal("override").setOptional().setName("MEMBER_PARENT_MODE"), literal("this"), brace("["), FORMAL_PARAMETER, brace("]"), brace("{"), classGetter, classSetter, brace("}")).setName("CLASS_INDEXER"),
                    group(literal("override").setOptional(), choice("explicit", "implicit").setName("CLASS_TRANSFORM_TYPE"), brace("<"), INTERPOLATION_TYPE, brace(">"), ANONYMOUS_INNER_FUNCTION).setName("CLASS_OVERRIDE"),
                    COMMENT_S
            ).setName("CLASS_BODY_ENTRY").setGreedy(true);
            itemBodyEntry.addTags(TridentSuggestionTags.CONTEXT_CLASS_BODY);

            LazyTokenPatternMatch classBody = group(
                    brace("{").addProcessor(startComplexValue),
                    list(classBodyEntry).setOptional().setName("CLASS_BODY_ENTRIES"),
                    brace("}")
            ).setOptional().setName("CLASS_DECLARATION_BODY").addProcessor(claimTopSymbol).addProcessor(endComplexValue).addFailProcessor((n, l) -> {if(n > 0) endComplexValue.accept(null, l);});

            INSTRUCTION.add(
                    group(instructionKeyword("define"),
                            choice(
                                    group(literal("objective"), group(identifierA()).setName("OBJECTIVE_NAME").addTags("cspn:Objective Name"), optional(sameLine(), group(identifierB()).setName("CRITERIA"), optional(TEXT_COMPONENT))).setName("DEFINE_OBJECTIVE")
                                            .addProcessor((p, l) -> {
                                                if(l.getSummaryModule() != null) {
                                                    ((TridentSummaryModule) l.getSummaryModule()).addObjective(new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.find("OBJECTIVE_NAME").flatten(false), p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_OBJECTIVE));
                                                }
                                            }),
                                    group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("entity"), choice(
                                            group(choice(identifierA(), identifierX().addTags("cspn:Entity Name"), literal("default")).setName("ENTITY_NAME").addTags("cspn:Entity Type Name"), choice(symbol("*"), ENTITY_ID_TAGGED).setName("ENTITY_BASE").addTags("cspn:Base Type")).setName("CONCRETE_ENTITY_DECLARATION").addProcessor(
                                                    (p, lx) -> {
                                                        if("*".equals(p.find("ENTITY_BASE").flatten(false)) && !"default".equals(p.find("ENTITY_NAME").flatten(false))) {
                                                            lx.getNotices().add(new Notice(NoticeType.ERROR, "The wildcard entity base may only be used on default entities", p.find("ENTITY_BASE")));
                                                        }
                                                    }
                                            ),
                                            group(literal("component"), choice(identifierA(), identifierX()).setName("ENTITY_NAME").addTags("cspn:Component Name")).setName("ABSTRACT_ENTITY_DECLARATION")
                                    ).setName("ENTITY_DECLARATION_HEADER"), optional(keyword("implements"), list(INTERPOLATION_VALUE, comma()).setName("COMPONENT_LIST").addTags("cspn:Implemented Components")).setName("IMPLEMENTED_COMPONENTS"), entityBody).setName("DEFINE_ENTITY")
                                            .addProcessor((p, l) -> {
                                                if(l.getSummaryModule() != null) {
                                                    TokenPattern<?> namePattern = p.find("ENTITY_DECLARATION_HEADER.ENTITY_NAME");
                                                    if(namePattern.find("IDENTIFIER_A") != null) return;
                                                    String name = namePattern.flatten(false);
                                                    if(!name.equals("default")) {
                                                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, p.getStringLocation().index);
                                                        sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                                                        sym.addTag(TridentSuggestionTags.TAG_CUSTOM_ENTITY);
                                                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.GLOBAL));
                                                        if(p.find("ENTITY_DECLARATION_HEADER.LITERAL_COMPONENT") != null) sym.addTag(TridentSuggestionTags.TAG_ENTITY_COMPONENT);
                                                        ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                                    } else {
                                                        if(p.find("IMPLEMENTED_COMPONENTS") != null) {
                                                            l.getNotices().add(new Notice(NoticeType.ERROR, "Default entities may not implement components", p.find("IMPLEMENTED_COMPONENTS")));
                                                        }

                                                        TokenList body = (TokenList) p.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");
                                                        if(body != null) {
                                                            for(TokenPattern<?> entry : body.getContents()) {
                                                                if(((TokenStructure) entry).getContents().getName().startsWith("DEFAULT_")) {
                                                                    l.getNotices().add(new Notice(NoticeType.ERROR, "Default properties are not allowed for default entities", entry));
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }),
                                    group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("event"), group(identifierX().addTags("cspn:Event Name")).setName("EVENT_NAME"), optional(ANONYMOUS_INNER_FUNCTION).setName("EVENT_INITIALIZATION")).setName("DEFINE_EVENT")
                                            .addProcessor((p, l) -> {
                                                if(l.getSummaryModule() != null) {
                                                    TokenPattern<?> namePattern = p.find("EVENT_NAME");
                                                    String name = namePattern.flatten(false);
                                                    SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_VARIABLE);
                                                    sym.addTag(TridentSuggestionTags.TAG_ENTITY_EVENT);
                                                    sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.LOCAL));
                                                    ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                                }
                                            }),
                                    group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("item"), choice(identifierA(), identifierX().addTags("cspn:Item Type Name"), literal("default")).setName("ITEM_NAME"), noToken().addTags("cspn:Base Type"), resourceLocationFixer, ITEM_ID, optional(hash(), integer().addTags("cspn:Model Index")).setName("CUSTOM_MODEL_DATA"), itemBody).setName("DEFINE_ITEM")
                                            .addProcessor((p, l) -> {
                                                if(l.getSummaryModule() != null) {
                                                    TokenPattern<?> namePattern = p.find("ITEM_NAME");
                                                    if(namePattern.find("IDENTIFIER_A") != null) return;
                                                    String name = namePattern.flatten(false);
                                                    if(!name.equals("default")) {
                                                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_VARIABLE);
                                                        sym.addTag(TridentSuggestionTags.TAG_CUSTOM_ITEM);
                                                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.GLOBAL));
                                                        ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                                    } else {
                                                        if(p.find("CUSTOM_MODEL_DATA") != null) {
                                                            l.getNotices().add(new Notice(NoticeType.ERROR, "Default items don't support custom model data specifiers", p.find("CUSTOM_MODEL_DATA")));
                                                        }

                                                        TokenList body = (TokenList) p.find("ITEM_DECLARATION_BODY.ITEM_BODY_ENTRIES");
                                                        if(body != null) {
                                                            for(TokenPattern<?> entry : body.getContents()) {
                                                                if(((TokenStructure) entry).getContents().getName().startsWith("DEFAULT_")) {
                                                                    l.getNotices().add(new Notice(NoticeType.ERROR, "Default properties are not allowed for default items", entry));
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }),
                                    group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), SYMBOL_MODIFIER_LIST, literal("class"),
                                            group(identifierX().addTags("cspn:Class Name")).setName("CLASS_NAME")
                                                    .addProcessor((p, l) -> {
                                                            if(l.getSummaryModule() != null) {
                                                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), p.getStringLocation().index);
                                                                sym.addTag(TridentSuggestionTags.TAG_CLASS);
                                                                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                                                                ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                                                            }
                                                    }),
                                            optional(colon(), list(INTERPOLATION_TYPE, comma()).addTags("cspn:Superclasses").setName("SUPERCLASS_LIST").addProcessor((p, l) -> checkDuplicates(((TokenList) p), "Duplicate superclass", l))).setName("CLASS_INHERITS"), classBody).setName("DEFINE_CLASS")
                                                    .addProcessor((p, l) -> {
                                                        if(l.getSummaryModule() != null) {
                                                            SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                                                            sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.LOCAL));
                                                            if(!sym.hasSubBlock()) {
                                                                ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                                            }
                                                        }
                                                    }),
                                    group(literal("function"), INNER_FUNCTION).setName("DEFINE_FUNCTION")
                            )
                    )
            );
        }

        LazyTokenPatternMatch VARIABLE_DECLARATION = group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), list(choice("final")).setOptional().setName("SYMBOL_MODIFIER_LIST"), instructionKeyword("var"),
                identifierX().setName("SYMBOL_NAME").addTags("cspn:Variable Name").addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), p.getStringLocation().index);
                        ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                    }
                }),
                INFERRABLE_TYPE_CONSTRAINTS,
                optional(equals(), choice(LINE_SAFE_INTERPOLATION_VALUE, INTERPOLATION_BLOCK).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")
        ).setName("VARIABLE_DECLARATION").addProcessor((p, l) -> {
            if(l.getSummaryModule() != null) {
                SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();

                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                TokenStructure root = ((TokenStructure) p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE.LINE_SAFE_INTERPOLATION_VALUE.EXPRESSION.MID_INTERPOLATION_VALUE.SURROUNDED_INTERPOLATION_VALUE.INTERPOLATION_CHAIN.ROOT_INTERPOLATION_VALUE"));
                if(root != null) {
                    switch(root.getContents().getName()) {
                        case "WRAPPED_ENTITY": {
                            sym.addTag(TridentSuggestionTags.TAG_ENTITY);
                            break;
                        }
                        case "WRAPPED_ITEM": {
                            sym.addTag(TridentSuggestionTags.TAG_ITEM);
                            break;
                        }
                        case "WRAPPED_COORDINATE": {
                            sym.addTag(TridentSuggestionTags.TAG_COORDINATE);
                            break;
                        }
                    }
                }
                sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), Symbol.SymbolVisibility.LOCAL));
                if(!sym.hasSubBlock()) {
                    ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                }
            }
        });

        {
            INSTRUCTION.add(
                    VARIABLE_DECLARATION
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("within"),
                            identifierX().setName("VARIABLE_NAME"),
                            group(COORDINATE_SET).setName("FROM").addTags("cspn:From"), group(COORDINATE_SET).setName("TO").addTags("cspn:To"), optional(literal("step"), real().addTags("cspn:Step")).setName("STEP"), ANONYMOUS_INNER_FUNCTION
                    ).addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.find("VARIABLE_NAME").flatten(false), p.find("ANONYMOUS_INNER_FUNCTION.FILE_INNER.FILE_START_MARKER").getStringLocation().index);
                            sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                            sym.setVisibility(Symbol.SymbolVisibility.LOCAL);
                            ((TridentSummaryModule) l.getSummaryModule()).peek().putLateElement(sym);
                        }
                    })
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("using"),
                            choice(
                                    group(literal("tag"), group(identifierA()).setName("USING_TAG_NAME").addTags("cspn:Tag"), ENTITY, list(MODIFIER).setOptional().setName("MODIFIER_LIST")).setName("USING_TAG"),
                                    group(literal("summon"), NEW_ENTITY_LITERAL, optional(COORDINATE_SET, optional(NBT_COMPOUND)), literal("with"), group(identifierA()).setName("USING_SUMMON_TAG_NAME").addTags("cspn:Summoning Tag"), list(MODIFIER).setOptional().setName("MODIFIER_LIST")).setName("USING_SUMMON")
                            ).setName("USING_CASE"),
                            ANONYMOUS_INNER_FUNCTION
                    )
            );
        }

        LazyTokenGroupMatch blockLabel = optional(identifierX().setName("LABEL"), colon()).setName("BLOCK_LABEL");

        {
            INSTRUCTION.add(
                    group(instructionKeyword("eval"), LINE_SAFE_INTERPOLATION_VALUE)
            );
        }

        {
            LazyTokenPatternMatch FOR_HEADER = choice(
                    group(identifierX().setName("VARIABLE_NAME").addTags("cspn:Iterator Name"), instructionKeyword("in", false), noToken().addTags("cspn:Iterable"), INTERPOLATION_VALUE).setName("ITERATOR_FOR"),
                    group(choice(INTERPOLATION_VALUE, group(VARIABLE_DECLARATION)).setOptional().setName("FOR_HEADER_INITIALIZATION").addTags("cspn:Initialization"), symbol(";"), optional(INTERPOLATION_VALUE).setName("FOR_HEADER_CONDITION").addTags("cspn:Loop Condition"), symbol(";"), optional(INTERPOLATION_VALUE).setName("FOR_HEADER_ITERATION").addTags("cspn:Iteration Expression")).setName("CLASSICAL_FOR")
            ).setName("LOOP_HEADER");

            INSTRUCTION.add(
                    group(blockLabel, instructionKeyword("for"), brace("("), FOR_HEADER, brace(")"), ANONYMOUS_INNER_FUNCTION)
                    .addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            TokenPattern<?> iteratorName = p.find("LOOP_HEADER.VARIABLE_NAME");
                            if(iteratorName != null) {
                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), iteratorName.flatten(false), p.find("ANONYMOUS_INNER_FUNCTION").getStringLocation().index);
                                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                                sym.setVisibility(Symbol.SymbolVisibility.LOCAL);
                                ((TridentSummaryModule) l.getSummaryModule()).peek().putLateElement(sym);
                            }
                        }
                    })
            );
        }

        {
            LazyTokenPatternMatch WHILE_HEADER = choice(
                    group(INTERPOLATION_VALUE).setName("WHILE_HEADER").addTags("cspn:Loop Condition")
            ).setName("LOOP_HEADER");

            INSTRUCTION.add(
                    group(blockLabel, instructionKeyword("while"), brace("("), WHILE_HEADER, brace(")"), ANONYMOUS_INNER_FUNCTION)
            );
        }

        {
            INSTRUCTION.add(
                    group(keyword("do").setOptional(), instructionKeyword("if"), brace("("), group(INTERPOLATION_VALUE).setName("CONDITION").addTags("cspn:Condition"), brace(")"), choice(ANONYMOUS_INNER_FUNCTION, ENTRY).setName("EXECUTION_BLOCK"), optional(instructionKeyword("else", false), choice(ANONYMOUS_INNER_FUNCTION, ENTRY).setName("EXECUTION_BLOCK")).setName("ELSE_CLAUSE"))
            );
        }

        {
            INSTRUCTION.add(
                    group(blockLabel, instructionKeyword("switch"), brace("("), group(INTERPOLATION_VALUE).setName("SWITCH_VALUE").addTags("cspn:Switch Value"), brace(")"),
                            brace("{"),
                            list(
                                    group(
                                            choice(instructionKeyword("default", false), group(instructionKeyword("case", false), INTERPOLATION_VALUE)).setName("CASE_BRANCH"), colon(),
                                            choice(ANONYMOUS_INNER_FUNCTION).setOptional().setName("CASE_BLOCK")
                                    )
                            ).setOptional().setName("SWITCH_CASES"),
                            brace("}")
                    )
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("try"), literal("recovering").setOptional(), choice(ANONYMOUS_INNER_FUNCTION, ENTRY).setName("EXECUTION_BLOCK"), group(instructionKeyword("catch"), brace("("), identifierX().setName("EXCEPTION_VARIABLE").addTags("cspn:Exception Variable Name"), brace(")"), choice(ANONYMOUS_INNER_FUNCTION, ENTRY).setName("EXECUTION_BLOCK")).setName("CATCH_CLAUSE"))
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("throw"), noToken().addTags("cspn:Cause"), LINE_SAFE_INTERPOLATION_VALUE)
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("log"), choice("info", "warning", "error").setName("NOTICE_GROUP"), noToken().addTags("cspn:Value"), LINE_SAFE_INTERPOLATION_VALUE)
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("return"), optional(LINE_SAFE_INTERPOLATION_VALUE).setName("RETURN_VALUE").addTags("cspn:Value"))
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("break"), identifierX().setName("BREAK_LABEL").setOptional())
            );
        }

        {
            INSTRUCTION.add(
                    group(instructionKeyword("continue"), identifierX().setName("CONTINUE_LABEL").setOptional())
            );
        }
        //endregion
    }

    private static LazyTokenPatternMatch noToken() {
        return ofType(NO_TOKEN).setOptional();
    }

    private static LazyTokenItemMatch literal(String text) {
        return (LazyTokenItemMatch) new LazyTokenItemMatch(TokenType.UNKNOWN, text).setName("LITERAL_" + text.toUpperCase()).addTags(SuggestionTags.ENABLED);
    }

    private LazyTokenStructureMatch numericDataType() {
        return choice("byte", "double", "float", "int", "long", "short").setName("NUMERIC_DATA_TYPE");
    }

    private LazyTokenPatternMatch anchor() {
        return choice("feet", "eyes").setName("ANCHOR").addTags(SuggestionTags.ENABLED).addTags("cspn:Anchor");
    }

    private static LazyTokenItemMatch symbol(String text) {
        return new LazyTokenItemMatch(SYMBOL, text).setName("SYMBOL");
    }

    private static LazyTokenItemMatch keyword(String text) {
        return matchItem(KEYWORD, text).setName("KEYWORD_" + text.toUpperCase());
    }

    private static LazyTokenItemMatch instructionKeyword(String text) {
        return instructionKeyword(text, true);
    }

    private static LazyTokenItemMatch instructionKeyword(String text, boolean updateContext) {
        LazyTokenItemMatch item = keyword(text).setName("INSTRUCTION_KEYWORD");
        item.addTags(TridentSuggestionTags.TAG_INSTRUCTION);
        return item;
    }

    private static LazyTokenItemMatch matchItem(TokenType type, String text) {
        LazyTokenItemMatch item = new LazyTokenItemMatch(type, text).setName("ITEM_MATCH");
        if(type == COMMAND_HEADER) {
            item.addTags(TridentSuggestionTags.TAG_COMMAND);
        } else if(type == MODIFIER_HEADER) {
            item.addTags(TridentSuggestionTags.TAG_MODIFIER);
        }
        return item;
    }

    private static LazyTokenItemMatch brace(String brace) {
        LazyTokenItemMatch item = matchItem(BRACE, brace);
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    private static LazyTokenItemMatch colon() {
        return ofType(COLON);
    }

    private static LazyTokenItemMatch comma() {
        LazyTokenItemMatch item = ofType(COMMA).setName("COMMA");
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    private static LazyTokenItemMatch dot() {
        LazyTokenItemMatch item = ofType(DOT);
        item.addTags(SuggestionTags.DISABLED);
        return item;
    }

    private LazyTokenPatternMatch objectiveName() {
        return group(identifierA()).setName("OBJECTIVE_NAME").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.OBJECTIVE_EXISTING).addTags("cspn:Objective");
    }

    private LazyTokenPatternMatch score() {
        return choice(group(ENTITY, objectiveName()).setName("EXPLICIT_SCORE"), group(literal("deref"), sameLine(), INTERPOLATION_BLOCK).setName("POINTER_WRAPPER")).setName("SCORE").addTags("cspn:Score");
    }

    private LazyTokenStructureMatch scoreOptionalObjective() {
        return (LazyTokenStructureMatch) choice(group(choice(ENTITY, symbol("*")).setName("TARGET_ENTITY"), group(sameLine(), choice(symbol("*"), objectiveName()).setName("OBJECTIVE_NAME_WRAPPER")).setOptional().setName("OBJECTIVE_CLAUSE")).setName("SCORE_OPTIONAL_OBJECTIVE"), POINTER).setName("SCORE").addTags("cspn:Score");
    }

    private static LazyTokenItemMatch equals() {
        return ofType(EQUALS);
    }

    private static LazyTokenItemMatch caret() {
        return ofType(CARET);
    }

    private static LazyTokenItemMatch tilde() {
        return ofType(TILDE);
    }

    private static LazyTokenItemMatch not() {
        return ofType(NOT).setName("NEGATED");
    }

    private static LazyTokenItemMatch hash() {
        return ofType(HASH).setName("HASH");
    }

    private LazyTokenStructureMatch string() {
        return choice(ofType(STRING_LITERAL).setName("STRING_LITERAL"), INTERPOLATION_BLOCK).setName("STRING");
    }

    private LazyTokenStructureMatch integer() {
        return choice(ofType(INTEGER_NUMBER).setName("RAW_INTEGER"), INTERPOLATION_BLOCK).setName("INTEGER");
    }

    private LazyTokenStructureMatch real() {
        return choice(ofType(REAL_NUMBER).setName("RAW_REAL"), INTERPOLATION_BLOCK).setName("REAL");
    }

    private LazyTokenPatternMatch rawBoolean() {
        return ofType(BOOLEAN);
    }

    private static LazyTokenItemMatch glue() {
        return ofType(GLUE).setName("GLUE");
    }

    private static LazyTokenItemMatch sameLine() {
        return ofType(LINE_GLUE).setName("LINE_GLUE");
    }

    private LazyTokenStructureMatch identifierA() {
        return choice(string(), ofType(IDENTIFIER_TYPE_A).setName("RAW_IDENTIFIER_A")).setName("IDENTIFIER_A");
    }

    private LazyTokenPatternMatch identifierA(String literal) {
        return matchItem(IDENTIFIER_TYPE_A, literal).setName("RAW_IDENTIFIER_A");
    }

    private LazyTokenStructureMatch identifierB() {
        return choice(ofType(IDENTIFIER_TYPE_B).setName("RAW_IDENTIFIER_B"), string()).setName("IDENTIFIER_B");
    }

    private LazyTokenStructureMatch identifierBLimited() {
        return choice(ofType(IDENTIFIER_TYPE_B_LIMITED).setName("RAW_IDENTIFIER_B"), string()).setName("IDENTIFIER_B");
    }

    private static LazyTokenPatternMatch identifierC() {
        return ofType(IDENTIFIER_TYPE_C).setName("IDENTIFIER_C");
    }

    private static LazyTokenPatternMatch identifierD() {
        return ofType(IDENTIFIER_TYPE_D).setName("IDENTIFIER_D");
    }

    private static LazyTokenItemMatch ofType(TokenType type) {
        return new LazyTokenItemMatch(type);
    }

    private static LazyTokenStructureMatch struct(String name) {
        return new LazyTokenStructureMatch(name);
    }

    private static LazyTokenStructureMatch choice(LazyTokenPatternMatch... options) {
        if(options.length == 0) throw new IllegalArgumentException("Need one or more options for choice");
        LazyTokenStructureMatch s = struct("CHOICE");
        for(LazyTokenPatternMatch option : options) {
            if(option != null) s.add(option);
        }
        return s;
    }

    private static LazyTokenStructureMatch choice(String... options) {
        if(options.length == 0) throw new IllegalArgumentException("Need one or more options for choice");
        LazyTokenStructureMatch s = struct("CHOICE");
        for(String option : options) {
            s.add(literal(option));
        }
        return s;
    }

    private static LazyTokenGroupMatch optional() {
        return new LazyTokenGroupMatch(true);
    }

    private static LazyTokenGroupMatch group(LazyTokenPatternMatch... items) {
        LazyTokenGroupMatch g = new LazyTokenGroupMatch();
        for(LazyTokenPatternMatch item : items) {
            if(item != null) g.append(item);
        }
        return g;
    }

    private static LazyTokenListMatch list(LazyTokenPatternMatch pattern) {
        return list(pattern, null);
    }

    private static LazyTokenListMatch list(LazyTokenPatternMatch pattern, LazyTokenPatternMatch separator) {
        return new LazyTokenListMatch(pattern, separator);
    }

    private static LazyTokenGroupMatch optional(LazyTokenPatternMatch... items) {
        LazyTokenGroupMatch g = group(items);
        g.setOptional();
        return g;
    }

    private LazyTokenItemMatch identifierX() {
        return ofType(IDENTIFIER_TYPE_X).setName("IDENTIFIER");
    }
    private LazyTokenItemMatch identifierY() {
        return ofType(IDENTIFIER_TYPE_Y).setName("IDENTIFIER");
    }

    private static Symbol.SymbolVisibility parseVisibility(TokenPattern<?> pattern, Symbol.SymbolVisibility defaultValue) {
        if(pattern == null) return defaultValue;
        switch(pattern.flatten(false)) {
            case "global": return Symbol.SymbolVisibility.GLOBAL;
            case "public": return Symbol.SymbolVisibility.PUBLIC;
            case "local": return Symbol.SymbolVisibility.LOCAL;
            case "private": return Symbol.SymbolVisibility.PRIVATE;
            default: return defaultValue;
        }
    }

    private static LazyTokenPatternMatch versionLimited(CommandModule module, String key, boolean defaultValue, LazyTokenPatternMatch match) {
        return checkVersionFeature(module, key, defaultValue) ? match : null;
    }

    private static boolean checkVersionFeature(CommandModule module, String key, boolean defaultValue) {
        VersionFeatures featureMap = VersionFeatureManager.getFeaturesForVersion(module.getSettingsManager().getTargetVersion());
        boolean available = defaultValue;
        if(featureMap != null) {
            available = featureMap.getBoolean(key, defaultValue);
        }
        return available;
    }

    public LazyTokenPatternMatch getStructureByName(String name) {
        switch(name) {
            case "INNER_FUNCTION": return INNER_FUNCTION;
            case "ANONYMOUS_INNER_FUNCTION": return ANONYMOUS_INNER_FUNCTION;
            case "OPTIONAL_NAME_INNER_FUNCTION": return OPTIONAL_NAME_INNER_FUNCTION;
            case "MODIFIER": return MODIFIER;
            case "RESOURCE_LOCATION": return RESOURCE_LOCATION_S;
            case "RESOURCE_LOCATION_TAGGED": return RESOURCE_LOCATION_TAGGED;
            case "SELECTOR": return SELECTOR;
            case "TEXT_COMPONENT": return TEXT_COMPONENT;
            case "TEXT_COLOR": return TEXT_COLOR;
            case "INTEGER_NUMBER_RANGE": return INTEGER_NUMBER_RANGE;
            case "REAL_NUMBER_RANGE": return REAL_NUMBER_RANGE;
            case "NBT_COMPOUND": return NBT_COMPOUND;
            case "NBT_LIST": return NBT_LIST;
            case "NBT_VALUE": return NBT_VALUE;
            case "NBT_PATH": return NBT_PATH;
            case "COORDINATE_SET": return COORDINATE_SET;
            case "TWO_COORDINATE_SET": return TWO_COORDINATE_SET;
            case "ROTATION": return ROTATION;
            case "UUID": return UUID;
            case "BLOCK": return BLOCK;
            case "BLOCK_TAGGED": return BLOCK_TAGGED;
            case "ITEM": return ITEM;
            case "ITEM_TAGGED": return ITEM_TAGGED;
            case "PARTICLE": return PARTICLE;
            case "NEW_ENTITY_LITERAL": return NEW_ENTITY_LITERAL;
            case "BLOCK_ID": return BLOCK_ID;
            case "ITEM_ID": return ITEM_ID;
            case "ENTITY_ID": return ENTITY_ID;
            case "ENTITY_ID_TAGGED": return ENTITY_ID_TAGGED;
            case "EFFECT_ID": return EFFECT_ID;
            case "PARTICLE_ID": return PARTICLE_ID;
            case "ENCHANTMENT_ID": return ENCHANTMENT_ID;
            case "DIMENSION_ID": return DIMENSION_ID;
            case "ATTRIBUTE_ID": return ATTRIBUTE_ID;
            case "BIOME_ID": return BIOME_ID;
            case "SLOT_ID": return SLOT_ID;
            case "GAMEMODE": return GAMEMODE;
            case "GAMERULE": return GAMERULE;
            case "STRUCTURE": return STRUCTURE;
            case "DIFFICULTY": return DIFFICULTY;
            case "STRING_LITERAL_OR_IDENTIFIER_A": return STRING_LITERAL_OR_IDENTIFIER_A;
            case "DICTIONARY": return DICTIONARY;
            case "LIST": return LIST;
            case "ENTITY": return ENTITY;
            case "LIMITED_ENTITY": return LIMITED_ENTITY;
            case "INTERPOLATION_BLOCK": return INTERPOLATION_BLOCK;
            case "INTERPOLATION_VALUE": return INTERPOLATION_VALUE;
            case "LINE_SAFE_INTERPOLATION_VALUE": return LINE_SAFE_INTERPOLATION_VALUE;
            case "POINTER": return POINTER;
            case "INTEGER": return integer();
            case "BOOLEAN": return rawBoolean();
            case "STRING": return string();
            case "REAL": return real();
            case "OBJECTIVE_NAME": return objectiveName();
            case "SCORE": return score();
            case "SCORE_OPTIONAL_OBJECTIVE": return scoreOptionalObjective();
            case "NUMERIC_DATA_TYPE": return numericDataType();
            case "ANCHOR": return anchor();
            case "COLON": return colon();
            case "COMMA": return comma();
            case "DOT": return dot();
            case "EQUALS": return equals();
            case "CARET": return caret();
            case "TILDE": return tilde();
            case "NOT": return not();
            case "HASH": return hash();
            case "GLUE": return glue();
            case "SAME_LINE": return sameLine();
            case "IDENTIFIER_A": return identifierA();
            case "IDENTIFIER_B": return identifierB();
            case "IDENTIFIER_B_LIMITED": return identifierBLimited();
            case "IDENTIFIER_C": return identifierC();
            case "IDENTIFIER_D": return identifierD();
            case "IDENTIFIER_X": return identifierX();
            case "IDENTIFIER_Y": return identifierY();
            case "TRAILING_STRING": return ofType(TRAILING_STRING).setName("TRAILING_STRING");
            default: return null;
        }
    }

    private void checkDuplicates(TokenList list, String message, Lexer lx) {
        if(lx == null) return;
        duplicateCheck.clear();
        for(TokenPattern<?> entry : list.getContents()) {
            if(entry.getName().equals("COMMA")) continue;
            if(!duplicateCheck.add(entry.flatten(false))) {
                lx.getNotices().add(new Notice(NoticeType.ERROR, message + " '" + entry.flatten(false) + "'", entry));
            }
        }
    }

    private ArrayList<CustomCommandProduction> customCommands;

    public void registerPlugin(TridentPlugin plugin) {
        PLUGIN_NAME.add(literal(plugin.getName()));
    }

    public void registerCustomCommand(String pluginName, String commandHeader, LazyTokenPatternMatch pattern) {
        if(customCommands == null) customCommands = new ArrayList<>();
        CustomCommandProduction commandProduction = new CustomCommandProduction(pluginName, commandHeader, pattern);
        customCommands.add(commandProduction);
        commandProduction.registerNamespacedCommand(COMMAND);
    }
    private void uninstallCommands() {
        if(customCommands != null) {
            for(CustomCommandProduction customCommand : customCommands) {
                customCommand.uninstallImportedCommand(COMMAND);
            }
        }
    }

    public void importPlugin(String name, TokenPattern<?> p, Lexer lx) {
        boolean any = false;
        if(customCommands != null) {
            for(CustomCommandProduction customCommand : customCommands) {
                if(customCommand.pluginName.equals(name)) {
                    customCommand.registerImportedCommand(COMMAND);
                    any = true;
                }
            }
        }
        if(!any && p != null && lx != null) {
            lx.getNotices().add(new Notice(NoticeType.WARNING, "Plugin '" + name + "' has no commands to import.", p));
        }
    }

    private static class CustomCommandProduction {
        String pluginName;
        String commandHeader;
        LazyTokenPatternMatch pattern;

        private final LazyTokenPatternMatch namespacedPattern;
        private final LazyTokenPatternMatch importedPattern;

        public CustomCommandProduction(String pluginName, String commandHeader, LazyTokenPatternMatch pattern) {
            this.pluginName = pluginName;
            this.commandHeader = commandHeader;
            this.pattern = pattern;

            this.namespacedPattern = group(resourceLocationFixer, matchItem(COMMAND_HEADER, pluginName + ":" + commandHeader).setName("CUSTOM_COMMAND_HEADER"), pattern).addTags(TDNMetaBuilder.PLUGIN_CREATED_COMMAND_TAG);
            this.importedPattern = group(matchItem(COMMAND_HEADER, commandHeader).setName("CUSTOM_COMMAND_HEADER"), pattern).addTags(TDNMetaBuilder.PLUGIN_CREATED_COMMAND_TAG);
        }

        public void registerNamespacedCommand(LazyTokenStructureMatch COMMAND) {
            COMMAND.add(namespacedPattern);
        }

        public void registerImportedCommand(LazyTokenStructureMatch COMMAND) {
            COMMAND.add(importedPattern);
        }

        public void uninstallImportedCommand(LazyTokenStructureMatch COMMAND) {
            COMMAND.remove(importedPattern);
        }
    }
}

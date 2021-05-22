package com.energyxxer.trident.sets;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.commands.execute.EntityAnchor;
import com.energyxxer.commodore.functionlogic.commands.gamerule.GameruleSetCommand;
import com.energyxxer.commodore.functionlogic.commands.playsound.PlaySoundCommand;
import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.textcomponents.TextColor;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.textcomponents.TextComponentContext;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.commodore.types.TypeNotFoundException;
import com.energyxxer.commodore.types.defaults.*;
import com.energyxxer.commodore.util.*;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.ItemTypeHandler;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupPropertiesTask;
import com.energyxxer.enxlex.pattern_matching.PatternEvaluator;
import com.energyxxer.enxlex.pattern_matching.StandardTags;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.util.logger.Debug;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.regex.Matcher;

import static com.energyxxer.trident.compiler.TridentProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;
import static com.energyxxer.trident.sets.BasicLiteralSet.parseQuotedString;
import static com.energyxxer.nbtmapper.tags.PathProtocol.BLOCK_ENTITY;
import static com.energyxxer.nbtmapper.tags.PathProtocol.DEFAULT;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class MinecraftLiteralSet extends PatternProviderSet {
    private static final HashMap<String, String> HUMAN_READABLE_CATEGORY_NAMES = new HashMap<>();

    static {
        HUMAN_READABLE_CATEGORY_NAMES.put(EntityType.CATEGORY, "Entity Type");
        HUMAN_READABLE_CATEGORY_NAMES.put(ItemSlot.CATEGORY, "Item Slot");
        HUMAN_READABLE_CATEGORY_NAMES.put(ScoreDisplayType.CATEGORY, "Objective Display");
    }

    private static final HashSet<String> CATEGORIES_WITH_TAGS = new HashSet<>(Arrays.asList(BlockType.CATEGORY, FluidType.CATEGORY, ItemType.CATEGORY, EntityType.CATEGORY, FunctionReference.CATEGORY));
    private static final HashSet<String> CATEGORIES_WITH_SPECIAL_SUGGESTION_TAGS = new HashSet<>(Arrays.asList(BlockType.CATEGORY, ItemType.CATEGORY, EntityType.CATEGORY));

    private static final HashSet<String> noValidationCategories = new HashSet<>();

    public MinecraftLiteralSet() {
        super(null);
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {

        if(TridentProductions.checkVersionFeature(productions.worker, "custom_dimensions", false)) {
            noValidationCategories.add(DimensionType.CATEGORY);
        }

        if(TridentProductions.checkVersionFeature(productions.worker, "custom_biomes", false)) {
            noValidationCategories.add(BiomeType.CATEGORY);
        }


        TokenPatternMatch RAW_RESOURCE_LOCATION = ofType(RESOURCE_LOCATION).setName("RAW_RESOURCE_LOCATION")
                .setEvaluator((p, d) -> CommonParsers.parseResourceLocation(p.flatten(false), p, (ISymbolContext) d[0]));
        
        productions.getOrCreateStructure("RESOURCE_LOCATION")
                .add(
                        RAW_RESOURCE_LOCATION
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, ResourceLocation.class)
                );

        productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED")
                .add(
                        group(TridentProductions.resourceLocationFixer, optional(TridentProductions.hash().setName("TAG_HEADER"), glue()).addTags(SuggestionTags.ENABLED, TridentSuggestionTags.FUNCTION_TAG).setName("TAG_HEADER_WRAPPER"), ofType(RESOURCE_LOCATION).setName("RAW_RESOURCE_LOCATION")).setName("RAW_RESOURCE_LOCATION_TAGGED")
                        .setEvaluator((p, d) -> CommonParsers.parseResourceLocation(p.flatten(false), p, (ISymbolContext) d[0]))
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, ResourceLocation.class).setName("INTERPOLATION_BLOCK_VALIDATION")
                );


        TokenPatternMatch selectorArgumentBlock = optional(
                glue(),
                TridentProductions.brace("["),
                list(productions.getOrCreateStructure("SELECTOR_ARGUMENT"), TridentProductions.comma()).setOptional().setName("SELECTOR_ARGUMENT_LIST").setEvaluator((p, d) -> {
                    PathContext pathContext = new PathContext().setIsSetting(false).setProtocol(PathProtocol.ENTITY);

                    Selector selector = ((Selector) d[1]);
                    for(TokenPattern<?> rawArgument : ((TokenList) p).getContentsExcludingSeparators()) {
                        Object result = rawArgument.evaluate((ISymbolContext) d[0], pathContext);
                        if(result == null) continue;
                        if(result instanceof SelectorArgument) {
                            result = Collections.singletonList(result);
                        }
                        Collection<SelectorArgument> args = (Collection<SelectorArgument>) result;
                        if(!args.isEmpty()) {
                            selector.addArgumentsMerging(args);
                            for(SelectorArgument arg : args) {
                                if(arg instanceof TypeArgument && !((TypeArgument) arg).isNegated()) {
                                    Type entityType = ((TypeArgument) arg).getType();
                                    if(entityType.isStandalone()) {
                                        pathContext.setProtocolMetadata(entityType);
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }),
                TridentProductions.brace("]")
        ).setName("SELECTOR_ARGUMENT_BLOCK").setSimplificationFunctionFind("SELECTOR_ARGUMENT_LIST");

        productions.getOrCreateStructure("SELECTOR")
                .add(
                        group(
                                ofType(SELECTOR_HEADER).setName("SELECTOR_HEADER"),
                                selectorArgumentBlock
                        ).setEvaluator(
                                (p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    char header = p.find("SELECTOR_HEADER").flatten(false).charAt(1);
                                    Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));

                                    try {
                                        p.findThenEvaluate("SELECTOR_ARGUMENT_BLOCK", null, ctx, selector);
                                    } catch(CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                .invokeThrow();
                                    }

                                    return selector;
                                }
                        )
                );

        TokenPatternMatch entityVariable = group(
                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Entity.class, String.class).setName("INTERPOLATION_BLOCK"),
                selectorArgumentBlock
        ).setName("ENTITY_VARIABLE").setEvaluator(
                (p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Object symbol = p.find("INTERPOLATION_BLOCK").evaluate(ctx);
                    if(symbol instanceof String) {
                        validateIdentifierB((String)symbol, p, ctx);
                    }
                    Entity entity = symbol instanceof Entity ? (Entity) symbol : new PlayerName(((String) symbol));
                    if(p.find("SELECTOR_ARGUMENT_BLOCK") != null) {
                        if(!(entity instanceof Selector)) {
                            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "The entity contained in this variable does not support selector arguments", p, ctx);
                        }

                        Selector copy = ((Selector) entity).clone();

                        p.find("SELECTOR_ARGUMENT_BLOCK").evaluate(ctx, copy);

                        return copy;
                    } else return entity;
                }
        );

        productions.getOrCreateStructure("PLAYER_NAME")
                .add(
                        wrapper(TridentProductions.identifierB(productions), (v, p, d) -> new PlayerName((String)v))
                );

        productions.getOrCreateStructure("ENTITY")
                .add(
                        productions.getOrCreateStructure("PLAYER_NAME")
                )
                .add(
                        productions.getOrCreateStructure("SELECTOR")
                )
                .add(
                        entityVariable
                ).addTags("cspn:Entity");

        productions.getOrCreateStructure("LIMITED_ENTITY")
                .add(
                        wrapper(TridentProductions.identifierBLimited(productions), (v, p, d) -> new PlayerName((String)v))
                )
                .add(
                        productions.getOrCreateStructure("SELECTOR")
                )
                .add(
                        entityVariable
                ).setName("ENTITY").addTags("cspn:Entity");


        productions.getOrCreateStructure("TEXT_COMPONENT")
                .add(
                        group(productions.getOrCreateStructure("JSON_ELEMENT")).setEvaluator((p, d) -> TextParser.jsonToTextComponent((JsonElement)p.find("JSON_ELEMENT").evaluate(d), (ISymbolContext) d[0], p, TextComponentContext.CHAT))
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, TextComponent.class)
                ).addTags("cspn:Text Component");



        //region NBT
        {
            {
                productions.getOrCreateStructure("NBT_VALUE").add(
                        productions.getOrCreateStructure("NBT_COMPOUND")
                                .add(
                                        group(
                                                TridentProductions.brace("{"),
                                                list(
                                                        group(
                                                                wrapper(productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setName("NBT_KEY"),
                                                                TridentProductions.colon(),
                                                                productions.getOrCreateStructure("NBT_VALUE")
                                                        ).setName("NBT_COMPOUND_ENTRY"),
                                                        TridentProductions.comma()
                                                ).setOptional().setName("NBT_COMPOUND_ENTRIES"),
                                                TridentProductions.brace("}")
                                        ).setName("NBT_COMPOUND_GROUP")
                                        .setEvaluator((p, d) -> {
                                            TagCompound compound = new TagCompound();
                                            TokenList entries = (TokenList) p.find("NBT_COMPOUND_ENTRIES");
                                            if (entries != null) {
                                                for (TokenPattern<?> inner : entries.getContentsExcludingSeparators()) {
                                                    String key = (String) inner.find("NBT_KEY").evaluate((ISymbolContext) d[0]);
                                                    NBTTag value = (NBTTag) inner.find("NBT_VALUE").evaluate((ISymbolContext) d[0]);
                                                    value.setName(key);
                                                    compound.add(value);
                                                }
                                            }
                                            return compound;
                                        })
                                )
                );

                productions.getOrCreateStructure("NBT_COMPOUND").add(productions.getOrCreateStructure("INTERPOLATION_BLOCK"));
            }
            productions.getOrCreateStructure("NBT_VALUE").add(
                    productions.getOrCreateStructure("NBT_LIST")
                            .add(
                                    group(
                                            TridentProductions.brace("["),
                                            list(
                                                    productions.getOrCreateStructure("NBT_VALUE"),
                                                    TridentProductions.comma()
                                            ).setOptional().setName("NBT_LIST_ENTRIES"),
                                            TridentProductions.brace("]")
                                    ).setEvaluator((p, d) -> {
                                        TagList list = new TagList();
                                        TokenList entries = (TokenList) p.find("NBT_LIST_ENTRIES");
                                        if (entries != null) {
                                            for (TokenPattern<?> inner : entries.getContentsExcludingSeparators()) {
                                                list.add((NBTTag)inner.evaluate(d));
                                            }
                                        }
                                        return list;
                                    })
                            )
            );
            {
                productions.getOrCreateStructure("NBT_VALUE")
                        .add(
                                group(
                                        TridentProductions.brace("["),
                                        literal("B"),
                                        TridentProductions.symbol(";"),
                                        list(productions.getOrCreateStructure("NBT_VALUE"), TridentProductions.comma()).setOptional().setName("NBT_ARRAY_ENTRIES"),
                                        TridentProductions.brace("]")
                                ).setName("NBT_BYTE_ARRAY").setEvaluator((p, d) -> {
                                    TagByteArray arr = new TagByteArray();
                                    TokenList entries = (TokenList) p.find("NBT_ARRAY_ENTRIES");
                                    if (entries != null) {
                                        for (TokenPattern<?> inner : entries.getContentsExcludingSeparators()) {
                                            NBTTag value = (NBTTag)inner.evaluate(d);
                                            if (value instanceof TagByte) {
                                                arr.add(value);
                                            } else {
                                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected TAG_Byte in TAG_Byte_Array, instead got " + value.getType(), inner, (ISymbolContext) d[0]);
                                            }
                                        }
                                    }
                                    return arr;
                                })
                        );
            }
            {
                productions.getOrCreateStructure("NBT_VALUE")
                        .add(
                                group(
                                        TridentProductions.brace("["),
                                        literal("I"),
                                        TridentProductions.symbol(";"),
                                        list(productions.getOrCreateStructure("NBT_VALUE"), TridentProductions.comma()).setOptional().setName("NBT_ARRAY_ENTRIES"),
                                        TridentProductions.brace("]")
                                ).setName("NBT_INT_ARRAY").setEvaluator((p, d) -> {
                                    TagIntArray arr = new TagIntArray();
                                    TokenList entries = (TokenList) p.find("NBT_ARRAY_ENTRIES");
                                    if (entries != null) {
                                        for (TokenPattern<?> inner : entries.getContentsExcludingSeparators()) {
                                            NBTTag value = (NBTTag)inner.evaluate(d);
                                            if (value instanceof TagInt) {
                                                arr.add(value);
                                            } else {
                                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected TAG_Int in TAG_Int_Array, instead got " + value.getType(), inner, (ISymbolContext) d[0]);
                                            }
                                        }
                                    }
                                    return arr;
                                })
                        );
            }
            {
                productions.getOrCreateStructure("NBT_VALUE")
                        .add(
                                group(
                                        TridentProductions.brace("["),
                                        literal("L"),
                                        TridentProductions.symbol(";"),
                                        list(productions.getOrCreateStructure("NBT_VALUE"), TridentProductions.comma()).setOptional().setName("NBT_ARRAY_ENTRIES"),
                                        TridentProductions.brace("]")
                                ).setName("NBT_LONG_ARRAY").setEvaluator((p, d) -> {
                                    TagLongArray arr = new TagLongArray();
                                    TokenList entries = (TokenList) p.find("NBT_ARRAY_ENTRIES");
                                    if (entries != null) {
                                        for (TokenPattern<?> inner : entries.getContentsExcludingSeparators()) {
                                            NBTTag value = (NBTTag)inner.evaluate(d);
                                            if (value instanceof TagLong) {
                                                arr.add(value);
                                            } else {
                                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Expected TAG_Long in TAG_Long_Array, instead got " + value.getType(), inner, (ISymbolContext) d[0]);
                                            }
                                        }
                                    }
                                    return arr;
                                })
                        );
            }
            productions.getOrCreateStructure("NBT_VALUE")
                    .add(group(TridentProductions.string(productions)).setEvaluator((p, d) -> new TagString((String) ((TokenGroup) p).getContents()[0].evaluate(d))))
                    .add(ofType(IDENTIFIER_TYPE_A).setName("RAW_STRING").setEvaluator((p, d) -> new TagString(p.flatten(false))))
                    .add(ofType(TYPED_NUMBER).setName("NBT_NUMBER").setEvaluator((p, d) -> parseNumericNBTTag(p, (ISymbolContext) d[0])))
                    .add(group(TridentProductions.rawBoolean().setName("BOOLEAN")).setEvaluator((p, d) -> new TagByte(((boolean) ((TokenGroup) p).getContents()[0].evaluate(d)) ? 1 : 0)))
                    .add(PrismarineTypeSystem.validatorGroup(
                            productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                            d -> new Object[] {d[0]},
                            (result, pattern, data) -> {
                                if(result instanceof Item) {
                                    return ItemTypeHandler.getSlotNBT((Item) result);
                                } else if (result instanceof TextComponent || result instanceof String) {
                                    return new TagString(result.toString());
                                }
                                return result;
                            },
                            false, NBTTag.class, Item.class, TextComponent.class, String.class))
                    .addTags("cspn:NBT Value");
        }


        {
            TokenStructureMatch NBT_PATH_NODE = productions.getOrCreateStructure("NBT_PATH_NODE");
            NBT_PATH_NODE.setGreedy(true);

            TokenStructureMatch STRING_LITERAL_OR_IDENTIFIER_D = choice(TridentProductions.string(productions), ofType(IDENTIFIER_TYPE_D).setName("IDENTIFIER_D").setEvaluator((p, d) -> p.flatten(false))).setName("STRING_LITERAL_OR_IDENTIFIER_D");

            NBT_PATH_NODE.add(
                    TridentProductions.dot().addTags(StandardTags.LIST_TERMINATOR).setName("NBT_PATH_TRAILING_DOT").setEvaluator((p, d) -> null)
            );

            PatternEvaluator pathKeyEvaluator = (p, d) -> {
                TagCompound compoundMatch = null; //may be null
                TokenPattern<?> compoundPattern = p.find("NBT_PATH_COMPOUND_MATCH.NBT_COMPOUND");
                if(compoundPattern != null) compoundMatch = (TagCompound) compoundPattern.evaluate(d);
                return new NBTPathKey((String) p.find("NBT_PATH_KEY_LABEL.STRING_LITERAL_OR_IDENTIFIER_D").evaluate(d), compoundMatch);
            };

            NBT_PATH_NODE.add(
                    group(
                            TridentProductions.dot().setName("NBT_PATH_SEPARATOR"),
                            glue(),
                            group(STRING_LITERAL_OR_IDENTIFIER_D).setName("NBT_PATH_KEY_LABEL"),
                            optional(productions.getOrCreateStructure("NBT_COMPOUND")).setName("NBT_PATH_COMPOUND_MATCH")
                    ).setName("NBT_PATH_KEY").setEvaluator(pathKeyEvaluator));

            NBT_PATH_NODE.add(
                    group(
                            TridentProductions.dot().setOptional(),
                            glue(),
                            TridentProductions.brace("["),
                            choice(
                                    group(TridentProductions.integer(productions)).setEvaluator((p, d) -> new NBTPathIndex((int) p.find("INTEGER").evaluate(d))),
                                    group(productions.getOrCreateStructure("NBT_COMPOUND")).setEvaluator((p, d) -> new NBTListMatch((TagCompound) p.find("NBT_COMPOUND").evaluate(d))),
                                    PrismarineTypeSystem.validatorGroup(
                                            productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                            d -> new Object[] {d[0]},
                                            (v, p, d) -> {
                                                    if(v instanceof Integer) {
                                                        return new NBTPathIndex((int) v);
                                                    } else {
                                                        return new NBTListMatch((TagCompound) v);
                                                    }
                                            },
                                            false,
                                            Integer.class,
                                            TagCompound.class
                                    )
                            ).setOptional().setName("NBT_PATH_LIST_CONTENT"),
                            TridentProductions.brace("]")
                    ).setName("NBT_PATH_LIST_ACCESS").setEvaluator((p, d) -> {
                        TokenStructure content = ((TokenStructure) p.find("NBT_PATH_LIST_CONTENT"));
                        if (content == null) return new NBTListMatch();
                        return content.evaluate(d);
                    })
            );

            productions.getOrCreateStructure("NBT_PATH")
                    .add(
                            group(
                                    choice(
                                            group(
                                                    group(STRING_LITERAL_OR_IDENTIFIER_D).setName("NBT_PATH_KEY_LABEL"),
                                                    optional(productions.getOrCreateStructure("NBT_COMPOUND")).setName("NBT_PATH_COMPOUND_MATCH")
                                            ).setName("NBT_PATH_KEY").setEvaluator(pathKeyEvaluator),
                                            group(productions.getOrCreateStructure("NBT_COMPOUND")).setName("NBT_PATH_COMPOUND_ROOT").setEvaluator((p, d) -> new NBTPathCompoundRoot((TagCompound) p.find("NBT_COMPOUND").evaluate(d)))
                                    ).setName("NBT_PATH_ROOT"),
                                    list(NBT_PATH_NODE).setOptional().setName("NBT_PATH_NODE_SEQUENCE")
                            ).setName("NBT_PATH_ROOT_WRAPPER").setEvaluator((p, d) -> {

                                ArrayList<NBTPathNode> nodes = new ArrayList<>();
                                nodes.add((NBTPathNode) p.find("NBT_PATH_ROOT").evaluate(d));

                                TokenList otherNodes = (TokenList) p.find("NBT_PATH_NODE_SEQUENCE");
                                if (otherNodes != null) {
                                    for (TokenPattern<?> rawNode : otherNodes.getContents()) {
                                        NBTPathNode node = (NBTPathNode) rawNode.evaluate(d);
                                        if (node != null) nodes.add(node);
                                    }
                                }

                                return new NBTPath(nodes.toArray(new NBTPathNode[0]));
                            })
                    )
                    .add(
                            PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, NBTPath.class)
                    ).addTags("cspn:NBT Path");
        }

        productions.getOrCreateStructure("DATA_HOLDER")
                .add(
                        group(literal("block"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> new DataHolderBlock((CoordinateSet) p.find("COORDINATE_SET").evaluate((ISymbolContext) d[0])))
                )
                .add(
                        group(literal("entity"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> new DataHolderEntity((Entity) p.find("ENTITY").evaluate((ISymbolContext) d[0])))
                )
                .add(
                        group(literal("storage"), TridentProductions.noToken().addTags("cspn:Storage Location"), productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((p, d) -> {
                            ResourceLocation loc = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate((ISymbolContext)d[0]);
                            return new DataHolderStorage(new StorageTarget(((ISymbolContext) d[0]).get(SetupModuleTask.INSTANCE).getNamespace(loc.namespace), loc.body));
                        })
                ).addTags("cspn:Data Holder");

        //endregion

        productions.getOrCreateStructure("TEXT_COLOR").add(
                choice("black", "dark_blue", "dark_aqua", "dark_green", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "reset")
                        .setEvaluator((p, d) -> TextColor.valueOf(p.flatten(false)))
        ).addTags("cspn:Text Color");

        productions.getOrCreateStructure("SOUND_CHANNEL").add(enumChoice(PlaySoundCommand.Source.class)).addTags(SuggestionTags.ENABLED).addTags("cspn:Sound Channel");


        PatternEvaluator intRangeEvaluator = (p, d) -> {
            Integer min = null;
            Integer max = null;

            TokenPattern<?> minPattern = p.find("MIN");
            TokenPattern<?> maxPattern = p.find("MAX");

            if(minPattern != null) {
                min = (Integer) minPattern.evaluate(d);
            }

            if(maxPattern != null) {
                max = (Integer) maxPattern.evaluate(d);
            }

            return new IntegerRange(min, max);
        };

        productions.getOrCreateStructure("INTEGER_NUMBER_RANGE")
                .add(
                        //exact
                        group(TridentProductions.integer(productions)).setEvaluator((p, d) -> new IntegerRange((int) p.find("INTEGER").evaluate(d)))
                )
                .add(
                        //interpolation block
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> new Object[] {d[0]},
                                (v, p, d) -> {
                                    if(v instanceof Integer) v = new IntegerRange((int)v);
                                    return v;
                                },
                                false,
                                IntegerRange.class,
                                Integer.class
                        )
                )
                .add(
                        //range
                        group(
                                TridentProductions.integer(productions).setName("MIN"),
                                glue(),
                                TridentProductions.dot(), glue(), TridentProductions.dot(),
                                optional(glue(), TridentProductions.integer(productions).setName("MAX")).setName("MAX").setSimplificationFunctionContentIndex(1)
                        ).setEvaluator(intRangeEvaluator)
                )
                .add(
                        //range
                        group(
                                TridentProductions.dot(), glue(), TridentProductions.dot(),
                                TridentProductions.integer(productions).setName("MAX")
                        ).setEvaluator(intRangeEvaluator)
                ).addTags("cspn:Integer Range");





        PatternEvaluator realRangeEvaluator = (p, d) -> {
            Double min = null;
            Double max = null;

            TokenPattern<?> minPattern = p.find("MIN");
            TokenPattern<?> maxPattern = p.find("MAX");

            if(minPattern != null) {
                min = (Double) minPattern.evaluate(d);
            }

            if(maxPattern != null) {
                max = (Double) maxPattern.evaluate(d);
            }

            return new DoubleRange(min, max);
        };

        productions.getOrCreateStructure("REAL_NUMBER_RANGE")
                .add(
                        //exact
                        group(real(productions)).setEvaluator((p, d) -> new DoubleRange((double) p.find("REAL").evaluate(d)))
                )
                .add(
                        //interpolation block
                        PrismarineTypeSystem.validatorGroup(
                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                d -> new Object[] {d[0]},
                                (v, p, d) -> {
                                    if(v instanceof Double) v = new DoubleRange((double)v);
                                    return v;
                                },
                                false,
                                DoubleRange.class,
                                Double.class
                        )
                )
                .add(
                        //range
                        group(
                                real(productions).setName("MIN"),
                                glue(),
                                TridentProductions.dot(), glue(), TridentProductions.dot(),
                                optional(glue(), real(productions).setName("MAX")).setName("MAX").setSimplificationFunctionContentIndex(1)
                        ).setEvaluator(realRangeEvaluator)
                )
                .add(
                        //range
                        group(
                                TridentProductions.dot(), glue(), TridentProductions.dot(),
                                real(productions).setName("MAX")
                        ).setEvaluator(realRangeEvaluator)
                ).addTags("cspn:Real Range");



        //region Coordinates


        TokenPatternMatch LOCAL_COORDINATE = productions.getOrCreateStructure("LOCAL_COORDINATE").add(group(caret(), optional(glue(), ofType(SHORT_REAL_NUMBER)).setName("COORDINATE_MAGNITUDE")).setEvaluator(
                (p, d) -> {
                    double magnitude = 0;
                    TokenPattern<?> magnitudePattern = p.find("COORDINATE_MAGNITUDE");
                    if(magnitudePattern != null) {
                        magnitude = Double.parseDouble(magnitudePattern.flatten(false));
                    }
                    return new Coordinate(Coordinate.Type.LOCAL, magnitude);
                }
        )).add(
                group(caret(), glue(), real(productions)).setEvaluator((p, d) -> new Coordinate(Coordinate.Type.LOCAL, (double) p.find("REAL").evaluate((ISymbolContext) d[0])))
        );

        TokenPatternMatch ABSOLUTE_COORDINATE = productions.getOrCreateStructure("ABSOLUTE_COORDINATE").add(group(optional(symbol("*"), glue()), ofType(SHORT_REAL_NUMBER).setEvaluator(
                (p, d) -> {
                    Axis axis = (Axis) d[1];
                    String magnitudeString = p.flatten(false);
                    double magnitude = Double.parseDouble(magnitudeString);
                    if(axis != Axis.Y && !magnitudeString.contains(".")) magnitude += 0.5;
                    return new Coordinate(Coordinate.Type.ABSOLUTE, magnitude);
                }
        )).setSimplificationFunction(d -> {
            TokenPattern<?>[] contents = ((TokenGroup) d.pattern).getContents();
            d.pattern = contents[contents.length-1];
        })).add(group(symbol("*"), glue(), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Integer.class, Double.class)).setEvaluator(
                (p, d) -> {
                    Axis axis = (Axis) d[1];
                    Number magnitude = (Number) ((TokenGroup) p).getContents()[2].evaluate((ISymbolContext) d[0]);
                    double realMagnitude = magnitude.doubleValue();
                    if(axis != Axis.Y && magnitude instanceof Integer) {
                        realMagnitude = magnitude.doubleValue() + 0.5;
                    }
                    return new Coordinate(Coordinate.Type.ABSOLUTE, realMagnitude);
                }
        ));

        TokenPatternMatch RELATIVE_COORDINATE = productions.getOrCreateStructure("RELATIVE_COORDINATE").add(group(TridentProductions.tilde(), optional(glue(), ofType(SHORT_REAL_NUMBER)).setName("COORDINATE_MAGNITUDE")).setEvaluator(
                (p, d) -> {
                    double magnitude = 0;
                    TokenPattern<?> magnitudePattern = p.find("COORDINATE_MAGNITUDE");
                    if(magnitudePattern != null) {
                        magnitude = Double.parseDouble(magnitudePattern.flatten(false));
                    }
                    return new Coordinate(Coordinate.Type.RELATIVE, magnitude);
                }
        )).add(
                group(tilde(), glue(), real(productions)).setEvaluator((p, d) -> new Coordinate(Coordinate.Type.RELATIVE, (double) p.find("REAL").evaluate((ISymbolContext) d[0])))
        );

        TokenPatternMatch MIXABLE_COORDINATE = productions.getOrCreateStructure("MIXABLE_COORDINATE")
                .add(ABSOLUTE_COORDINATE)
                .add(RELATIVE_COORDINATE);


        PatternEvaluator coordinateEvaluator = (p, d) -> {
            TokenPattern<?>[] parts = ((TokenGroup) p).getContents();
            if(parts.length == 3) {
                return new CoordinateSet(
                        (Coordinate) parts[0].evaluate(d[0], Axis.X),
                        (Coordinate) parts[1].evaluate(d[0], Axis.Y),
                        (Coordinate) parts[2].evaluate(d[0], Axis.Z)
                );
            } else if(parts.length == 2) {
                return new CoordinateSet(
                        (Coordinate) parts[0].evaluate(d[0], Axis.X),
                        new Coordinate(Coordinate.Type.RELATIVE, 0),
                        (Coordinate) parts[1].evaluate(d[0], Axis.Z)
                );
            } else {
                throw new IllegalStateException("(Coordinate Evaluator) parts.length =" + parts.length);
            }
        };


        productions.getOrCreateStructure("COORDINATE_SET")
                .add(
                        group(
                                MIXABLE_COORDINATE,
                                MIXABLE_COORDINATE,
                                MIXABLE_COORDINATE
                        ).setName("MIXABLE_COORDINATE_SET").setEvaluator(coordinateEvaluator)
                )
                .add(
                        group(
                                LOCAL_COORDINATE,
                                LOCAL_COORDINATE,
                                LOCAL_COORDINATE
                        ).setName("LOCAL_COORDINATE_SET").setEvaluator(coordinateEvaluator)
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, CoordinateSet.class)
                )
                .addTags("cspn:Position");

        productions.getOrCreateStructure("TWO_COORDINATE_SET")
                .add(
                        group(
                                MIXABLE_COORDINATE,
                                MIXABLE_COORDINATE
                        ).setName("MIXABLE_TWO_COORDINATE_SET").setEvaluator(coordinateEvaluator)
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, CoordinateSet.class)
                )
                .addTags("cspn:XZ Position");


        PatternEvaluator rotationEvaluator = (p, d) -> {
            TokenPattern<?>[] parts = ((TokenGroup) p).getContents();
            return new Rotation(
                    (RotationUnit) parts[0].evaluate(d[0]),
                    (RotationUnit) parts[1].evaluate(d[0])
            );
        };

        TokenPatternMatch ROTATION_UNIT = productions.getOrCreateStructure("ROTATION_UNIT")
                .add(
                        ofType(SHORT_REAL_NUMBER).setEvaluator(
                                (p, d) -> new RotationUnit(RotationUnit.Type.ABSOLUTE, Double.parseDouble(p.flatten(false)))
                        )
                )
                .add(
                        group(TridentProductions.tilde(), optional(glue(), ofType(SHORT_REAL_NUMBER)).setName("ROTATION_MAGNITUDE")).setEvaluator(
                                (p, d) -> {
                                    double magnitude = 0;
                                    TokenPattern<?> magnitudePattern = p.find("ROTATION_MAGNITUDE");
                                    if(magnitudePattern != null) {
                                        magnitude = Double.parseDouble(magnitudePattern.flatten(false));
                                    }
                                    return new RotationUnit(RotationUnit.Type.RELATIVE, magnitude);
                                }
                        )
                );


        productions.getOrCreateStructure("ROTATION")
                .add(
                        group(
                                ROTATION_UNIT,
                                ROTATION_UNIT
                        ).setEvaluator(rotationEvaluator)
                )
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Rotation.class)
                )
                .addTags("cspn:Pitch-Yaw Rotation");

        //endregion


        //region types


        productions.getOrCreateStructure("COLOR").add(
                group(
                        real(productions).setName("RED_COMPONENT").addTags("cspn:Red Component (0..1)"),
                        real(productions).setName("GREEN_COMPONENT").addTags("cspn:Green Component (0..1)"),
                        real(productions).setName("BLUE_COMPONENT").addTags("cspn:Blue Component (0..1)")
                ).setName("COLOR").addTags("cspn:RGB Color").setEvaluator((p, d) -> new ParticleColor(
                        (double) p.find("RED_COMPONENT").evaluate(d),
                        (double) p.find("GREEN_COMPONENT").evaluate(d),
                        (double) p.find("BLUE_COMPONENT").evaluate(d)
                ))
        );

        HashMap<String, TokenStructureMatch> categoryMap = new HashMap<>();

        for(Namespace namespace : productions.worker.output.get(SetupModuleTask.INSTANCE).getAllNamespaces()) {
            TokenGroupMatch namespaceMatch = (TokenGroupMatch) group(literal(namespace.getName()), TridentProductions.colon()).setOptional(namespace.getName().equals("minecraft")).setName("NAMESPACE");
            for(TypeDictionary typeDict : namespace.types.getAllDictionaries()) {
                String category = typeDict.getCategory();
                TokenStructureMatch categoryStructure;
                if(!categoryMap.containsKey(category)) {
                    categoryMap.put(category, categoryStructure = productions.getOrCreateStructure(category.toUpperCase(Locale.ENGLISH) + "_ID"));
                    categoryStructure.add(PrismarineTypeSystem.validatorGroup(
                            productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                            d -> new Object[] {d[0]},
                            (v, p, d) -> parseType(v, p, (ISymbolContext) d[0], category, d.length > 1 && (boolean) d[1]),
                            false,
                            ResourceLocation.class,
                            String.class
                    ));
                    categoryStructure.add(ofType(STRING_LITERAL).setEvaluator((p, d) -> parseType(parseQuotedString(p.flatten(false), p, (ISymbolContext) d[0]), p, (ISymbolContext) d[0], category, d.length > 1 && (boolean) d[1])));

                    if(noValidationCategories.contains(category)) {
                        categoryStructure.add(PrismarineTypeSystem.validatorGroup(
                                RAW_RESOURCE_LOCATION,
                                d -> new Object[] {d[0]},
                                (v, p, d) -> parseType(v, p, (ISymbolContext) d[0], category, false),
                                false,
                                ResourceLocation.class
                        ));
                    }

                    categoryStructure.addTags("cspn:" + getHumanReadableCategoryName(category));

                    if(CATEGORIES_WITH_SPECIAL_SUGGESTION_TAGS.contains(category)) {
                        categoryStructure.addTags(SuggestionTags.ENABLED, TridentSuggestionTags.__TYPE_TEMPLATE + category);
                    }

                    if(CATEGORIES_WITH_TAGS.contains(category)) {
                        productions.getOrCreateStructure(category.toUpperCase(Locale.ENGLISH) + "_ID_TAGGED")
                                .add(group(categoryStructure).setEvaluator((p, d) -> ((TokenGroup) p).getContents()[0].evaluate(d[0], true)))
                                .add(
                                        group(
                                                TridentProductions.resourceLocationFixer,
                                                TridentProductions.hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.__TAG_TEMPLATE + category),
                                                glue(),
                                                RAW_RESOURCE_LOCATION
                                        ).setEvaluator((p, d) -> {
                                            ResourceLocation loc = (ResourceLocation) p.find("RAW_RESOURCE_LOCATION").evaluate(d);
                                            loc.isTag = true;
                                            return parseType(loc, p, (ISymbolContext) d[0], category, true);
                                        })
                                );
                    }
                }

                categoryStructure = categoryMap.get(category);

                TokenStructureMatch typeNameStructure = struct("TYPE_NAME");
                boolean any = false;
                boolean usesNamespace = false;
                for(Type type : typeDict.list()) {
                    String name = type.getName();
                    if(type instanceof AliasType) {
                        name = ((AliasType)type).getAliasName();
                    }
                    TokenPatternMatch typeNameMatch = TridentProductions.identifierA(name).addTags(SuggestionTags.LITERAL_SORT, CATEGORIES_WITH_SPECIAL_SUGGESTION_TAGS.contains(category) ? SuggestionTags.DISABLED : SuggestionTags.ENABLED);
                    if(Objects.equals(category, ParticleType.CATEGORY)) {
                        processParticleType(productions, type, typeNameMatch, namespaceMatch);
                    }
                    if(Objects.equals(category, GameruleType.CATEGORY)) {
                        processGameruleType(productions, type, typeNameMatch, namespaceMatch);
                    }
                    typeNameStructure.add(typeNameMatch);
                    any = true;
                    usesNamespace = type.useNamespace();
                }
                if(any) {
                    categoryStructure.add((usesNamespace ? group(TridentProductions.resourceLocationFixer, namespaceMatch, typeNameStructure) : group(typeNameStructure)).setName(category.toUpperCase() + "_ID_DEFAULT").setEvaluator((p, d) -> parseType(p.flatten(false), p, (ISymbolContext) d[0], category, false)));
                }
            }
        }


        //endregion




        productions.getOrCreateStructure("BLOCKSTATE").add(
                group(
                        TridentProductions.brace("["),
                        list(
                                group(
                                        wrapper(TridentProductions.identifierA(productions)).setName("BLOCKSTATE_PROPERTY_KEY").addTags("cspn:Blockstate Key"),
                                        TridentProductions.equals(),
                                        choice(
                                                TridentProductions.identifierA(productions),
                                                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), data -> new Object[] {(ISymbolContext) data[0]}, (v, p, d) -> v.toString(), false, String.class, Integer.class, Boolean.class)
                                        ).setName("BLOCKSTATE_PROPERTY_VALUE").addTags("cspn:Blockstate Value")
                                ).setName("BLOCKSTATE_PROPERTY"),
                                TridentProductions.comma()
                        ).setOptional().setName("BLOCKSTATE_LIST"),
                        TridentProductions.brace("]")
                ).setEvaluator((p, d) -> {
                    TokenList blockstateList = (TokenList) p.find("BLOCKSTATE_LIST");
                    if(blockstateList == null) return null;
                    Blockstate properties = new Blockstate();
                    for(TokenPattern<?> rawProperty : blockstateList.getContentsExcludingSeparators()) {
                        String key = (String) rawProperty.find("BLOCKSTATE_PROPERTY_KEY").evaluate(d[0]);
                        String value = (String) rawProperty.find("BLOCKSTATE_PROPERTY_VALUE").evaluate(d[0]);
                        properties.put(key, value);
                    }
                    return properties;
                })
        );

        productions.getOrCreateStructure("BLOCK")
                .add(
                        group(
                                TridentProductions.resourceLocationFixer,
                                productions.getOrCreateStructure("BLOCK_ID"),
                                optional(glue(), productions.getOrCreateStructure("BLOCKSTATE")).setSimplificationFunctionContentIndex(1).setName("APPENDED_BLOCKSTATE"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            Type blockId = (Type) p.find("BLOCK_ID").evaluate(d[0], d.length > 1 && (boolean) d[1]);
                            return evaluateBlock(blockId, p, p.find("BLOCK_ID"), d);
                        })
                )
                .add(
                        group(
                                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Block.class, ResourceLocation.class, String.class).setName("INTERPOLATION_BLOCK"),
                                optional(glue(), productions.getOrCreateStructure("BLOCKSTATE")).setSimplificationFunctionContentIndex(1).setName("APPENDED_BLOCKSTATE"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            Object inBlock = p.find("INTERPOLATION_BLOCK").evaluate(d[0]);
                            return evaluateBlock(inBlock, p, p.find("INTERPOLATION_BLOCK"), d);
                        })
                ).addTags("cspn:Block");

        productions.getOrCreateStructure("BLOCK_TAGGED")
                .add(
                        group(productions.getOrCreateStructure("BLOCK")).setSimplificationFunction(d -> {
                            d.pattern = ((TokenGroup) d.pattern).getContents()[0];
                            d.data = new Object[] {(ISymbolContext) d.data[0], true};
                        })
                )
                .add(
                        group(
                                TridentProductions.resourceLocationFixer,
                                TridentProductions.hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.BLOCK_TAG),
                                glue(),
                                RAW_RESOURCE_LOCATION,
                                optional(glue(), productions.getOrCreateStructure("BLOCKSTATE")).setSimplificationFunctionContentIndex(1).setName("APPENDED_BLOCKSTATE"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            d = new Object[] {(ISymbolContext) d[0], true};
                            ResourceLocation loc = (ResourceLocation) p.find("RAW_RESOURCE_LOCATION").evaluate(d);
                            loc.isTag = true;
                            Type blockType = parseType(loc, p, (ISymbolContext) d[0], BlockType.CATEGORY, true);
                            return evaluateBlock(blockType, p, p.find("RAW_RESOURCE_LOCATION"), d);
                        })
                );





        productions.getOrCreateStructure("ITEM")
                .add(
                        group(
                                TridentProductions.resourceLocationFixer,
                                productions.getOrCreateStructure("ITEM_ID"),
                                optional(glue(), TridentProductions.hash(), TridentProductions.integer(productions).addTags("cspn:Model Index")).setSimplificationFunctionContentIndex(2).setName("APPENDED_MODEL_DATA"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            Type itemId = (Type) p.find("ITEM_ID").evaluate((ISymbolContext) d[0], d.length > 2 && (boolean) d[2]);
                            return evaluateItem(itemId, p, p.find("ITEM_ID"), d);
                        })
                )
                .add(
                        group(
                                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, Item.class, CustomItem.class, ResourceLocation.class, String.class).setName("INTERPOLATION_BLOCK"),
                                optional(glue(), TridentProductions.hash(), TridentProductions.integer(productions).addTags("cspn:Model Index")).setSimplificationFunctionContentIndex(2).setName("APPENDED_MODEL_DATA"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            Object inBlock = p.find("INTERPOLATION_BLOCK").evaluate(d[0]);
                            return evaluateItem(inBlock, p, p.find("INTERPOLATION_BLOCK"), d);
                        })
                ).addTags("cspn:Item");

        productions.getOrCreateStructure("ITEM_TAGGED")
                .add(
                        group(productions.getOrCreateStructure("ITEM")).setSimplificationFunction(d -> {
                            d.pattern = ((TokenGroup) d.pattern).getContents()[0];
                            d.data = new Object[] {(ISymbolContext) d.data[0], (NBTMode) d.data[1], true};
                        })
                )
                .add(
                        group(
                                TridentProductions.resourceLocationFixer,
                                TridentProductions.hash().setName("TAG_HEADER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.ITEM_TAG),
                                glue(),
                                RAW_RESOURCE_LOCATION,
                                optional(glue(), TridentProductions.hash(), TridentProductions.integer(productions).addTags("cspn:Model Index")).setSimplificationFunctionContentIndex(2).setName("APPENDED_MODEL_DATA"),
                                optional(glue(), productions.getOrCreateStructure("NBT_COMPOUND")).setSimplificationFunctionContentIndex(1).setName("APPENDED_NBT")
                        ).setEvaluator((p, d) -> {
                            d = new Object[] {(ISymbolContext) d[0], (NBTMode) d[1], true};
                            ResourceLocation loc = (ResourceLocation) p.find("RAW_RESOURCE_LOCATION").evaluate(d);
                            loc.isTag = true;
                            Type itemType = parseType(loc, p, (ISymbolContext) d[0], ItemType.CATEGORY, true);
                            return evaluateItem(itemType, p, p.find("RAW_RESOURCE_LOCATION"), d);
                        })
                );



        productions.getOrCreateStructure("UUID")
                .add(ofType(TridentTokens.UUID).setName("RAW_UUID").setEvaluator((p, d) -> {
                    try {
                        return java.util.UUID.fromString(p.flatten(false));
                    } catch(NumberFormatException x) {
                        throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Invalid UUID: " + p.flatten(false), p, (ISymbolContext) d[0]);
                    }
                }))
                .add(
                        PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), false, UUID.class)
                )
                .addTags("cspn:UUID");


        productions.getOrCreateStructure("NUMERIC_NBT_TYPE")
                .add(literal("byte").setEvaluator((p, d) -> NumericNBTType.BYTE))
                .add(literal("short").setEvaluator((p, d) -> NumericNBTType.SHORT))
                .add(literal("int").setEvaluator((p, d) -> NumericNBTType.INT))
                .add(literal("float").setEvaluator((p, d) -> NumericNBTType.FLOAT))
                .add(literal("long").setEvaluator((p, d) -> NumericNBTType.LONG))
                .add(literal("double").setEvaluator((p, d) -> NumericNBTType.DOUBLE)).addTags("cspn:Numeric NBT Type");


        productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")
                .add(
                        TridentProductions.identifierA(productions)
                )
                .add(
                        TridentProductions.string(productions)
                );

        productions.getOrCreateStructure("TIME").add(
                ofType(TIME).addTags("cspn:Time").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    try {
                        String raw = p.flatten(false);
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
                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                .invokeThrow();
                        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", p, ctx);
                    }
                })
        );

        productions.getOrCreateStructure("OBJECTIVE_NAME").add(
                group(TridentProductions.identifierA(productions)).setName("OBJECTIVE_NAME").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.OBJECTIVE_EXISTING).addTags("cspn:Objective").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    String objectiveName = (String) ((TokenGroup) p).getContents()[0].evaluate(ctx);

                    Class expectedClass = (Class) d[1];
                    if(expectedClass == String.class) {
                        return objectiveName;
                    } else if(expectedClass == Objective.class) {
                        if(!ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().exists(objectiveName)) {
                            ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Undefined objective name '" + objectiveName + "'", p));
                            return ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().create(objectiveName);
                        } else {
                            return ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().getOrCreate(objectiveName);
                        }
                    } else {
                        throw new IllegalArgumentException("Don't know how to turn an objective name into an instance of " + expectedClass);
                    }
                })
        );

        productions.getOrCreateStructure("SCORE").add(
                group(
                        productions.getOrCreateStructure("ENTITY"),
                        productions.getOrCreateStructure("OBJECTIVE_NAME")
                ).setName("EXPLICIT_SCORE").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];

                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                    Objective objective = (Objective) p.find("OBJECTIVE_NAME").evaluate(ctx, Objective.class);
                    return new LocalScore(entity, objective);
                })
        ).addTags("cspn:Score");
        productions.getOrCreateStructure("SCORE_OPTIONAL_OBJECTIVE").add(
                group(
                        choice(TridentProductions.symbol("*").setEvaluator((p, d) -> new PlayerName("*")), productions.getOrCreateStructure("ENTITY")).setName("ENTITY"),
                        optional(TridentProductions.sameLine(), choice(TridentProductions.symbol("*").setEvaluator((p, d) -> null), productions.getOrCreateStructure("OBJECTIVE_NAME"))).setSimplificationFunctionContentIndex(1).setName("OBJECTIVE_NAME")
                ).setName("EXPLICIT_SCORE").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];

                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                    Objective objective = (Objective) p.findThenEvaluate("OBJECTIVE_NAME", null, ctx, Objective.class);

                    return new LocalScore(entity, objective);
                })
        ).addTags("cspn:Score");

        productions.getOrCreateStructure("ANCHOR").add(enumChoice(EntityAnchor.class));
    }

    private Block evaluateBlock(Object obj, TokenPattern<?> p, TokenPattern<?> root, Object[] d) {
        boolean allowTags = d.length > 1 && (boolean) d[1];

        Block block;
        if(obj instanceof Block) {
            if(!allowTags && !((Block) obj).getBlockType().isStandalone()) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Block tags aren't allowed in this context", root, (ISymbolContext) d[0]);
            }

            Blockstate clonedBlockstate = ((Block) obj).getBlockstate();
            if(clonedBlockstate != null) clonedBlockstate = clonedBlockstate.clone();

            TagCompound clonedNBT = ((Block) obj).getNBT();
            if(clonedNBT != null) clonedNBT = clonedNBT.clone();
            block = new Block(((Block) obj).getBlockType(), clonedBlockstate, clonedNBT);
        } else if(obj instanceof Type) {
            if(!allowTags && !((Type) obj).isStandalone()) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Block tags aren't allowed in this context", root, (ISymbolContext) d[0]);
            }
            block = new Block((Type)obj);
        } else {
            block = new Block(parseType(obj, root, (ISymbolContext) d[0], BlockType.CATEGORY, allowTags));
        }

        Blockstate appendedBlockstate = (Blockstate) p.findThenEvaluate("APPENDED_BLOCKSTATE", null, d[0]);
        TagCompound appendedNBT = (TagCompound) p.findThenEvaluate("APPENDED_NBT", null, d[0]);

        if(appendedBlockstate != null) {
            Blockstate blockstate = block.getBlockstate();
            if(blockstate == null) blockstate = new Blockstate();
            blockstate = blockstate.merge(appendedBlockstate);
            block.setBlockstate(blockstate);
        }

        if(appendedNBT != null) {
            TagCompound nbt = block.getNBT();
            if(nbt == null) nbt = new TagCompound();
            nbt = nbt.merge(appendedNBT);
            PathContext context = new PathContext().setIsSetting(true).setProtocol(BLOCK_ENTITY);
            NBTInspector.inspectTag(nbt, context, p.find("APPENDED_NBT"), (ISymbolContext) d[0]);
            block.setNbt(nbt);
        }

        return block;
    }

    private Item evaluateItem(Object obj, TokenPattern<?> p, TokenPattern<?> root, Object[] d) {
        boolean allowTags = d.length > 2 && (boolean) d[2];
        NBTMode mode = (NBTMode) d[1];

        Item item;
        if(obj instanceof Item) {
            if(!allowTags && !((Item) obj).getItemType().isStandalone()) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Item tags aren't allowed in this context", root, (ISymbolContext) d[0]);
            }

            TagCompound clonedNBT = ((Item) obj).getNBT();
            if(clonedNBT != null) clonedNBT = clonedNBT.clone();
            item = new Item(((Item) obj).getItemType(), clonedNBT);
        } else if(obj instanceof CustomItem) {
            item = ((CustomItem) obj).constructItem(mode);
        } else if(obj instanceof Type) {
            if(!allowTags && !((Type) obj).isStandalone()) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Item tags aren't allowed in this context", root, (ISymbolContext) d[0]);
            }
            item = new Item((Type)obj);
        } else {
            item = new Item(parseType(obj, root, (ISymbolContext) d[0], ItemType.CATEGORY, allowTags));
        }

        Integer appendedModelData = (Integer) p.findThenEvaluate("APPENDED_MODEL_DATA", null, d[0]);
        TagCompound appendedNBT = (TagCompound) p.findThenEvaluate("APPENDED_NBT", null, d[0]);

        if(appendedModelData != null) {
            TagCompound nbt = item.getNBT();
            if(nbt == null) nbt = new TagCompound();
            nbt = nbt.merge(new TagCompound(new TagInt("CustomModelData", appendedModelData)));
            item.setNbt(nbt);
        }
        if(appendedNBT != null) {
            TagCompound nbt = item.getNBT();
            if(nbt == null) nbt = new TagCompound();
            nbt = nbt.merge(appendedNBT);
            PathContext context = new PathContext().setIsSetting(mode == NBTMode.SETTING).setProtocol(DEFAULT, "ITEM_TAG");
            NBTInspector.inspectTag(nbt, context, p.find("APPENDED_NBT"), (ISymbolContext) d[0]);
            item.setNbt(nbt);
        }

        return item;
    }

    public static Type parseType(Object obj, TokenPattern<?> pattern, ISymbolContext ctx, String category, boolean allowTags) {
        String str;
        ResourceLocation loc;
        if(obj instanceof String) {
            str = (String) obj;
            loc = new ResourceLocation(str);
        } else {
            loc = ((ResourceLocation) obj);
            str = loc.toString();
        }
        if(!loc.isTag) {
            try {
                TypeDictionary dict = ctx.get(SetupModuleTask.INSTANCE).getTypeManager(loc.namespace).getDictionary(category);
                return noValidationCategories.contains(category) ? dict.getOrCreate(loc.body) : dict.get(loc.body);
            } catch(TypeNotFoundException x) {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "No such type '" + str + "' for category '" + category + "'", pattern, ctx);
            }
        } else if(allowTags) {
            TagGroup group = ctx.get(SetupModuleTask.INSTANCE).getTagManager(loc.namespace).getGroup(category);
            if(group != null) {
                Tag tag = group.get(loc.body);
                if(tag != null) {
                    return tag;
                } else {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "No such tag '" + loc + "' for category '" + category + "'", pattern, ctx);
                }
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Type category '" + category + "' does not support tags or has none: '" + loc + "'", pattern, ctx);
            }
        } else {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, getHumanReadableCategoryName(category) + " tags aren't allowed in this context", pattern, ctx);
        }
    }

    private static void processParticleType(PrismarineProductions productions, Type type, TokenPatternMatch nameMatch, TokenGroupMatch namespaceMatch) {
        TokenGroupMatch g = group();
        g.append((type.useNamespace() ? group(TridentProductions.resourceLocationFixer, namespaceMatch, nameMatch) : nameMatch).setName("PARTICLE_ID_DEFAULT").setEvaluator((p, d) -> parseType(p.flatten(false), p, (ISymbolContext) d[0], ParticleType.CATEGORY, false)));

        TokenGroupMatch argsGroupMatch = group().setName("PARTICLE_ARGUMENTS");

        g.append(argsGroupMatch);

        String allArgs = type.getProperty("argument");
        if (allArgs != null && !allArgs.equals("none")) {
            String[] args = allArgs.split("-");
            for (String arg : args) {
                switch (arg) {
                    case "boolean": {
                        argsGroupMatch.append(TridentProductions.rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Boolean"));
                        break;
                    }
                    case "int": {
                        argsGroupMatch.append(TridentProductions.integer(productions).addTags("cspn:Integer"));
                        break;
                    }
                    case "double": {
                        argsGroupMatch.append(real(productions).addTags("cspn:Real"));
                        break;
                    }
                    case "color": {
                        argsGroupMatch.append(productions.getOrCreateStructure("COLOR"));
                        break;
                    }
                    case "block": {
                        argsGroupMatch.append(productions.getOrCreateStructure("BLOCK"));
                        break;
                    }
                    case "item": {
                        argsGroupMatch.append(group(productions.getOrCreateStructure("ITEM")).setSimplificationFunction(d -> {
                            d.data = new Object[] {(ISymbolContext) d.data[0], NBTMode.SETTING};
                            d.pattern = d.pattern.find("ITEM");
                        }));
                        break;
                    }
                    default: {
                        Debug.log("Invalid particle argument type '" + arg + "', could not be added to particle production", Debug.MessageType.ERROR);
                    }
                }
            }
        }

        g.setEvaluator((p, d) -> {
            Type particleType = (Type) p.find("PARTICLE_ID_DEFAULT").evaluate(d[0]);
            TokenGroup argsGroup = (TokenGroup) p.find("PARTICLE_ARGUMENTS");
            Object[] particleArgs;
            if(argsGroup != null) {
                TokenPattern<?>[] argsGroupContents = argsGroup.getContents();
                particleArgs = new Object[argsGroupContents.length];
                int i = 0;
                for(TokenPattern<?> arg : argsGroupContents) {
                    particleArgs[i] = arg.evaluate(d[0]);
                    i++;
                }
            } else {
                particleArgs = new Object[0];
            }
            return new Particle(particleType, particleArgs);
        });

        productions.getOrCreateStructure("PARTICLE").add(g);
    }

    private static void processGameruleType(PrismarineProductions productions, Type type, TokenPatternMatch nameMatch, TokenGroupMatch namespaceMatch) {
        TokenGroupMatch g = group();
        g.append((type.useNamespace() ? group(TridentProductions.resourceLocationFixer, namespaceMatch, nameMatch) : nameMatch).setName("GAMERULE_ID_DEFAULT").setEvaluator((p, d) -> parseType(p.flatten(false), p, (ISymbolContext) d[0], GameruleType.CATEGORY, false)));

        g.append(TridentProductions.sameLine());

        String arg = type.getProperty("argument");
        switch (arg) {
            case "boolean": {
                g.append(TridentProductions.rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Boolean"));
                break;
            }
            case "int": {
                g.append(TridentProductions.integer(productions).addTags("cspn:Integer"));
                break;
            }
            case "double": {
                g.append(real(productions).addTags("cspn:Real"));
                break;
            }
            case "color": {
                g.append(productions.getOrCreateStructure("COLOR"));
                break;
            }
            case "block": {
                g.append(productions.getOrCreateStructure("BLOCK"));
                break;
            }
            case "item": {
                g.append(productions.getOrCreateStructure("ITEM"));
                break;
            }
            default: {
                Debug.log("Invalid particle argument type '" + arg + "', could not be added to gamerule setter production", Debug.MessageType.ERROR);
            }
        }

        g.setEvaluator((p, d) -> {
            Type gameruleType = (Type) p.find("GAMERULE_ID_DEFAULT").evaluate(d[0]);
            return new GameruleSetCommand(gameruleType, ((TokenGroup) p).getContents()[2].evaluate(d[0]));
        });

        productions.getOrCreateStructure("GAMERULE_SETTER").add(g);
    }

    private static String getHumanReadableCategoryName(String category) {
        String name = HUMAN_READABLE_CATEGORY_NAMES.get(category);
        if(name == null) {
            HUMAN_READABLE_CATEGORY_NAMES.put(category, name = getHumanReadableName(category));
        }
        return name;
    }

    private static String getHumanReadableName(String str) {
        StringBuilder sb = new StringBuilder();
        boolean firstOfWord = true;
        for(char c : str.toCharArray()) {
            if(c == '_' || Character.isWhitespace(c)) {
                sb.append(' ');
                firstOfWord = true;
            } else if(Character.isLetter(c) && firstOfWord) {
                sb.append(Character.toUpperCase(c));
                firstOfWord = false;
            } else {
                sb.append(c);
                firstOfWord = false;
            }
        }
        return sb.toString();
    }

    public static NBTTag parseNumericNBTTag(TokenPattern<?> pattern, ISymbolContext ctx) {
        String flat = pattern.flattenTokens(new ArrayList<>()).get(0).value;

        Matcher matcher = TridentLexerProfile.NUMBER_REGEX.matcher(flat);
        matcher.lookingAt(); //must be true

        String numberPart = matcher.group(1);
        boolean expectedReal = numberPart.contains(".") || numberPart.contains("e") || numberPart.contains("E");
        try {
            switch (matcher.group(3).toLowerCase()) {
                case "": {
                    return expectedReal ?
                            new TagDouble(Double.parseDouble(numberPart)) :
                            new TagInt(Integer.parseInt(numberPart));
                }
                case "b": {
                    return new TagByte(Integer.parseInt(numberPart));
                }
                case "d": {
                    return new TagDouble(Double.parseDouble(numberPart));
                }
                case "f": {
                    return new TagFloat(Float.parseFloat(numberPart));
                }
                case "s": {
                    return new TagShort(Short.parseShort(numberPart));
                }
                case "l": {
                    return new TagLong(Long.parseLong(numberPart));
                }
                default: {
                    throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
                }
            }
        } catch (NumberFormatException x) {
            NumericNBTType expectedType = matcher.group(3).length() == 0 && expectedReal ? NumericNBTType.DOUBLE : NumericNBTType.getTypeForSuffix(matcher.group(3));
            String baseError = "Numeric value out of range: " + numberPart + " for a number of type " + expectedType.toString().toLowerCase() + ".";
            if (ctx.get(SetupPropertiesTask.INSTANCE).has("strict-nbt") &&
                    ctx.get(SetupPropertiesTask.INSTANCE).get("strict-nbt").isJsonPrimitive() &&
                    ctx.get(SetupPropertiesTask.INSTANCE).get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                    ctx.get(SetupPropertiesTask.INSTANCE).get("strict-nbt").getAsBoolean()) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, baseError, pattern, ctx);
            } else {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, baseError + " Interpreting as String: \"" + flat + "\"", pattern));
                return new TagString(flat);
            }
        }
    }

    public static void validateIdentifierA(String str, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(!TridentLexerProfile.IDENTIFIER_A_REGEX.matcher(str).matches()) {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "The string '" + str + "' is not a valid argument here", pattern, ctx);
        }
    }

    public static void validateIdentifierB(String str, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(!TridentLexerProfile.IDENTIFIER_B_REGEX.matcher(str).matches()) {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "The string '" + str + "' is not a valid argument here", pattern, ctx);
        }
    }

    @Contract("null, _, _ -> null")
    public static Objective parseObjective(String objName, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(objName == null) return null;
        MinecraftLiteralSet.validateIdentifierA(objName, pattern, ctx);
        if(!ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().exists(objName)) {
            ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "Undefined objective name '" + objName + "'", pattern));
            return ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().create(objName);
        } else {
            return ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().getOrCreate(objName);
        }
    }
}

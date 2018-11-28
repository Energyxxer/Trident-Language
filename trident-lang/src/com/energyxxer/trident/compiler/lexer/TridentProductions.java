package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.defpacks.DefinitionBlueprint;
import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.*;
import com.energyxxer.util.logger.Debug;

import java.io.IOException;
import java.util.HashMap;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentProductions {

    public static final LazyTokenStructureMatch FILE;
    public static final LazyTokenStructureMatch ENTRY;

    public static final LazyTokenItemMatch COMMENT_S;
    public static final LazyTokenItemMatch VERBATIM_COMMAND_S;
    public static final LazyTokenGroupMatch DIRECTIVE;
    //public static final LazyTokenItemMatch INSTRUCTION;
    public static final LazyTokenStructureMatch COMMAND;

    public static final LazyTokenGroupMatch RESOURCE_LOCATION_TAGGED;

    public static final LazyTokenStructureMatch SELECTOR;
    public static final LazyTokenStructureMatch SELECTOR_ARGUMENT;
    public static final LazyTokenStructureMatch TEXT_COMPONENT;

    public static final LazyTokenStructureMatch PLAYER_NAME;

    public static final LazyTokenStructureMatch TEXT_COLOR;

    public static final LazyTokenStructureMatch INTEGER_NUMBER_RANGE = new LazyTokenStructureMatch("INTEGER_NUMBER_RANGE");
    public static final LazyTokenStructureMatch REAL_NUMBER_RANGE = new LazyTokenStructureMatch("REAL_NUMBER_RANGE");

    public static final LazyTokenStructureMatch NBT_COMPOUND = new LazyTokenStructureMatch("NBT_COMPOUND");
    public static final LazyTokenStructureMatch NBT_LIST = new LazyTokenStructureMatch("NBT_LIST");
    public static final LazyTokenStructureMatch NBT_VALUE = new LazyTokenStructureMatch("NBT_VALUE");

    public static final LazyTokenStructureMatch NBT_PATH = new LazyTokenStructureMatch("NBT_PATH");
    
    public static final LazyTokenStructureMatch SINGLE_COORDINATE = new LazyTokenStructureMatch("SINGLE_COORDINATE");
    public static final LazyTokenStructureMatch ABSOLUTE_COORDINATE = new LazyTokenStructureMatch("ABSOLUTE_COORDINATE");
    public static final LazyTokenStructureMatch RELATIVE_COORDINATE = new LazyTokenStructureMatch("RELATIVE_COORDINATE");
    public static final LazyTokenStructureMatch LOCAL_COORDINATE = new LazyTokenStructureMatch("LOCAL_COORDINATE");
    public static final LazyTokenStructureMatch MIXABLE_COORDINATE = new LazyTokenStructureMatch("MIXABLE_COORDINATE");
    public static final LazyTokenStructureMatch COORDINATE_SET = new LazyTokenStructureMatch("COORDINATE_SET");
    public static final LazyTokenStructureMatch TWO_COORDINATE_SET = new LazyTokenStructureMatch("TWO_COORDINATE_SET");

    public static final LazyTokenStructureMatch BLOCKSTATE = new LazyTokenStructureMatch("BLOCKSTATE");
    public static final LazyTokenStructureMatch BLOCK = new LazyTokenStructureMatch("BLOCK");
    public static final LazyTokenStructureMatch BLOCK_TAGGED = new LazyTokenStructureMatch("BLOCK_TAGGED");
    public static final LazyTokenStructureMatch ITEM = new LazyTokenStructureMatch("ITEM");
    public static final LazyTokenStructureMatch ITEM_TAGGED = new LazyTokenStructureMatch("ITEM_TAGGED");

    public static final LazyTokenStructureMatch PARTICLE = new LazyTokenStructureMatch("PARTICLE");

    public static final LazyTokenStructureMatch BLOCK_ID = new LazyTokenStructureMatch("BLOCK_ID");
    public static final LazyTokenStructureMatch ITEM_ID = new LazyTokenStructureMatch("ITEM_ID");
    public static final LazyTokenStructureMatch ENTITY_ID = new LazyTokenStructureMatch("ENTITY_ID");
    public static final LazyTokenStructureMatch EFFECT_ID = new LazyTokenStructureMatch("EFFECT_ID");
    public static final LazyTokenStructureMatch PARTICLE_ID = new LazyTokenStructureMatch("PARTICLE_ID");
    public static final LazyTokenStructureMatch ENCHANTMENT_ID = new LazyTokenStructureMatch("ENCHANTMENT_ID");
    public static final LazyTokenStructureMatch DIMENSION_ID = new LazyTokenStructureMatch("DIMENSION_ID");
    public static final LazyTokenStructureMatch SLOT_ID = new LazyTokenStructureMatch("SLOT_ID");

    public static final LazyTokenStructureMatch GAMEMODE = new LazyTokenStructureMatch("GAMEMODE");
    public static final LazyTokenStructureMatch GAMERULE = new LazyTokenStructureMatch("GAMERULE");
    public static final LazyTokenStructureMatch GAMERULE_SETTER = new LazyTokenStructureMatch("GAMERULE_SETTER");
    public static final LazyTokenStructureMatch STRUCTURE = new LazyTokenStructureMatch("STRUCTURE");
    public static final LazyTokenStructureMatch DIFFICULTY = new LazyTokenStructureMatch("DIFFICULTY");

    public static final LazyTokenStructureMatch STRING_LITERAL_OR_IDENTIFIER_A = new LazyTokenStructureMatch("STRING_LITERAL_OR_IDENTIFIER_A");

    //grouped arguments
    public static final LazyTokenStructureMatch ENTITY = new LazyTokenStructureMatch("ENTITY");
    public static final LazyTokenGroupMatch VARIABLE_MARKER = group(ofType(VARIABLE_MARKER_START), identifierA().setName("VARIABLE_NAME"), ofType(VARIABLE_MARKER_END)).setName("VARIABLE_MARKER");

    static {
        FILE = new LazyTokenStructureMatch("FILE");
        ENTRY = new LazyTokenStructureMatch("ENTRY");
        COMMAND = new LazyTokenStructureMatch("COMMAND");
        TEXT_COMPONENT = new LazyTokenStructureMatch("TEXT_COMPONENT");
        SELECTOR = new LazyTokenStructureMatch("SELECTOR");
        SELECTOR_ARGUMENT = new LazyTokenStructureMatch("SELECTOR_ARGUMENT");
        PLAYER_NAME = choice(identifierB()).setName("PLAYER_NAME");

        COMMENT_S = new LazyTokenItemMatch(COMMENT).setName("COMMENT");
        VERBATIM_COMMAND_S = new LazyTokenItemMatch(VERBATIM_COMMAND).setName("VERBATIM_COMMAND");

        {
            LazyTokenStructureMatch directiveBody = new LazyTokenStructureMatch("DIRECTIVE_BODY");

            DIRECTIVE = group(ofType(DIRECTIVE_HEADER), directiveBody).setName("DIRECTIVE");

            directiveBody.add(group(literal("on"), ofType(DIRECTIVE_ON_KEYWORD)).setName("ON_DIRECTIVE"));
            directiveBody.add(group(literal("tag"), ofType(RESOURCE_LOCATION)).setName("TAG_DIRECTIVE"));
            directiveBody.add(group(literal("require"), ofType(RESOURCE_LOCATION)).setName("REQUIRE_DIRECTIVE"));
        }

        RESOURCE_LOCATION_TAGGED = group(optional(hash(), ofType(GLUE)), ofType(RESOURCE_LOCATION)).setName("RESOURCE_LOCATION_TAGGED");

        ENTRY.add(COMMENT_S);
        ENTRY.add(COMMAND);
        ENTRY.add(VERBATIM_COMMAND_S);

        STRING_LITERAL_OR_IDENTIFIER_A.add(string());
        STRING_LITERAL_OR_IDENTIFIER_A.add(identifierA());

        {
            LazyTokenGroupMatch separator = new LazyTokenGroupMatch(true).setName("LINE_PADDING");
            separator.append(new LazyTokenListMatch(TokenType.NEWLINE, true));
            LazyTokenListMatch l = new LazyTokenListMatch(new LazyTokenGroupMatch(true).append(ENTRY), separator, true).setName("ENTRIES");
            FILE.add(group(optional(list(DIRECTIVE).setOptional(true).setName("DIRECTIVES")),l));
        }

        TEXT_COLOR = choice("black", "dark_blue", "dark_aqua", "dark_green", "dark_red", "dark_purple", "gold", "light_gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white");


        ENTITY.add(PLAYER_NAME);
        ENTITY.add(SELECTOR);
        ENTITY.add(VARIABLE_MARKER);

        //region Commands
        //region say
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "say"));
            g.append(ofType(TRAILING_STRING));
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
            g.append(new LazyTokenGroupMatch(true).append(ENTITY).setName("PLAYER"));
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
                    group(literal("add"), ofType(IDENTIFIER_TYPE_A)),
                    group(literal("remove"), ofType(IDENTIFIER_TYPE_A))
            ));
            COMMAND.add(g);
        }
        //endregion
        //region experience
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(choice(matchItem(COMMAND_HEADER, "experience"), matchItem(COMMAND_HEADER, "xp")));
            {
                LazyTokenStructureMatch u = choice("points", "levels");

                LazyTokenStructureMatch s = new LazyTokenStructureMatch("SUBCOMMAND");
                s.add(new LazyTokenGroupMatch().append(literal("add")).append(ENTITY).append(integer()).append(new LazyTokenGroupMatch(true).append(u)));
                s.add(new LazyTokenGroupMatch().append(literal("set")).append(ENTITY).append(integer()).append(new LazyTokenGroupMatch(true).append(u)));
                s.add(new LazyTokenGroupMatch().append(literal("query")).append(ENTITY).append(new LazyTokenGroupMatch(true).append(u)));
                g.append(s);
            }
            COMMAND.add(g);
        }
        //endregion
        //region seed
        {
            COMMAND.add(matchItem(COMMAND_HEADER, "seed"));
        }
        //endregion
        //region give
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "give"),
                    ENTITY,
                    ITEM,
                    integer().setOptional().setName("AMOUNT")
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
                                    integer().setOptional().setName("AMOUNT")
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
                            group(literal("clear"), ENTITY, optional(EFFECT_ID)),
                            group(literal("give"), ENTITY, EFFECT_ID, optional(integer(), integer().setOptional()))
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
                    integer().setOptional()
            ));
        }
        //endregion
        //region function
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "function"),
                    ofType(LINE_GLUE),
                    RESOURCE_LOCATION_TAGGED
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
        //region me
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "me"),
                    ofType(TRAILING_STRING)
            ));
        }
        //endregion
        //region msg
        {
            COMMAND.add(group(
                    choice(matchItem(COMMAND_HEADER, "msg"), matchItem(COMMAND_HEADER, "w")),
                    ENTITY,
                    ofType(TRAILING_STRING)
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
                    ofType(RESOURCE_LOCATION),
                    ofType(SOUND_CHANNEL),
                    ENTITY,
                    optional(
                            COORDINATE_SET,
                            optional(real(),
                                    optional(real(),
                                            real().setOptional()))
                    )
            ));
        }
        //endregion
        //region clone
        {
            LazyTokenStructureMatch mode = choice("force", "move", "normal");

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "clone"),
                    COORDINATE_SET,
                    COORDINATE_SET,
                    COORDINATE_SET,
                    choice(
                            group(literal("filtered"), ofType(LINE_GLUE), BLOCK_TAGGED, optional(mode)),
                            group(literal("masked"), optional(mode)),
                            group(literal("replace"), optional(mode))
                    ).setOptional()
            ));
        }
        //endregion
        //region fill
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "fill"),
                    COORDINATE_SET,
                    COORDINATE_SET,
                    BLOCK,
                    choice(
                            literal("destroy"),
                            literal("hollow"),
                            literal("keep"),
                            literal("outline"),
                            group(literal("replace"), optional(BLOCK_TAGGED))
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
                                    COORDINATE_SET,
                                    real(),
                                    integer(),
                                    optional(
                                            choice("force", "normal"),
                                            optional(ENTITY)
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
                    choice("give", "take"),
                    ENTITY,
                    choice(
                            matchItem(SYMBOL, "*"),
                            ofType(RESOURCE_LOCATION)
                    )
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
                    ),
                    SLOT_ID,
                    ITEM
            ));
        }
        //endregion
        //region schedule
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "schedule"),
                    literal("function"),
                    ofType(LINE_GLUE),
                    RESOURCE_LOCATION_TAGGED,
                    ofType(TIME)
            ));
        }
        //endregion
        //region setblock
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "setblock"),
                    COORDINATE_SET,
                    BLOCK,
                    choice("destroy", "keep", "replace").setOptional()
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
                    TWO_COORDINATE_SET,
                    real(),
                    real(),
                    ofType(BOOLEAN),
                    SELECTOR
            ));
        }
        //endregion
        //region stopsound
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "stopsound"),
                    ENTITY,
                    optional(
                            choice(
                                    group(ofType(SOUND_CHANNEL), ofType(RESOURCE_LOCATION).setOptional()),
                                    group(matchItem(SYMBOL, "*"), ofType(RESOURCE_LOCATION)))
                    )
            ));
        }
        //endregion
        //region summon
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "summon"),
                    ENTITY_ID,
                    optional(
                            COORDINATE_SET,
                            optional(NBT_COMPOUND)
                    )
            ));
        }
        //endregion
        //region teleport
        {
            COMMAND.add(group(
                    choice(matchItem(COMMAND_HEADER, "teleport"), matchItem(COMMAND_HEADER, "tp")),
                    choice(ENTITY, COORDINATE_SET),
                    optional(
                            choice(ENTITY, COORDINATE_SET),
                            optional(
                                    literal("facing"),
                                    choice(
                                            COORDINATE_SET,
                                            group(literal("entity"), ENTITY, ofType(ANCHOR).setOptional())
                                    )
                            )
                    )
            ));
        }
        //endregion
        //region time
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "time"),
                    choice(
                            group(literal("query"), choice("day", "daytime", "gametime")),
                            group(literal("add"), choice(ofType(TIME), choice("day", "midnight", "night", "noon"))),
                            group(literal("set"), ofType(TIME))
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
                            group(choice("title", "subtitle", "actionbar"), TEXT_COMPONENT),
                            choice("clear", "reset"),
                            group(literal("times"), integer(), integer(), integer())
                    )
            ));
        }
        //endregion
        //region trigger
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "trigger"),
                    ofType(IDENTIFIER_TYPE_A),
                    optional(choice("add", "set"), integer())
            ));
        }
        //endregion
        //region weather
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "weather"),
                    choice("clear", "rain", "thunder"),
                    integer().setOptional()
            ));
        }
        //endregion
        //region worldborder
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "worldborder"),
                    choice(
                            literal("get"),
                            group(choice("add", "set"), real(), integer().setOptional()),
                            group(literal("center"), TWO_COORDINATE_SET),
                            group(literal("damage"), choice("amount", "buffer"), real()),
                            group(literal("warning"), choice("distance", "time"), integer())
                    )
            ));
        }
        //endregion
        //region team
        {
            LazyTokenStructureMatch teamOptions = choice(
                    group(literal("collisionRule"), choice("always", "never", "pushOtherTeams", "pushOwnTeam")),
                    group(literal("color"), TEXT_COLOR),
                    group(literal("deathMessageVisibility"), choice("always", "hideForOtherTeams", "hideForOwnTeam", "never")),
                    group(literal("displayName"), TEXT_COMPONENT),
                    group(literal("friendlyFire"), ofType(BOOLEAN)),
                    group(literal("nametagVisibility"), choice("always", "hideForOtherTeams", "hideForOwnTeam", "never")),
                    group(literal("prefix"), TEXT_COMPONENT),
                    group(literal("suffix"), TEXT_COMPONENT),
                    group(literal("seeFriendlyInvisibles"), ofType(BOOLEAN))
            );

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "team"),
                    choice(
                            group(literal("add"), identifierA(), optional(TEXT_COMPONENT)),
                            group(literal("empty"), identifierA()),
                            group(literal("join"), identifierA(), optional(ENTITY)),
                            group(literal("leave"), ENTITY),
                            group(literal("list"), optional(sameLine(), identifierA())),
                            group(literal("modify"), identifierA(), teamOptions),
                            group(literal("remove"), identifierA())
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
                                    group(literal("add"), identifierA(), ofType(RESOURCE_LOCATION), optional(TEXT_COMPONENT)),
                                    literal("list"),
                                    group(literal("modify"), identifierA(), choice(
                                            group(literal("displayname"), TEXT_COMPONENT),
                                            group(literal("rendertype"), choice("integer", "hearts"))
                                    )),
                                    group(literal("remove"), identifierA()),
                                    group(literal("setdisplay"), identifierA().setName("DISPLAY_SLOT"), optional(sameLine(), identifierA()))
                            )),
                            group(literal("players"), choice(
                                    group(choice("add", "remove", "set"), ENTITY, identifierA(), integer()),
                                    group(literal("enable"), ENTITY, identifierA()),
                                    group(literal("get"), ENTITY, identifierA()),
                                    group(literal("list"), optional(ENTITY)),
                                    group(literal("operation"), ENTITY, identifierA(), ofType(SCOREBOARD_OPERATOR), ENTITY, identifierA()),
                                    group(literal("reset"), ENTITY, optional(sameLine(), identifierA()))
                            ))
                    )
            ));
        }
        //endregion
        //region advancement
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "advancement"),
                    choice("grant", "revoke"),
                    ENTITY,
                    choice(
                            literal("everything"),
                            group(choice("from", "through", "until"), ofType(RESOURCE_LOCATION)),
                            group(literal("only"), ofType(RESOURCE_LOCATION), optional(sameLine(), ofType(TRAILING_STRING)))
                    )
            ));
        }
        //endregion
        //region bossbar
        {
            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "bossbar"),
                    choice(
                            literal("list"),
                            group(literal("add"), ofType(RESOURCE_LOCATION), TEXT_COMPONENT),
                            group(literal("get"), ofType(RESOURCE_LOCATION), choice("max", "players", "value", "visible")),
                            group(literal("remove"), ofType(RESOURCE_LOCATION)),
                            group(literal("set"), ofType(RESOURCE_LOCATION), choice(
                                    group(literal("color"), choice("blue", "green", "pink", "purple", "red", "white", "yellow")),
                                    group(literal("max"), integer()),
                                    group(literal("name"), TEXT_COMPONENT),
                                    group(literal("players"), ENTITY),
                                    group(literal("style"), choice("progress", "notched_6", "notched_10", "notched_12", "notched_20")),
                                    group(literal("value"), integer()),
                                    group(literal("visible"), ofType(BOOLEAN))
                            ))
                    )
            ));
        }
        //endregion
        //region data
        {

            LazyTokenStructureMatch target = choice(
                    group(literal("block"), COORDINATE_SET),
                    group(literal("entity"), ENTITY)
            );

            LazyTokenStructureMatch source = choice(
                    group(literal("from"), target, optional(NBT_PATH)),
                    group(literal("value"), NBT_VALUE)
            );

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "data"),
                    choice(
                            group(literal("get"), target, optional(NBT_PATH, real().setOptional())),
                            group(literal("merge"), target, NBT_COMPOUND),
                            group(literal("modify"), target, NBT_PATH, choice(
                                    group(literal("append"), source),
                                    group(literal("insert"), choice("before", "after"), integer(), source),
                                    group(literal("merge"), source),
                                    group(literal("prepend"), source),
                                    group(literal("set"), source)
                            )),
                            group(literal("remove"), target, NBT_PATH)
                    )
            ));
        }
        //endregion
        //region drop
        {

            LazyTokenStructureMatch tool = choice(literal("mainhand"), literal("offhand"), ITEM);

            LazyTokenStructureMatch destination = choice(
                    group(literal("block"), COORDINATE_SET, choice(
                            group(literal("distribute")),
                            group(literal("insert"), SLOT_ID)
                    )),
                    group(literal("entity"), ENTITY, SLOT_ID),
                    group(literal("player"), ENTITY),
                    group(literal("world"), COORDINATE_SET)
            );

            LazyTokenStructureMatch source = choice(
                    group(literal("fish"), ofType(RESOURCE_LOCATION), COORDINATE_SET, optional(tool)),
                    group(literal("kill"), ENTITY),
                    group(literal("loot"), ofType(RESOURCE_LOCATION)),
                    group(literal("mine"), COORDINATE_SET, optional(tool))
            );

            COMMAND.add(group(
                    matchItem(COMMAND_HEADER, "drop"),
                    destination, source
            ));
        }
        //endregion
        //endregion



        //region Blockstate
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(brace("["));
            {
                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch().setName("BLOCKSTATE_PROPERTY");
                g2.append(ofType(IDENTIFIER_TYPE_A).setName("BLOCKSTATE_PROPERTY_KEY"));
                g2.append(equals());
                {
                    LazyTokenStructureMatch s = new LazyTokenStructureMatch("BLOCKSTATE_PROPERTY_VALUE");
                    s.add(real());
                    s.add(ofType(BOOLEAN));
                    s.add(ofType(IDENTIFIER_TYPE_A));
                    g2.append(s);
                }
                g.append(new LazyTokenListMatch(g2, comma(), true));
            }
            g.append(brace("]"));

            BLOCKSTATE.add(g);
        }
        //endregion
        //region Block
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("CONCRETE_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(BLOCK_ID).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(BLOCKSTATE));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            BLOCK.add(g);
            BLOCK_TAGGED.add(BLOCK);
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ABSTRACT_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(hash().setName("TAG_HEADER")).append(ofType(GLUE)).append(ofType(RESOURCE_LOCATION)).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(BLOCKSTATE));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            BLOCK_TAGGED.add(g);
        }
        //endregion
        //region Item
        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("CONCRETE_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(ITEM_ID).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            ITEM.add(g);
            ITEM_TAGGED.add(ITEM);
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ABSTRACT_RESOURCE");
            g.append(new LazyTokenGroupMatch().append(hash().setName("TAG_HEADER")).append(ofType(GLUE)).append(ofType(RESOURCE_LOCATION).setName("RESOURCE_LOCATION")).setName("RESOURCE_NAME"));
            g.append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(NBT_COMPOUND));
            ITEM_TAGGED.add(g);
        }
        //endregion


        //region Text Components
        {
            LazyTokenStructureMatch JSON_ELEMENT = new LazyTokenStructureMatch("JSON_ELEMENT");

            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("JSON_OBJECT");
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(string().setName("JSON_OBJECT_KEY"));
                    g2.append(colon());
                    g2.append(JSON_ELEMENT);
                    g.append(new LazyTokenListMatch(g2, comma(), true).setName("JSON_OBJECT_ENTRIES"));
                }
                g.append(brace("}"));
                JSON_ELEMENT.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("JSON_ARRAY");
                g.append(brace("["));
                g.append(new LazyTokenListMatch(JSON_ELEMENT, comma(), true).setName("JSON_ARRAY_ENTRIES"));
                g.append(brace("]"));
                JSON_ELEMENT.add(g);
            }
            JSON_ELEMENT.add(string().setName("STRING_LITERAL"));
            JSON_ELEMENT.add(real().setName("NUMBER"));
            JSON_ELEMENT.add(ofType(BOOLEAN).setName("BOOLEAN"));

            TEXT_COMPONENT.add(JSON_ELEMENT);
        }
        //endregion

        //region NBT
        {
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(new LazyTokenGroupMatch().append(STRING_LITERAL_OR_IDENTIFIER_A).setName("NBT_KEY"));
                    g2.append(colon());
                    g2.append(NBT_VALUE);
                    g.append(new LazyTokenListMatch(g2, comma(), true).setName("NBT_COMPOUND_ENTRIES"));
                }
                g.append(brace("}"));
                NBT_COMPOUND.add(g);
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
            NBT_VALUE.add(string());
            NBT_VALUE.add(ofType(IDENTIFIER_TYPE_A).setName("RAW_STRING"));
            NBT_VALUE.add(ofType(TYPED_NUMBER).setName("NBT_NUMBER"));
            NBT_VALUE.add(ofType(BOOLEAN).setName("BOOLEAN"));
        }
        //endregion

        {
            LazyTokenStructureMatch NBT_PATH_NODE = new LazyTokenStructureMatch("NBT_PATH_NODE");

            NBT_PATH_NODE.add(
                    group(
                            glue(),
                            dot().setName("NBT_PATH_SEPARATOR"),
                            glue(),
                            group(STRING_LITERAL_OR_IDENTIFIER_A).setName("NBT_PATH_KEY_LABEL")
                    ).setName("NBT_PATH_KEY"));

            NBT_PATH_NODE.add(group(glue(), brace("["), integer(), brace("]")).setName("NBT_PATH_INDEX"));

            NBT_PATH.add(group(group(group(STRING_LITERAL_OR_IDENTIFIER_A).setName("NBT_PATH_KEY_LABEL")).setName("NBT_PATH_KEY"),
                    list(NBT_PATH_NODE, glue()).setOptional()));
        }

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
            INTEGER_NUMBER_RANGE.add(integer());
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(integer());
                g.append(dot());
                g.append(dot());
                g.append(new LazyTokenGroupMatch(true).append(integer()));
                INTEGER_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(dot());
                g.append(integer());
                INTEGER_NUMBER_RANGE.add(g);
            }

            REAL_NUMBER_RANGE.add(real());
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(real());
                g.append(dot());
                g.append(dot());
                g.append(new LazyTokenGroupMatch(true).append(real()));
                REAL_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(dot());
                g.append(real());
                REAL_NUMBER_RANGE.add(g);
            }
        }
        //endregion
        //region Selector Arguments
        {
            //Integer Range Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("INTEGER_RANGE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("level"));

            g.append(new LazyTokenGroupMatch().setName("INTEGER_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(INTEGER_NUMBER_RANGE);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Real Number Range Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("REAL_RANGE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("distance"));
            s.add(literal("x_rotation"));
            s.add(literal("y_rotation"));

            g.append(new LazyTokenGroupMatch().setName("REAL_NUMBER_RANGE_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(REAL_NUMBER_RANGE);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Integer Number Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("INTEGER_NUMBER_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("limit"));

            g.append(new LazyTokenGroupMatch().setName("INTEGER_RANGE_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(integer());

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Real Number Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("REAL_NUMBER_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("x"));
            s.add(literal("y"));
            s.add(literal("z"));
            s.add(literal("dx"));
            s.add(literal("dy"));
            s.add(literal("dz"));

            g.append(new LazyTokenGroupMatch().setName("REAL_NUMBER_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(real());

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Identifier Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("IDENTIFIER_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("tag"));
            s.add(literal("team"));

            g.append(s);
            g.append(equals());
            g.append(not().setOptional());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE", true);
            s2.add(identifierA());

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //String Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("STRING_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("name"));

            g.append(s);
            g.append(equals());
            g.append(not().setOptional());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE", true);
            s2.add(STRING_LITERAL_OR_IDENTIFIER_A);

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Gamemode argument
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("GAMEMODE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("gamemode"));

            g.append(s);
            g.append(equals());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE");
            s2.add(new LazyTokenGroupMatch().append(not().setOptional()).append(GAMEMODE));

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Type argument
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("TYPE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("type"));

            g.append(s);
            g.append(equals());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE");
            s2.add(new LazyTokenGroupMatch().append(not().setOptional()).append(ENTITY_ID));

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Sort argument
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("SORT_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(literal("sort"));

            g.append(s);
            g.append(equals());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE");
            s2.add(ofType(SORTING));

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }
        //endregion

        //region Coordinates
        {
            LOCAL_COORDINATE.add(new LazyTokenGroupMatch().append(caret()).append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(real())));

            ABSOLUTE_COORDINATE.add(real());

            RELATIVE_COORDINATE.add(new LazyTokenGroupMatch().append(tilde()).append(new LazyTokenGroupMatch(true).append(ofType(GLUE)).append(real())));

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
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("LOCAL_TWO_COORDINATE_SET");
                g.append(LOCAL_COORDINATE);
                g.append(LOCAL_COORDINATE);
                TWO_COORDINATE_SET.add(g);
            }
        }
        //endregion



        try {
            DefinitionPack defpack = StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT;
            defpack.load();

            for (DefinitionBlueprint def : defpack.getBlueprints("structure")) {
                STRUCTURE.add(literal(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("difficulty")) {
                DIFFICULTY.add(literal(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("gamemode")) {
                GAMEMODE.add(literal(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("dimension")) {
                DIMENSION_ID.add(literal(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("slot")) {
                String[] parts = def.getName().split("\\.");

                LazyTokenGroupMatch g = new LazyTokenGroupMatch();

                for (int i = 0; i < parts.length; i++) {
                    g.append(literal(parts[i]));
                    if (i < parts.length - 1) g.append(dot());
                }

                SLOT_ID.add(g);
            }

            HashMap<String, LazyTokenStructureMatch> namespaceGroups = new HashMap<>();

            for (DefinitionBlueprint def : defpack.getBlueprints("block")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("BLOCK_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(literal(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("TYPE_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    BLOCK_ID.add(g);
                }

                s.add(literal(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("item")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ITEM_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(literal(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("TYPE_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ITEM_ID.add(g);
                }

                s.add(literal(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("entity")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ENTITY_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(literal(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("TYPE_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ENTITY_ID.add(g);
                }

                s.add(literal(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("effect")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("EFFECT_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(literal(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("TYPE_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    EFFECT_ID.add(g);
                }

                s.add(literal(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("enchantment")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ENCHANTMENT_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(literal(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("TYPE_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ENCHANTMENT_ID.add(g);
                }

                s.add(literal(def.getName()));
            }

            namespaceGroups.clear();

            LazyTokenGroupMatch COLOR = new LazyTokenGroupMatch().setName("COLOR")
                    .append(real().setName("RED_COMPONENT"))
                    .append(real().setName("GREEN_COMPONENT"))
                    .append(real().setName("BLUE_COMPONENT"));


            for (DefinitionBlueprint def : defpack.getBlueprints("particle")) {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("PARTICLE_ID");

                LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                ns.append(literal(def.getNamespace()));
                ns.append(colon());

                g.append(ns);

                g.append(literal(def.getName()).setName("TYPE_NAME"));

                PARTICLE_ID.add(g);

                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();

                g2.append(g);

                LazyTokenGroupMatch argsGroup = new LazyTokenGroupMatch().setName("PARTICLE_ARGUMENTS");

                String allArgs = def.getProperties().get("argument");
                if (!allArgs.equals("none")) {
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
                                Debug.log("Invalid particle argument type '" + arg + "', could not be added to .mcfunction particle production", Debug.MessageType.ERROR);
                            }
                        }
                    }
                }

                g2.append(argsGroup);

                PARTICLE.add(g2);
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("gamerule")) {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("GAMERULE_ID");

                g.append(literal(def.getName()).setName("GAMERULE"));

                GAMERULE.add(g);

                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();

                g2.append(g);

                LazyTokenGroupMatch argsGroup = new LazyTokenGroupMatch().setName("GAMERULE_ARGUMENT");

                String arg = def.getProperties().get("argument");

                switch (arg) {
                    case "boolean": {
                        argsGroup.append(ofType(BOOLEAN));
                        break;
                    }
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
                        Debug.log("Invalid gamerule argument type '" + arg + "', could not be added to .mcfunction gamerule setter production", Debug.MessageType.ERROR);
                    }
                }

                g2.append(argsGroup);

                GAMERULE_SETTER.add(g2);
            }

            namespaceGroups.clear();

        } catch (IOException x) {
            Debug.log("Error in loading standard definition pack for Minecraft Java Edition 1.13: " + x.getMessage(), Debug.MessageType.ERROR);
        }
    }

    private static LazyTokenPatternMatch identifierB() {
        return ofType(IDENTIFIER_TYPE_B);
    }

    private static LazyTokenItemMatch literal(String text) {
        return new LazyTokenItemMatch(TokenType.UNKNOWN, text);
    }

    private static LazyTokenItemMatch matchItem(TokenType type, String text) {
        return new LazyTokenItemMatch(type, text).setName("ITEM_MATCH");
    }

    private static LazyTokenItemMatch brace(String brace) {
        return matchItem(BRACE, brace);
    }

    private static LazyTokenItemMatch colon() {
        return ofType(COLON);
    }

    private static LazyTokenItemMatch comma() {
        return ofType(COMMA).setName("COMMA");
    }

    private static LazyTokenItemMatch dot() {
        return ofType(DOT);
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
        return ofType(HASH);
    }

    private static LazyTokenItemMatch string() {
        return ofType(STRING_LITERAL).setName("STRING_LITERAL");
    }

    private static LazyTokenItemMatch integer() {
        return ofType(INTEGER_NUMBER).setName("INTEGER");
    }

    private static LazyTokenItemMatch real() {
        return ofType(REAL_NUMBER).setName("REAL");
    }

    private static LazyTokenItemMatch glue() {
        return ofType(GLUE);
    }

    private static LazyTokenItemMatch sameLine() {
        return ofType(LINE_GLUE);
    }



    private static LazyTokenItemMatch identifierA() {
        return ofType(IDENTIFIER_TYPE_A);
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
            s.add(option);
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
            g.append(item);
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
}

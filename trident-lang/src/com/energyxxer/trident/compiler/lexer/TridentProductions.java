package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.commodore.defpacks.DefinitionBlueprint;
import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenStructureMatch;
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


    public static final LazyTokenStructureMatch SELECTOR;
    public static final LazyTokenStructureMatch SELECTOR_ARGUMENT;
    public static final LazyTokenStructureMatch TEXT_COMPONENT;

    public static final LazyTokenStructureMatch INTEGER_NUMBER_RANGE = new LazyTokenStructureMatch("INTEGER_NUMBER_RANGE");
    public static final LazyTokenStructureMatch REAL_NUMBER_RANGE = new LazyTokenStructureMatch("REAL_NUMBER_RANGE");

    public static final LazyTokenStructureMatch NBT_COMPOUND = new LazyTokenStructureMatch("NBT_COMPOUND");
    public static final LazyTokenStructureMatch NBT_VALUE = new LazyTokenStructureMatch("NBT_VALUE");


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

    public static final LazyTokenStructureMatch STRING_LITERAL_OR_UNKNOWN = new LazyTokenStructureMatch("STRING_LITERAL_OR_UNKNOWN");


    static {
        FILE = new LazyTokenStructureMatch("FILE");
        ENTRY = new LazyTokenStructureMatch("ENTRY");
        COMMAND = new LazyTokenStructureMatch("COMMAND");
        TEXT_COMPONENT = new LazyTokenStructureMatch("TEXT_COMPONENT");
        SELECTOR = new LazyTokenStructureMatch("SELECTOR");
        SELECTOR_ARGUMENT = new LazyTokenStructureMatch("SELECTOR_ARGUMENT");

        COMMENT_S = new LazyTokenItemMatch(COMMENT);
        VERBATIM_COMMAND_S = new LazyTokenItemMatch(VERBATIM_COMMAND);

        DIRECTIVE = new LazyTokenGroupMatch();
        DIRECTIVE.setName("DIRECTIVE");
        {
            DIRECTIVE.append(new LazyTokenItemMatch(DIRECTIVE_HEADER));
            DIRECTIVE.append(verbatimMatch("on"));
            DIRECTIVE.append(ofType(DIRECTIVE_ON_KEYWORD));
        }

        ENTRY.add(COMMENT_S);
        ENTRY.add(DIRECTIVE);
        ENTRY.add(COMMAND);
        ENTRY.add(VERBATIM_COMMAND_S);

        STRING_LITERAL_OR_UNKNOWN.add(string());
        STRING_LITERAL_OR_UNKNOWN.add(ofType(TokenType.UNKNOWN));

        {
            LazyTokenGroupMatch separator = new LazyTokenGroupMatch(true);
            separator.append(new LazyTokenListMatch(TokenType.NEWLINE, true));
            LazyTokenListMatch l = new LazyTokenListMatch(new LazyTokenGroupMatch(true).append(ENTRY), separator, true);
            FILE.add(l);
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "say"));
            g.append(ofType(TRAILING_STRING));
            COMMAND.add(g);
        }


        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(matchItem(COMMAND_HEADER, "tellraw"));
            g.append(SELECTOR);
            g.append(TEXT_COMPONENT);
            COMMAND.add(g);
        }






        {
            LazyTokenStructureMatch JSON_ELEMENT = new LazyTokenStructureMatch("JSON_ELEMENT");

            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(string());
                    g2.append(colon());
                    g2.append(JSON_ELEMENT);
                    g.append(new LazyTokenListMatch(g2, comma(), true));
                }
                g.append(brace("}"));
                JSON_ELEMENT.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(new LazyTokenListMatch(JSON_ELEMENT, comma(), true));
                g.append(brace("]"));
                JSON_ELEMENT.add(g);
            }
            JSON_ELEMENT.add(string());
            JSON_ELEMENT.add(ofType(REAL_NUMBER));
            JSON_ELEMENT.add(ofType(BOOLEAN));

            TEXT_COMPONENT.add(JSON_ELEMENT);
        }

        {
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("{"));
                {
                    LazyTokenGroupMatch g2 = new LazyTokenGroupMatch();
                    g2.append(new LazyTokenGroupMatch().append(STRING_LITERAL_OR_UNKNOWN).setName("NBT_KEY"));
                    g2.append(colon());
                    g2.append(NBT_VALUE);
                    g.append(new LazyTokenListMatch(g2, comma(), true));
                }
                g.append(brace("}"));
                NBT_VALUE.add(g);
                NBT_COMPOUND.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(brace("["));
                g.append(new LazyTokenListMatch(NBT_VALUE, comma(), true));
                g.append(brace("]"));
                NBT_VALUE.add(g);
            }
            NBT_VALUE.add(STRING_LITERAL_OR_UNKNOWN);
            NBT_VALUE.add(ofType(TYPED_NUMBER));
            NBT_VALUE.add(ofType(BOOLEAN));
            NBT_VALUE.add(NBT_VALUE);
        }

        {
            LazyTokenGroupMatch g = new LazyTokenGroupMatch();
            g.append(ofType(SELECTOR_HEADER));

            {
                LazyTokenGroupMatch g2 = new LazyTokenGroupMatch(true);
                g2.append(ofType(GLUE));
                g2.append(brace("["));
                g2.append(new LazyTokenListMatch(SELECTOR_ARGUMENT, comma(), true));
                g2.append(brace("]"));
                g.append(g2);
            }

            SELECTOR.add(g);
        }

        {
            INTEGER_NUMBER_RANGE.add(ofType(INTEGER_NUMBER));
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(ofType(INTEGER_NUMBER));
                g.append(dot());
                g.append(dot());
                g.append(new LazyTokenGroupMatch(true).append(ofType(INTEGER_NUMBER)));
                INTEGER_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(dot());
                g.append(ofType(INTEGER_NUMBER));
                INTEGER_NUMBER_RANGE.add(g);
            }

            REAL_NUMBER_RANGE.add(ofType(REAL_NUMBER));
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(ofType(REAL_NUMBER));
                g.append(dot());
                g.append(dot());
                g.append(new LazyTokenGroupMatch(true).append(ofType(REAL_NUMBER)));
                REAL_NUMBER_RANGE.add(g);
            }
            {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch();
                g.append(dot());
                g.append(dot());
                g.append(ofType(REAL_NUMBER));
                REAL_NUMBER_RANGE.add(g);
            }
        }

        //region Selector Arguments
        {
            //Integer Range Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("INTEGER_RANGE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("level"));

            g.append(new LazyTokenGroupMatch().setName("INTEGER_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(INTEGER_NUMBER_RANGE);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Real Number Range Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("REAL_RANGE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("distance"));
            s.add(verbatimMatch("x_rotation"));
            s.add(verbatimMatch("y_rotation"));

            g.append(new LazyTokenGroupMatch().setName("REAL_NUMBER_RANGE_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(REAL_NUMBER_RANGE);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Integer Number Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("INTEGER_NUMBER_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("limit"));

            g.append(new LazyTokenGroupMatch().setName("INTEGER_RANGE_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(ofType(INTEGER_NUMBER));

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Real Number Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("REAL_NUMBER_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("x"));
            s.add(verbatimMatch("y"));
            s.add(verbatimMatch("z"));
            s.add(verbatimMatch("dx"));
            s.add(verbatimMatch("dy"));
            s.add(verbatimMatch("dz"));

            g.append(new LazyTokenGroupMatch().setName("REAL_NUMBER_ARGUMENT_VALUE").append(s));
            g.append(equals());
            g.append(ofType(REAL_NUMBER));

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //String Arguments
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("STRING_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("name"));
            s.add(verbatimMatch("tag"));
            s.add(verbatimMatch("team"));

            g.append(s);
            g.append(equals());
            g.append(not().setOptional());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE", true);
            s2.add(STRING_LITERAL_OR_UNKNOWN);

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }

        {
            //Gamemode argument
            LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("GAMEMODE_ARGUMENT");

            LazyTokenStructureMatch s = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_KEY");
            s.add(verbatimMatch("gamemode"));

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
            s.add(verbatimMatch("type"));

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
            s.add(verbatimMatch("sort"));

            g.append(s);
            g.append(equals());

            LazyTokenStructureMatch s2 = new LazyTokenStructureMatch("SELECTOR_ARGUMENT_VALUE");
            s2.add(ofType(SORTING));

            g.append(s2);

            SELECTOR_ARGUMENT.add(g);
        }
        //endregion





        try {
            DefinitionPack defpack = StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT;
            defpack.load();

            for (DefinitionBlueprint def : defpack.getBlueprints("structure")) {
                STRUCTURE.add(verbatimMatch(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("difficulty")) {
                DIFFICULTY.add(verbatimMatch(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("gamemode")) {
                GAMEMODE.add(verbatimMatch(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("dimension")) {
                DIMENSION_ID.add(verbatimMatch(def.getName()));
            }

            for (DefinitionBlueprint def : defpack.getBlueprints("slot")) {
                String[] parts = def.getName().split("\\.");

                LazyTokenGroupMatch g = new LazyTokenGroupMatch();

                for (int i = 0; i < parts.length; i++) {
                    g.append(verbatimMatch(parts[i]));
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
                    ns.append(verbatimMatch(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("BLOCK_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    BLOCK_ID.add(g);
                }

                s.add(verbatimMatch(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("item")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ITEM_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(verbatimMatch(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("ITEM_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ITEM_ID.add(g);
                }

                s.add(verbatimMatch(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("entity")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ENTITY_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(verbatimMatch(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("ENTITY_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ENTITY_ID.add(g);
                }

                s.add(verbatimMatch(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("effect")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("EFFECT_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(verbatimMatch(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("EFFECT_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    EFFECT_ID.add(g);
                }

                s.add(verbatimMatch(def.getName()));
            }

            namespaceGroups.clear();

            for (DefinitionBlueprint def : defpack.getBlueprints("enchantment")) {

                LazyTokenStructureMatch s = namespaceGroups.get(def.getNamespace());

                if (s == null) {
                    LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("ENCHANTMENT_ID");

                    LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                    ns.append(verbatimMatch(def.getNamespace()));
                    ns.append(colon());

                    g.append(ns);

                    s = new LazyTokenStructureMatch("ENCHANTMENT_NAME");
                    g.append(s);

                    namespaceGroups.put(def.getNamespace(), s);

                    ENCHANTMENT_ID.add(g);
                }

                s.add(verbatimMatch(def.getName()));
            }

            namespaceGroups.clear();

            LazyTokenGroupMatch COLOR = new LazyTokenGroupMatch().setName("COLOR")
                    .append(ofType(REAL_NUMBER).setName("RED_COMPONENT"))
                    .append(ofType(REAL_NUMBER).setName("GREEN_COMPONENT"))
                    .append(ofType(REAL_NUMBER).setName("BLUE_COMPONENT"));


            for (DefinitionBlueprint def : defpack.getBlueprints("particle")) {
                LazyTokenGroupMatch g = new LazyTokenGroupMatch().setName("PARTICLE_ID");

                LazyTokenGroupMatch ns = new LazyTokenGroupMatch(def.getNamespace().equals("minecraft")).setName("NAMESPACE");
                ns.append(verbatimMatch(def.getNamespace()));
                ns.append(colon());

                g.append(ns);

                g.append(verbatimMatch(def.getName()).setName("PARTICLE_NAME"));

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
                                argsGroup.append(ofType(INTEGER_NUMBER));
                                break;
                            }
                            case "double": {
                                argsGroup.append(ofType(REAL_NUMBER));
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

                g.append(verbatimMatch(def.getName()).setName("GAMERULE_NAME"));

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
                        argsGroup.append(ofType(INTEGER_NUMBER));
                        break;
                    }
                    case "double": {
                        argsGroup.append(ofType(REAL_NUMBER));
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

    private static LazyTokenItemMatch verbatimMatch(String text) {
        return new LazyTokenItemMatch(TokenType.UNKNOWN, text);
    }

    private static LazyTokenItemMatch matchItem(TokenType type, String text) {
        return new LazyTokenItemMatch(type, text);
    }

    private static LazyTokenItemMatch brace(String brace) {
        return matchItem(BRACE, brace);
    }

    private static LazyTokenItemMatch colon() {
        return ofType(COLON);
    }

    private static LazyTokenItemMatch comma() {
        return ofType(COMMA);
    }

    private static LazyTokenItemMatch dot() {
        return ofType(DOT);
    }

    private static LazyTokenItemMatch equals() {
        return ofType(EQUALS);
    }

    private static LazyTokenItemMatch not() {
        return ofType(NOT);
    }

    private static LazyTokenItemMatch string() {
        return ofType(STRING_LITERAL);
    }

    private static LazyTokenItemMatch ofType(TokenType type) {
        return new LazyTokenItemMatch(type);
    }

    private static LazyTokenStructureMatch struct(String name) {
        return new LazyTokenStructureMatch(name);
    }
}

package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentCompiler;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTParser {
    public static TagCompound parseCompound(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) return null;
        return (TagCompound)parseValue(pattern, compiler);
    }

    public static NBTTag parseValue(TokenPattern<?> pattern, TridentCompiler compiler) {
        switch(pattern.getName()) {
            case "NBT_VALUE": {
                return parseValue(((TokenStructure)pattern).getContents(), compiler);
            }
            case "NBT_COMPOUND": {
                return parseValue(((TokenStructure)pattern).getContents(), compiler);
            }
            case "VARIABLE_MARKER": {
                return CommonParsers.retrieveSymbol(pattern, compiler, TagCompound.class);
            }
            case "NBT_COMPOUND_GROUP": {
                TagCompound compound = new TagCompound();
                TokenList entries = (TokenList) pattern.find("NBT_COMPOUND_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (inner instanceof TokenGroup) {
                            String key = inner.find("NBT_KEY").flattenTokens().get(0).value;
                            if (key.startsWith("\"")) {
                                key = CommandUtils.parseQuotedString(key);
                            }
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), compiler);
                            value.setName(key);
                            compound.add(value);
                        }
                    }
                }
                return compound;
            }
            case "NBT_LIST": {
                TagList list = new TagList();
                TokenList entries = (TokenList) pattern.find("..NBT_LIST_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (!inner.getName().equals("COMMA")) {
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), compiler);
                            list.add(value);
                        }
                    }
                }
                return list;
            }
            case "BOOLEAN": {
                return new TagByte(pattern.flattenTokens().get(0).value.equals("true") ? 1 : 0);
            }
            case "RAW_STRING": {
                return new TagString(pattern.flattenTokens().get(0).value);
            }
            case "STRING_LITERAL": {
                return new TagString(CommandUtils.parseQuotedString(pattern.flattenTokens().get(0).value));
            }
            case "NBT_NUMBER": {
                String flat = pattern.flattenTokens().get(0).value;

                final Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?)([bdfsL]?)", Pattern.CASE_INSENSITIVE);

                Matcher matcher = regex.matcher(flat);
                matcher.lookingAt(); //must be true

                String numberPart = matcher.group(1);
                switch(matcher.group(3).toLowerCase()) {
                    case "": {
                        return new TagInt(Integer.parseInt(numberPart));
                    }
                    case "b": {
                        return new TagByte(Byte.parseByte(numberPart));
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
                }
            }
        }
        return new TagString("ERROR WHILE PARSING TAG " + pattern.getName());
    }

    /**
     * NBTParser should not be instantiated.
     * */
    private NBTParser() {
    }

    public static NBTPath parsePath(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) return null;
        if((((TokenStructure)pattern).getContents()).getName().equals("VARIABLE_MARKER")) {
            return CommonParsers.retrieveSymbol(((TokenStructure) pattern).getContents(), compiler, NBTPath.class);
        }
        NBTPathNode start = parsePathNode(pattern.find("NBT_PATH_NODE"), compiler);
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        nodes.add(start);

        TokenList otherNodes = (TokenList) pattern.find("OTHER_NODES");
        if(otherNodes != null) {
            for(TokenPattern<?> node : otherNodes.getContents()) {
                if(!node.getName().equals("GLUE")) {
                    nodes.add(parsePathNode(node, compiler));
                }
            }
        }

        return new NBTPath(nodes.toArray(new NBTPathNode[0]));
    }

    private static NBTPathNode parsePathNode(TokenPattern<?> pattern, TridentCompiler compiler) {
        switch(pattern.getName()) {
            case "NBT_PATH_NODE": {
                return parsePathNode(((TokenStructure) pattern).getContents(), compiler);
            }
            case "NBT_PATH_KEY": {
                return new NBTPathKey(CommonParsers.parseStringLiteralOrIdentifierA(pattern.find("NBT_PATH_KEY_LABEL")));
            }
            case "NBT_PATH_INDEX": {
                return new NBTPathIndex(Integer.parseInt(pattern.find("INTEGER").flatten(false)));
            }
            case "NBT_PATH_LIST_MATCH": {
                return new NBTListMatch(parseCompound(pattern.find("NBT_COMPOUND"), compiler));
            }
            case "NBT_PATH_COMPOUND_MATCH": {
                return new NBTObjectMatch(parseCompound(pattern.find("NBT_COMPOUND"), compiler));
            }
            default: {
                throw new IllegalArgumentException("Unknown NBT path grammar pattern name '" + pattern.getName() + "'");
            }
        }
    }
}

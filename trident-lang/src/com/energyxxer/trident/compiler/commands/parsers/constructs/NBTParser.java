package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataType;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.nbtmapper.tags.FlatType;
import com.energyxxer.nbtmapper.tags.TypeFlags;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;

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
            case "INTERPOLATION_BLOCK": {
                return InterpolationManager.parse(pattern, compiler, TagCompound.class);
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
                        return (numberPart.contains('.')) ?
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
        if((((TokenStructure)pattern).getContents()).getName().equals("INTERPOLATION_BLOCK")) {
            return InterpolationManager.parse(((TokenStructure) pattern).getContents(), compiler, NBTPath.class);
        }
        NBTPathNode start = parsePathNode(pattern.find("NBT_PATH_NODE"), compiler);
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        nodes.add(start);

        TokenList otherNodes = (TokenList) pattern.find("OTHER_NODES");
        if(otherNodes != null) {
            for(TokenPattern<?> node : otherNodes.getContents()) {
                nodes.add(parsePathNode(node.find("NBT_PATH_NODE"), compiler));
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
                return new NBTPathIndex(CommonParsers.parseInt(pattern.find("INTEGER"), compiler));
            }
            case "NBT_PATH_LIST_MATCH": {
                return new NBTListMatch(parseCompound(pattern.find("NBT_COMPOUND"), compiler));
            }
            case "NBT_PATH_LIST_UNKNOWN": {
                Object val = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), compiler, Integer.class, TagCompound.class);
                if(val instanceof Integer) {
                    return new NBTPathIndex((int) val);
                } else if(val instanceof TagCompound){
                    return new NBTListMatch((TagCompound) val);
                } else {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown symbol return type: " + val.getClass().getSimpleName(), pattern));
                    throw new EntryParsingException();
                }
            }
            case "NBT_PATH_COMPOUND_MATCH": {
                return new NBTObjectMatch(parseCompound(pattern.find("NBT_COMPOUND"), compiler));
            }
            default: {
                throw new IllegalArgumentException("Unknown NBT path grammar pattern name '" + pattern.getName() + "'");
            }
        }
    }

    public static void analyzeTag(TagCompound compound, PathContext context, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) throw new RuntimeException();
        NoticeType noticeType = compiler.getProperties().has("strict-nbt") &&
                compiler.getProperties().get("strict-nbt").isJsonPrimitive() &&
                compiler.getProperties().get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                compiler.getProperties().get("strict-nbt").getAsBoolean() ?
                NoticeType.ERROR : NoticeType.WARNING;

        String auxiliaryVerb = noticeType == NoticeType.ERROR ? "must" : "should";

        TagCompoundTraverser traverser = new TagCompoundTraverser(compound);

        TagCompoundTraverser.PathContents next = null;
        while((next = (traverser.next())) != null) {
            NBTPath path = next.getPath();
            NBTTag value = next.getValue();

            DataTypeQueryResponse response = compiler.getTypeMap().collectTypeInformation(next.getPath(), context);

            if(!response.isEmpty()) {
                boolean isAGoodBoy = false;
                for(DataType type : response.getPossibleTypes()) {
                    if(type.getCorrespondingTagType() == value.getClass()) {
                        isAGoodBoy = true;

                        TypeFlags flags;
                        if(type instanceof FlatType && (flags = type.getFlags()) != null) {

                            //region (boolean) flag
                            if(flags.hasFlag("boolean") && value instanceof TagByte) {
                                byte byteValue = ((TagByte) value).getValue();
                                if(byteValue != 0 && byteValue != 1) {
                                    compiler.getReport().addNotice(new Notice(noticeType, "Byte at path '" + path + "' is boolean-like; " + auxiliaryVerb + " be either 0b or 1b", pattern));
                                }
                            }
                            //endregion

                            //region type() flags
                            if(!flags.getTypeCategories().isEmpty() && value instanceof TagString) {
                                boolean matched = false;
                                TridentUtil.ResourceLocation location = TridentUtil.ResourceLocation.createStrict(((TagString)value).getValue());
                                if(location == null) {
                                    compiler.getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' is a type; but it doesn't look like a resource location", pattern));
                                    continue;
                                }
                                for(String category : flags.getTypeCategories()) {
                                    Type referencedType = compiler.fetchType(location, category);
                                    if(referencedType != null) {
                                        matched = true;
                                        ((TagString) value).setValue(referencedType.toString());
                                    }
                                }

                                if(!matched) {
                                    if(flags.getTypeCategories().size() > 1) {
                                        compiler.getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be one of the following types: " + flags.getTypeCategories().join(",") + "; but '" + ((TagString) value).getValue() + "' is not a type of any of the previous categories", pattern));
                                    } else {
                                        compiler.getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be of type '" + flags.getTypeCategories().first() + "'. Instead got '" + ((TagString) value).getValue() + "'.", pattern));
                                    }
                                }
                            }
                            //endregion

                            //region one_of flags
                            if(!flags.getStringOptions().isEmpty() && value instanceof TagString) {
                                boolean matched = false;

                                for(String option : flags.getStringOptions()) {
                                    if(option.equals(((TagString) value).getValue())) {
                                        matched = true;
                                        break;
                                    }
                                }
                                if(!matched) {
                                    compiler.getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be one of the following: " + flags.getStringOptions().join(",") + "; instead got '" + ((TagString) value).getValue() + "'", pattern));
                                }
                            }
                            //endregion
                        }

                        break;
                    }
                }
                if(!isAGoodBoy) {
                    if(response.getPossibleTypes().size() > 1) {
                        compiler.getReport().addNotice(new Notice(noticeType, "Data type at path '" + path + "' " + auxiliaryVerb + " be one of the following: " + response.getPossibleTypes().map(DataType::getShortTypeName).toList().join(", "), pattern));
                    } else {
                        compiler.getReport().addNotice(new Notice(noticeType, "Data type at path '" + path + "' " + auxiliaryVerb + " be of type " + response.getPossibleTypes().toList().get(0).getShortTypeName(), pattern));
                    }
                }
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + next.getPath() + "'. Consider adding it to the type map", pattern));
            }
        }
    }
}

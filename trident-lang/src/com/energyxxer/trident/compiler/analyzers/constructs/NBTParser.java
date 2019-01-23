package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.extensions.EObject;
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
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NBTParser {
    public static TagCompound parseCompound(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;
        NBTTag value = parseValue(pattern, file);
        if(value instanceof TagCompound) return ((TagCompound) value);
        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type TagCompound", pattern, file);
    }

    public static NBTTag parseValue(TokenPattern<?> pattern, TridentFile file) {
        switch(pattern.getName()) {
            case "NBT_VALUE": {
                return parseValue(((TokenStructure)pattern).getContents(), file);
            }
            case "NBT_COMPOUND": {
                return parseValue(((TokenStructure)pattern).getContents(), file);
            }
            case "INTERPOLATION_BLOCK": {
                NBTTag result = InterpolationManager.parse(pattern, file, NBTTag.class);
                EObject.assertNotNull(result, pattern, file);
                return result;
            }
            case "NBT_COMPOUND_GROUP": {
                TagCompound compound = new TagCompound();
                TokenList entries = (TokenList) pattern.find("NBT_COMPOUND_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (inner instanceof TokenGroup) {
                            String key = CommonParsers.parseStringLiteralOrIdentifierA(inner.find("NBT_KEY.STRING_LITERAL_OR_IDENTIFIER_A"), file);
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), file);
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
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), file);
                            list.add(value);
                        }
                    }
                }
                return list;
            }
            case "NBT_BYTE_ARRAY": {
                TagByteArray arr = new TagByteArray();
                TokenList entries = (TokenList) pattern.find("..NBT_ARRAY_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (!inner.getName().equals("COMMA")) {
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), file);
                            if(value instanceof TagByte) {
                                arr.add(value);
                            } else {
                                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Byte in TAG_Byte_Array, instead got " + value.getType(), inner, file);
                            }
                        }
                    }
                }
                return arr;
            }
            case "NBT_INT_ARRAY": {
                TagIntArray arr = new TagIntArray();
                TokenList entries = (TokenList) pattern.find("..NBT_ARRAY_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (!inner.getName().equals("COMMA")) {
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), file);
                            if(value instanceof TagInt) {
                                arr.add(value);
                            } else {
                                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Int in TAG_Int_Array, instead got " + value.getType(), inner, file);
                            }
                        }
                    }
                }
                return arr;
            }
            case "NBT_LONG_ARRAY": {
                TagLongArray arr = new TagLongArray();
                TokenList entries = (TokenList) pattern.find("..NBT_ARRAY_ENTRIES");
                if(entries != null) {
                    for (TokenPattern<?> inner : entries.getContents()) {
                        if (!inner.getName().equals("COMMA")) {
                            NBTTag value = parseValue(inner.find("NBT_VALUE"), file);
                            if(value instanceof TagLong) {
                                arr.add(value);
                            } else {
                                throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Long in TAG_Long_Array, instead got " + value.getType(), inner, file);
                            }
                        }
                    }
                }
                return arr;
            }
            case "BOOLEAN": {
                return new TagByte(pattern.flattenTokens().get(0).value.equals("true") ? 1 : 0);
            }
            case "RAW_STRING":
            case "STRING": {
                return new TagString(CommonParsers.parseStringLiteral(pattern, file));
            }
            case "NBT_NUMBER": {
                String flat = pattern.flattenTokens().get(0).value;

                final Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?)([bdfsL]?)", Pattern.CASE_INSENSITIVE);

                Matcher matcher = regex.matcher(flat);
                matcher.lookingAt(); //must be true

                String numberPart = matcher.group(1);
                switch(matcher.group(3).toLowerCase()) {
                    case "": {
                        return (numberPart.contains(".")) ?
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
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, file);
            }
        }
    }

    /**
     * NBTParser should not be instantiated.
     * */
    private NBTParser() {
    }

    public static NBTPath parsePath(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;
        if((((TokenStructure)pattern).getContents()).getName().equals("INTERPOLATION_BLOCK")) {
            NBTPath result = InterpolationManager.parse(((TokenStructure) pattern).getContents(), file, NBTPath.class);
            EObject.assertNotNull(result, pattern, file);
            return result;
        }
        NBTPathNode start = parsePathNode(pattern.find("NBT_PATH_NODE"), file);
        ArrayList<NBTPathNode> nodes = new ArrayList<>();
        nodes.add(start);

        TokenList otherNodes = (TokenList) pattern.find("OTHER_NODES");
        if(otherNodes != null) {
            for(TokenPattern<?> node : otherNodes.getContents()) {
                nodes.add(parsePathNode(node.find("NBT_PATH_NODE"), file));
            }
        }

        return new NBTPath(nodes.toArray(new NBTPathNode[0]));
    }

    private static NBTPathNode parsePathNode(TokenPattern<?> pattern, TridentFile file) {
        switch(pattern.getName()) {
            case "NBT_PATH_NODE": {
                return parsePathNode(((TokenStructure) pattern).getContents(), file);
            }
            case "NBT_PATH_KEY": {
                return new NBTPathKey(CommonParsers.parseStringLiteralOrIdentifierA(pattern.find("NBT_PATH_KEY_LABEL.STRING_LITERAL_OR_IDENTIFIER_D"), file));
            }
            case "NBT_PATH_LIST_ALL": {
                return new NBTListMatch();
            }
            case "NBT_PATH_INDEX": {
                return new NBTPathIndex(CommonParsers.parseInt(pattern.find("INTEGER"), file));
            }
            case "NBT_PATH_LIST_MATCH": {
                return new NBTListMatch(parseCompound(pattern.find("NBT_COMPOUND"), file));
            }
            case "NBT_PATH_LIST_UNKNOWN": {
                Object val = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), file, Integer.class, TagCompound.class);
                if(val == null) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Unexpected null at path", pattern, file);
                } else if(val instanceof Integer) {
                    return new NBTPathIndex((int) val);
                } else if(val instanceof TagCompound){
                    return new NBTListMatch((TagCompound) val);
                } else {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown symbol return type: " + val.getClass().getSimpleName(), pattern, file);
                }
            }
            case "NBT_PATH_COMPOUND_MATCH": {
                return new NBTObjectMatch(parseCompound(pattern.find("NBT_COMPOUND"), file));
            }
            default: {
                throw new IllegalArgumentException("Unknown NBT path grammar pattern name '" + pattern.getName() + "'");
            }
        }
    }

    public static void analyzeTag(TagCompound compound, PathContext context, TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) throw new RuntimeException();
        NoticeType noticeType = file.getCompiler().getProperties().has("strict-nbt") &&
                file.getCompiler().getProperties().get("strict-nbt").isJsonPrimitive() &&
                file.getCompiler().getProperties().get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                file.getCompiler().getProperties().get("strict-nbt").getAsBoolean() ?
                NoticeType.ERROR : NoticeType.WARNING;

        String auxiliaryVerb = noticeType == NoticeType.ERROR ? "must" : "should";

        TagCompoundTraverser traverser = new TagCompoundTraverser(compound);

        TagCompoundTraverser.PathContents next = null;
        while((next = (traverser.next())) != null) {
            NBTPath path = next.getPath();
            NBTTag value = next.getValue();

            DataTypeQueryResponse response = file.getCompiler().getTypeMap().collectTypeInformation(next.getPath(), context);

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
                                    file.getCompiler().getReport().addNotice(new Notice(noticeType, "Byte at path '" + path + "' is boolean-like; " + auxiliaryVerb + " be either 0b or 1b", pattern));
                                }
                            }
                            //endregion

                            //region type() flags
                            if(!flags.getTypeCategories().isEmpty() && value instanceof TagString) {
                                boolean matched = false;
                                TridentUtil.ResourceLocation location = TridentUtil.ResourceLocation.createStrict(((TagString)value).getValue());
                                if(location == null) {
                                    file.getCompiler().getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' is a type; but it doesn't look like a resource location", pattern));
                                    continue;
                                }
                                for(String category : flags.getTypeCategories()) {
                                    Type referencedType = file.getCompiler().fetchType(location, category);
                                    if(referencedType != null) {
                                        matched = true;
                                        ((TagString) value).setValue(referencedType.toString());
                                    }
                                }

                                if(!matched) {
                                    if(flags.getTypeCategories().size() > 1) {
                                        file.getCompiler().getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be one of the following types: " + flags.getTypeCategories().stream().collect(Collectors.joining(", ")) + "; but '" + ((TagString) value).getValue() + "' is not a type of any of the previous categories", pattern));
                                    } else {
                                        file.getCompiler().getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be of type '" + flags.getTypeCategories().stream().findFirst().get() + "'. Instead got '" + ((TagString) value).getValue() + "'.", pattern));
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
                                    file.getCompiler().getReport().addNotice(new Notice(noticeType, "String at path '" + path + "' " + auxiliaryVerb + " be one of the following: " + flags.getStringOptions().stream().collect(Collectors.joining(", ")) + "; instead got '" + ((TagString) value).getValue() + "'", pattern));
                                }
                            }
                            //endregion
                        }

                        break;
                    }
                }
                if(!isAGoodBoy) {
                    if(response.getPossibleTypes().size() > 1) {
                        file.getCompiler().getReport().addNotice(new Notice(noticeType, "Data type at path '" + path + "' " + auxiliaryVerb + " be one of the following: " + response.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")), pattern));
                    } else {
                        file.getCompiler().getReport().addNotice(new Notice(noticeType, "Data type at path '" + path + "' " + auxiliaryVerb + " be of type " + response.getPossibleTypes().stream().findFirst().get().getShortTypeName(), pattern));
                    }
                }
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + next.getPath() + "'. Consider adding it to the type map", pattern));
            }
        }
    }
}

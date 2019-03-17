package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommodoreException;
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
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class NBTParser {
    public static TagCompound parseCompound(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        NBTTag value = parseValue(pattern, ctx);
        if(value instanceof TagCompound) return ((TagCompound) value);
        throw new TridentException(TridentException.Source.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' does not contain a value of type TagCompound", pattern, ctx);
    }

    public static NBTTag parseValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            switch(pattern.getName()) {
                case "NBT_VALUE": {
                    return parseValue(((TokenStructure)pattern).getContents(), ctx);
                }
                case "NBT_COMPOUND": {
                    return parseValue(((TokenStructure)pattern).getContents(), ctx);
                }
                case "INTERPOLATION_BLOCK": {
                    NBTTag result = InterpolationManager.parse(pattern, ctx, NBTTag.class);
                    EObject.assertNotNull(result, pattern, ctx);
                    return result;
                }
                case "NBT_COMPOUND_GROUP": {
                    TagCompound compound = new TagCompound();
                    TokenList entries = (TokenList) pattern.find("NBT_COMPOUND_ENTRIES");
                    if(entries != null) {
                        for (TokenPattern<?> inner : entries.getContents()) {
                            if (inner instanceof TokenGroup) {
                                String key = CommonParsers.parseStringLiteralOrIdentifierA(inner.find("NBT_KEY.STRING_LITERAL_OR_IDENTIFIER_A"), ctx);
                                NBTTag value = parseValue(inner.find("NBT_VALUE"), ctx);
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
                                NBTTag value = parseValue(inner.find("NBT_VALUE"), ctx);
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
                                NBTTag value = parseValue(inner.find("NBT_VALUE"), ctx);
                                if(value instanceof TagByte) {
                                    arr.add(value);
                                } else {
                                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Byte in TAG_Byte_Array, instead got " + value.getType(), inner, ctx);
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
                                NBTTag value = parseValue(inner.find("NBT_VALUE"), ctx);
                                if(value instanceof TagInt) {
                                    arr.add(value);
                                } else {
                                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Int in TAG_Int_Array, instead got " + value.getType(), inner, ctx);
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
                                NBTTag value = parseValue(inner.find("NBT_VALUE"), ctx);
                                if(value instanceof TagLong) {
                                    arr.add(value);
                                } else {
                                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected TAG_Long in TAG_Long_Array, instead got " + value.getType(), inner, ctx);
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
                    return new TagString(CommonParsers.parseStringLiteral(pattern, ctx));
                }
                case "NBT_NUMBER": {
                    String flat = pattern.flattenTokens().get(0).value;

                    Matcher matcher = TridentLexerProfile.NUMBER_REGEX.matcher(flat);
                    matcher.lookingAt(); //must be true

                    String numberPart = matcher.group(1);
                    try {
                        switch (matcher.group(3).toLowerCase()) {
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
                    } catch(NumberFormatException x) {
                        NumericNBTType expectedType = matcher.group(3).length() == 0 && numberPart.contains(".") ? NumericNBTType.DOUBLE : NumericNBTType.getTypeForSuffix(matcher.group(3));
                        String baseError = "Numeric value out of range: " + numberPart + " for a number of type " + expectedType.toString().toLowerCase() + ".";
                        if(ctx.getCompiler().getProperties().has("strict-nbt") &&
                                ctx.getCompiler().getProperties().get("strict-nbt").isJsonPrimitive() &&
                                ctx.getCompiler().getProperties().get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                                ctx.getCompiler().getProperties().get("strict-nbt").getAsBoolean()) {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, baseError, pattern, ctx);
                        } else {
                            ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, baseError + " Interpreting as String: \"" + flat + "\"", pattern));
                            return new TagString(flat);
                        }
                    }
                }
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
                }
            }
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }

    /**
     * NBTParser should not be instantiated.
     * */
    private NBTParser() {
    }

    public static NBTPath parsePath(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "NBT_PATH": {
                return parsePath(((TokenStructure) pattern).getContents(), ctx);
            }
            case "INTERPOLATION_BLOCK": {
                return InterpolationManager.parse(((TokenStructure) pattern).getContents(), ctx, NBTPath.class);
            }
            case "NBT_PATH_ROOT_WRAPPER": {
                NBTPathNode root = parsePathNode(pattern.find("NBT_PATH_ROOT"), ctx);
                ArrayList<NBTPathNode> nodes = new ArrayList<>();
                nodes.add(root);

                TokenList otherNodes = (TokenList) pattern.find("NBT_PATH_NODE_SEQUENCE");
                if(otherNodes != null) {
                    for(TokenPattern<?> rawNode : otherNodes.getContents()) {
                        NBTPathNode node = parsePathNode(rawNode, ctx);
                        if(node != null) nodes.add(node);
                    }
                }

                return new NBTPath(nodes.toArray(new NBTPathNode[0]));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    private static NBTPathNode parsePathNode(TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(pattern.getName()) {
            case "NBT_PATH_ROOT":
            case "NBT_PATH_NODE": {
                return parsePathNode(((TokenStructure) pattern).getContents(), ctx);
            }
            case "NBT_PATH_TRAILING_DOT": {
                return null;
            }
            case "NBT_PATH_COMPOUND_ROOT": {
                return new NBTPathCompoundRoot(parseCompound(pattern.find("NBT_COMPOUND"), ctx));
            }
            case "NBT_PATH_KEY": {
                TagCompound compoundMatch = NBTParser.parseCompound(pattern.find("NBT_PATH_COMPOUND_MATCH.NBT_COMPOUND"), ctx);
                return new NBTPathKey(CommonParsers.parseStringLiteralOrIdentifierA(pattern.find("NBT_PATH_KEY_LABEL.STRING_LITERAL_OR_IDENTIFIER_D"), ctx), compoundMatch);
            }
            case "NBT_PATH_LIST_ACCESS": {
                TokenStructure content = ((TokenStructure) pattern.find("NBT_PATH_LIST_CONTENT"));
                if(content == null) return new NBTListMatch();
                TokenPattern<?> contentInner = content.getContents();
                switch(contentInner.getName()) {
                    case "INTEGER": return new NBTPathIndex(CommonParsers.parseInt(contentInner, ctx));
                    case "NBT_COMPOUND": return new NBTListMatch(parseCompound(contentInner, ctx));
                    case "INTERPOLATION_BLOCK": {
                        Object val = InterpolationManager.parse(contentInner, ctx, Integer.class, TagCompound.class);
                        if(val instanceof Integer) {
                            return new NBTPathIndex((int) val);
                        } else if(val instanceof TagCompound){
                            return new NBTListMatch((TagCompound) val);
                        } else {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown symbol return type: " + val.getClass().getSimpleName(), pattern, ctx);
                        }
                    }
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static void analyzeTag(TagCompound compound, PathContext context, TokenPattern<?> pattern, ISymbolContext file) {
        if(pattern == null) throw new RuntimeException();

        ReportDelegate delegate = new ReportDelegate(file, file.getCompiler().getProperties().has("strict-nbt") &&
                file.getCompiler().getProperties().get("strict-nbt").isJsonPrimitive() &&
                file.getCompiler().getProperties().get("strict-nbt").getAsJsonPrimitive().isBoolean() &&
                file.getCompiler().getProperties().get("strict-nbt").getAsBoolean(), pattern);

        TagCompoundTraverser traverser = new TagCompoundTraverser(compound);

        TagCompoundTraverser.PathContents next;
        while((next = (traverser.next())) != null) {
            NBTPath path = next.getPath();
            NBTTag value = next.getValue();

            DataTypeQueryResponse response = file.getCompiler().getTypeMap().collectTypeInformation(next.getPath(), context);

            if(!response.isEmpty()) {
                boolean matchesType = false;
                for(DataType type : response.getPossibleTypes()) {
                    if(type.getCorrespondingTagType() == value.getClass()) {
                        matchesType = true;

                        TypeFlags flags;
                        if(type instanceof FlatType && (flags = type.getFlags()) != null) {

                            //region (boolean) flag
                            if(flags.hasFlag("boolean") && value instanceof TagByte) {
                                byte byteValue = ((TagByte) value).getValue();
                                if(byteValue != 0 && byteValue != 1) {
                                    delegate.report("Byte at path '" + path + "' is boolean-like; %s be either 0b or 1b");
                                }
                            }
                            //endregion

                            //region type() flags
                            if(!flags.getTypeCategories().isEmpty() && value instanceof TagString) {
                                boolean matched = false;
                                TridentUtil.ResourceLocation location = TridentUtil.ResourceLocation.createStrict(((TagString)value).getValue());
                                if(location == null) {
                                    delegate.report("String at path '" + path + "' is a type; but it doesn't look like a resource location");
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
                                        delegate.report("String at path '" + path + "' %s be one of the following types: " + String.join(", ", flags.getTypeCategories()) + "; but '" + ((TagString) value).getValue() + "' is not a type of any of the previous categories");
                                    } else {
                                        delegate.report("String at path '" + path + "' %s be of type '" + flags.getTypeCategories().toArray(new String[0])[0] + "'. Instead got '" + ((TagString) value).getValue() + "'.");
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
                                    delegate.report("String at path '" + path + "' %s be one of the following: " + String.join(", ", flags.getStringOptions()) + "; instead got '" + ((TagString) value).getValue() + "'");
                                }
                            }
                            //endregion
                        }

                        break;
                    }
                }
                if(!matchesType) {
                    if(response.getPossibleTypes().size() > 1) {
                        delegate.report("Data type at path '" + path + "' %s be one of the following: " + response.getPossibleTypes().stream().map(DataType::getShortTypeName).collect(Collectors.joining(", ")));
                    } else {
                        delegate.report("Data type at path '" + path + "' %s be of type " + response.getPossibleTypes().toArray(new DataType[0])[0].getShortTypeName());
                    }
                }
            } else {
                if(delegate.strict) {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "Unknown data type for path '" + next.getPath() + "'. Consider adding it to the type map", pattern));
                }
            }
        }
    }


    static class ReportDelegate {
        private boolean strict;
        private TokenPattern<?> pattern;
        private ISymbolContext file;

        private String auxiliaryVerb;

        ReportDelegate(ISymbolContext file, boolean strict, TokenPattern<?> pattern) {
            this.strict = strict;
            this.pattern = pattern;
            this.file = file;

            auxiliaryVerb = strict ? "must" : "should";
        }

        public void report(String message) {
            message = message.replace("%s", auxiliaryVerb);
            if(this.strict) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, message, pattern, file);
            } else {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, message, pattern));
            }
        }
    }
}

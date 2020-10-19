package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

public class StringTypeHandler implements TypeHandler<String> {
    private TypeHandlerMemberCollection<String> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.putMethod(StringTypeHandler.class.getMethod("substring", int.class, Integer.class, String.class));
            members.putMethod(String.class.getMethod("indexOf", String.class));
            members.putMethod(String.class.getMethod("lastIndexOf", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("split", String.class, Integer.class, String.class, ISymbolContext.class));
            members.putMethod(StringTypeHandler.class.getMethod("splitRegex", String.class, Integer.class, String.class, ISymbolContext.class));
            members.putMethod(StringTypeHandler.class.getMethod("replace", String.class, String.class, String.class));
            members.putMethod("replaceRegex", String.class.getMethod("replaceAll", String.class, String.class));
            members.putMethod("replaceFirst", String.class.getMethod("replaceFirst", String.class, String.class));
            members.putMethod(StringTypeHandler.class.getMethod("toLowerCase", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("toUpperCase", String.class));
            members.putMethod(String.class.getMethod("trim"));
            members.putMethod(String.class.getMethod("startsWith", String.class));
            members.putMethod(String.class.getMethod("endsWith", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("contains", String.class, String.class));
            members.putMethod(String.class.getMethod("matches", String.class));
            members.putMethod(String.class.getMethod("isEmpty"));
            members.putMethod(StringTypeHandler.class.getMethod("isWhitespace", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("isDigit", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("isLetter", String.class));
            members.putMethod(StringTypeHandler.class.getMethod("isLetterOrDigit", String.class));

            members.putReadOnlyField("length", String::length);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final PrismarineTypeSystem typeSystem;

    public StringTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(String str, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(str, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getIndexer(String object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        int realIndex = PrismarineTypeSystem.assertOfClass(index, pattern, ctx, Integer.class);
        if(realIndex < 0 || realIndex >= object.length()) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Index out of bounds: " + index, pattern, ctx);
        }

        return object.charAt(realIndex) + "";
    }

    @Override
    public Object cast(String object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(typeSystem.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(nbt_value)":
            case "primitive(tag_string)":
                return new TagString(object);
            case "primitive(entity)": {
                if(object.isEmpty()) {
                    throw new ClassCastException("Player names cannot be empty");
                    //throw new PrismarineException(PrismarineException.Source.COMMAND_ERROR, "Player names cannot be empty", pattern, ctx);
                } else if(object.contains(" ")) {
                    throw new ClassCastException("Player names may not contain whitespace");
                    //throw new PrismarineException(PrismarineException.Source.COMMAND_ERROR, "Player names may not contain whitespaces", pattern, ctx);
                } else {
                    return new PlayerName(object);
                }
            }
            case "primitive(resource)":
                try {
                    return CommonParsers.parseResourceLocation(object, pattern, ctx);
                } catch(PrismarineException x) {
                    throw new ClassCastException(x.getNotice().getMessage());
                }
            case "primitive(text_component)":
                return new StringTextComponent(object);
        }
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(String str, ISymbolContext ctx) {
        return new Iterator<Object>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < str.length()-1;
            }

            @Override
            public Object next() {
                if(!hasNext()) return null;
                return "" + str.charAt(i++);
            }
        };
    }

    @Override
    public Class<String> getHandledClass() {
        return String.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "string";
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static String substring(int beginIndex, @NativeFunctionAnnotations.NullableArg Integer endIndex, @NativeFunctionAnnotations.ThisArg String thiz) {
        return (endIndex != null) ? thiz.substring(beginIndex, endIndex) : thiz.substring(beginIndex);
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static ListObject split(String delimiter, @NativeFunctionAnnotations.NullableArg Integer limit, @NativeFunctionAnnotations.ThisArg String thiz, ISymbolContext ctx) {
        if(limit == null) limit = 0;
        String[] parts = thiz.split(Pattern.quote(delimiter), limit);
        ListObject list = new ListObject(ctx.getTypeSystem());
        for(String part : parts) {
            list.add(part);
        }
        return list;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static ListObject splitRegex(String delimiter, @NativeFunctionAnnotations.NullableArg Integer limit, @NativeFunctionAnnotations.ThisArg String thiz, ISymbolContext ctx) {
        if(limit == null) limit = 0;
        String[] parts = thiz.split(delimiter, limit);
        ListObject list = new ListObject(ctx.getTypeSystem());
        for(String part : parts) {
            list.add(part);
        }
        return list;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static String toLowerCase(@NativeFunctionAnnotations.ThisArg String thiz) {
        return thiz.toLowerCase(Locale.ENGLISH);
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static String toUpperCase(@NativeFunctionAnnotations.ThisArg String thiz) {
        return thiz.toUpperCase(Locale.ENGLISH);
    }

    public static boolean isWhitespace(@NativeFunctionAnnotations.ThisArg String thiz) {
        return Character.isWhitespace(thiz.charAt(0));
    }

    public static boolean isLetter(@NativeFunctionAnnotations.ThisArg String thiz) {
        return Character.isLetter(thiz.charAt(0));
    }

    public static boolean isDigit(@NativeFunctionAnnotations.ThisArg String thiz) {
        return Character.isDigit(thiz.charAt(0));
    }

    public static boolean isLetterOrDigit(@NativeFunctionAnnotations.ThisArg String thiz) {
        return Character.isLetterOrDigit(thiz.charAt(0));
    }

    public static String replace(String find, String replace, @NativeFunctionAnnotations.ThisArg String thiz) {
        return thiz.replace(find, replace);
    }

    public static boolean contains(@NativeFunctionAnnotations.ThisArg String thiz, String sub) {
        return thiz.contains(sub);
    }
}

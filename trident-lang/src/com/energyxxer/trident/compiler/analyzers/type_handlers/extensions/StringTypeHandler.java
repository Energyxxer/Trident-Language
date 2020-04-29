package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.NBTTagTypeHandler;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags.TagStringTypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

@AnalyzerMember(key = "java.lang.String")
public class StringTypeHandler implements TypeHandler<String> {
    private static HashMap<String, MemberWrapper<String>> members = new HashMap<>();

    static {
        try {
            members.put("substring", new MethodWrapper<String>("substring", (instance, params) -> instance.substring((int)params[0], params[1] != null ? (int)params[1] : instance.length()), Integer.class, Integer.class).setNullable(1));
            members.put("indexOf", new MethodWrapper<>(String.class.getMethod("indexOf", String.class)));
            members.put("lastIndexOf", new MethodWrapper<>(String.class.getMethod("lastIndexOf", String.class)));
            members.put("split", new MethodWrapper<String>("split", (instance, params) -> instance.split(Pattern.quote((String) params[0]), params[1] != null ? (int)params[1] : 0), String.class, Integer.class).setNullable(1));
            members.put("splitRegex", new MethodWrapper<String>("splitRegex", (instance, params) -> instance.split(((String) params[0]), params[1] != null ? (int)params[1] : 0), String.class, Integer.class).setNullable(1));
            members.put("replace", new MethodWrapper<>(String.class.getMethod("replace", CharSequence.class, CharSequence.class)));
            members.put("replaceRegex", new MethodWrapper<>(String.class.getMethod("replaceAll", String.class, String.class)));
            members.put("replaceFirst", new MethodWrapper<>("replaceFirst", (instance, params) -> instance.replaceFirst((String)params[0], (String)params[1]), String.class, String.class));
            members.put("toLowerCase", new MethodWrapper<>("toLowerCase", (instance, params) -> instance.toLowerCase(Locale.ENGLISH)));
            members.put("toUpperCase", new MethodWrapper<>("toUpperCase", (instance, params) -> instance.toUpperCase(Locale.ENGLISH)));
            members.put("trim", new MethodWrapper<>(String.class.getMethod("trim")));
            members.put("startsWith", new MethodWrapper<>(String.class.getMethod("startsWith", String.class)));
            members.put("endsWith", new MethodWrapper<>(String.class.getMethod("endsWith", String.class)));
            members.put("contains", new MethodWrapper<>(String.class.getMethod("contains", CharSequence.class)));
            members.put("matches", new MethodWrapper<>(String.class.getMethod("matches", String.class)));
            members.put("isEmpty", new MethodWrapper<>(String.class.getMethod("isEmpty")));
            members.put("isWhitespace", new MethodWrapper<>("isWhitespace", (instance, params) -> Character.isWhitespace(instance.charAt(0))));
            members.put("isDigit", new MethodWrapper<>("isDigit", (instance, params) -> Character.isDigit(instance.charAt(0))));
            members.put("isLetter", new MethodWrapper<>("isLetter", (instance, params) -> Character.isLetter(instance.charAt(0))));
            members.put("isLetterOrDigit", new MethodWrapper<>("isLetterOrDigit", (instance, params) -> Character.isLetterOrDigit(instance.charAt(0))));

            members.put("length", new FieldWrapper<>(String::length));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Object getMember(String str, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<String> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(str);
    }

    @Override
    public Object getIndexer(String object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        int realIndex = TridentMethod.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);
        if(realIndex < 0 || realIndex >= object.length()) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index, pattern, ctx);
        }

        return object.charAt(realIndex) + "";
    }

    @Override
    public Object cast(String object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(TridentTypeManager.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(nbt_value)":
            case "primitive(tag_string)":
                return new TagString(object);
            case "primitive(entity)": {
                if(object.isEmpty()) {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "Player names cannot be empty", pattern, ctx);
                } else if(object.contains(" ")) {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, "Player names may not contain whitespaces", pattern, ctx);
                } else {
                    return new PlayerName(object);
                }
            }
            case "primitive(resource)":
                return CommonParsers.parseResourceLocation(object, pattern, ctx);
            case "primitive(text_component)":
                return new StringTextComponent(object);
        }
        throw new ClassCastException();
    }

    @Override
    public Object coerce(String object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        //TODO remove these coerces. Not necessary, and could lead to unintended JS-style behavior
        //Leaving these here to test the coercion system
        switch(TridentTypeManager.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(nbt_value)":
            case "primitive(tag_string)":
                return new TagString(object);
            case "primitive(text_component)":
                return new StringTextComponent(object);
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into) {
        return object instanceof String && (
                    into instanceof TagStringTypeHandler ||
                    into instanceof NBTTagTypeHandler ||
                    into instanceof TextComponentTypeHandler
        );
    }

    @Override
    public Iterator<?> getIterator(String str) {
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
}

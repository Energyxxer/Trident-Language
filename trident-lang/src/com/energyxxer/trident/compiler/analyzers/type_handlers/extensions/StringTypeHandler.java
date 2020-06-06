package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
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
            members.put("substring", new NativeMethodWrapper<String>("substring", (instance, params) -> instance.substring((int)params[0], params[1] != null ? (int)params[1] : instance.length()), Integer.class, Integer.class).setNullable(1));
            members.put("indexOf", new NativeMethodWrapper<>(String.class.getMethod("indexOf", String.class)));
            members.put("lastIndexOf", new NativeMethodWrapper<>(String.class.getMethod("lastIndexOf", String.class)));
            members.put("split", new NativeMethodWrapper<String>("split", (instance, params) -> instance.split(Pattern.quote((String) params[0]), params[1] != null ? (int)params[1] : 0), String.class, Integer.class).setNullable(1));
            members.put("splitRegex", new NativeMethodWrapper<String>("splitRegex", (instance, params) -> instance.split(((String) params[0]), params[1] != null ? (int)params[1] : 0), String.class, Integer.class).setNullable(1));
            members.put("replace", new NativeMethodWrapper<>(String.class.getMethod("replace", CharSequence.class, CharSequence.class)));
            members.put("replaceRegex", new NativeMethodWrapper<>(String.class.getMethod("replaceAll", String.class, String.class)));
            members.put("replaceFirst", new NativeMethodWrapper<>("replaceFirst", (instance, params) -> instance.replaceFirst((String)params[0], (String)params[1]), String.class, String.class));
            members.put("toLowerCase", new NativeMethodWrapper<>("toLowerCase", (instance, params) -> instance.toLowerCase(Locale.ENGLISH)));
            members.put("toUpperCase", new NativeMethodWrapper<>("toUpperCase", (instance, params) -> instance.toUpperCase(Locale.ENGLISH)));
            members.put("trim", new NativeMethodWrapper<>(String.class.getMethod("trim")));
            members.put("startsWith", new NativeMethodWrapper<>(String.class.getMethod("startsWith", String.class)));
            members.put("endsWith", new NativeMethodWrapper<>(String.class.getMethod("endsWith", String.class)));
            members.put("contains", new NativeMethodWrapper<>(String.class.getMethod("contains", CharSequence.class)));
            members.put("matches", new NativeMethodWrapper<>(String.class.getMethod("matches", String.class)));
            members.put("isEmpty", new NativeMethodWrapper<>(String.class.getMethod("isEmpty")));
            members.put("isWhitespace", new NativeMethodWrapper<>("isWhitespace", (instance, params) -> Character.isWhitespace(instance.charAt(0))));
            members.put("isDigit", new NativeMethodWrapper<>("isDigit", (instance, params) -> Character.isDigit(instance.charAt(0))));
            members.put("isLetter", new NativeMethodWrapper<>("isLetter", (instance, params) -> Character.isLetter(instance.charAt(0))));
            members.put("isLetterOrDigit", new NativeMethodWrapper<>("isLetterOrDigit", (instance, params) -> Character.isLetterOrDigit(instance.charAt(0))));

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
        int realIndex = TridentFunction.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);
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

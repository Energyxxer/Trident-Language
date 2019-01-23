package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "java.lang.String")
public class StringType implements VariableTypeHandler<java.lang.String> {
    private static HashMap<String, MemberWrapper<String>> members = new HashMap<>();

    static {
        members.put("substring", instance -> (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 1 || params.length > 2) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'substring' requires 1 or 2 parameters, instead found " + params.length, pattern, file);
            }

            int start = VariableMethod.HelperMethods.assertOfType(params[0], patterns[0], file, Integer.class);
            int end = params.length >= 2 ? VariableMethod.HelperMethods.assertOfType(params[1], patterns[1], file, Integer.class) : instance.length();

            try {
                return instance.substring(start, end);
            } catch(IndexOutOfBoundsException x) {
                throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.getMessage(), pattern, file);
            }
        });

        try {
            members.put("indexOf", new MethodWrapper<>(String.class.getMethod("indexOf", String.class)));
            members.put("lastIndexOf", new MethodWrapper<>(String.class.getMethod("lastIndexOf", String.class)));
            members.put("split", new MethodWrapper<>("split", (instance, params) -> instance.split(Pattern.quote((String)params[0])), String.class));
            members.put("replace", new MethodWrapper<>(String.class.getMethod("replace", CharSequence.class, CharSequence.class)));
            members.put("replaceFirst", new MethodWrapper<>("replaceFirst", (instance, params) -> instance.replaceFirst(Pattern.quote((String)params[0]), (String)params[1]), String.class, String.class));
            members.put("toUpperCase", new MethodWrapper<>("toUpperCase", (instance, params) -> instance.toUpperCase(Locale.ENGLISH)));
            members.put("toLowerCase", new MethodWrapper<>("toLowerCase", (instance, params) -> instance.toLowerCase(Locale.ENGLISH)));
            members.put("trim", new MethodWrapper<>(String.class.getMethod("trim")));
            members.put("startsWith", new MethodWrapper<>(String.class.getMethod("startsWith", String.class)));
            members.put("endsWith", new MethodWrapper<>(String.class.getMethod("endsWith", String.class)));
            members.put("contains", new MethodWrapper<>(String.class.getMethod("contains", CharSequence.class)));
            members.put("matches", new MethodWrapper<>(String.class.getMethod("matches", String.class)));

            members.put("length", new FieldWrapper<>(String::length));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Object getMember(String str, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        MemberWrapper<String> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(str);
    }

    @Override
    public Object getIndexer(String object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        int realIndex = assertOfType(index, pattern, file, Integer.class);
        if(realIndex < 0 || realIndex >= object.length()) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index, pattern, file);
        }

        return object.charAt(realIndex) + "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(String object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        if(targetType == NBTTag.class || targetType == TagString.class) {
            return (F) new TagString(object);
        }
        if(targetType == Entity.class) {
            if(object.isEmpty()) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Player names cannot be empty", pattern, file);
            } else if(object.contains(" ")) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Player names may not contain whitespaces", pattern, file);
            } else {
                return (F) new PlayerName(object);
            }
        }
        if(targetType == TridentUtil.ResourceLocation.class) {
            TridentUtil.ResourceLocation loc = TridentUtil.ResourceLocation.createStrict(object);
            if(loc == null) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Illegal resource location path: " + object, pattern, file);
            }
            return (F) loc;
        }
        if(targetType == TextComponent.class) {
            return (F) new StringTextComponent(object);
        }
        throw new ClassCastException();
    }
}

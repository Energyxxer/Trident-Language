package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "java.lang.String")
public class StringType implements VariableTypeHandler<java.lang.String> {
    private static HashMap<String, MemberWrapper<String>> members = new HashMap<>();

    static {
        members.put("substring", instance -> (VariableMethod) (params, patterns, pattern, file) -> {
            if(params.length < 1 || params.length > 2) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'substring' requires 1 or 2 parameters, instead found " + params.length, pattern));
                throw new EntryParsingException();
            }

            int start = VariableMethod.HelperMethods.assertOfType(params[0], patterns[0], file, Integer.class);
            int end = params.length >= 2 ? VariableMethod.HelperMethods.assertOfType(params[1], patterns[1], file, Integer.class) : instance.length();

            try {
                return instance.substring(start, end);
            } catch(IndexOutOfBoundsException x) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
                throw new EntryParsingException();
            }
        });

        try {
            members.put("indexOf", new MethodWrapper<>(String.class.getMethod("indexOf", String.class)));
            members.put("lastIndexOf", new MethodWrapper<>(String.class.getMethod("lastIndexOf", String.class)));
            members.put("split", new MethodWrapper<>("split", (instance, params) -> instance.split(Pattern.quote((String)params[0])), String.class));
            members.put("replace", new MethodWrapper<>(String.class.getMethod("replace", CharSequence.class, CharSequence.class)));
            members.put("toUpperCase", new MethodWrapper<>(String.class.getMethod("toUpperCase")));
            members.put("toLowerCase", new MethodWrapper<>(String.class.getMethod("toLowerCase")));

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
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Index out of bounds: " + index, pattern));
            throw new EntryParsingException();
        }

        return object.charAt(realIndex) + "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(String object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        if(targetType == String.class) return (F) object;
        if(targetType == NBTTag.class || targetType == TagString.class) {
            return (F) new TagString(object);
        }
        if(targetType == Entity.class) {
            if(object.isEmpty()) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Player names cannot be empty", pattern));
                throw new EntryParsingException();
            } else if(object.contains(" ")) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Player names may not contain whitespaces", pattern));
                throw new EntryParsingException();
            } else {
                return (F) new PlayerName(object);
            }
        }
        if(targetType == TridentUtil.ResourceLocation.class) {
            TridentUtil.ResourceLocation loc = TridentUtil.ResourceLocation.createStrict(object);
            if(loc == null) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Illegal resource location path: " + object, pattern));
                throw new EntryParsingException();
            }
            return (F) loc;
        }
        if(targetType == TextComponent.class) {
            return (F) new StringTextComponent(object);
        }
        throw new ClassCastException();
    }
}

package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

import java.util.HashMap;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "java.lang.String")
public class StringType implements VariableTypeHandler<java.lang.String> {
    @Override
    public Object getMember(String str, String member, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        StringDecorator decorator = new StringDecorator(str);
        return decorator.members.get(member);
    }

    @Override
    public Object getIndexer(String object, Object index, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        int realIndex = assertOfType(index, pattern, compiler, Integer.class);
        if(realIndex < 0 || realIndex >= object.length()) {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Index out of bounds: " + index, pattern));
            throw new EntryParsingException();
        }

        return object.charAt(realIndex) + "";
    }

    private class StringDecorator {
        HashMap<String, Object> members = new HashMap<>();

        public StringDecorator(String string) {
            members.put("substring", (VariableMethod)(params, patterns, pattern, compiler) -> {
                if(params.length < 1 || params.length > 2) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'substring' requires 1 or 2 parameters, instead found " + params.length, pattern));
                    throw new EntryParsingException();
                }

                int start = assertOfType(params[0], patterns[0], compiler, Integer.class);
                int end = params.length >= 2 ? assertOfType(params[1], patterns[1], compiler, Integer.class) : string.length();

                try {
                    return string.substring(start, end);
                } catch(IndexOutOfBoundsException x) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
                    throw new EntryParsingException();
                }
            });
            members.put("indexOf", (VariableMethod)(params, patterns, pattern, compiler) -> {
                if(params.length != 1) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'indexOf' requires 1 parameter, instead found " + params.length, pattern));
                    throw new EntryParsingException();
                }

                String str = assertOfType(params[0], patterns[0], compiler, String.class);

                try {
                    return string.indexOf(str);
                } catch(IndexOutOfBoundsException x) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
                    throw new EntryParsingException();
                }
            });
            members.put("split", (VariableMethod)(params, patterns, pattern, compiler) -> {
                if(params.length != 1) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'split' requires 1 parameter, instead found " + params.length, pattern));
                    throw new EntryParsingException();
                }

                String str = assertOfType(params[0], patterns[0], compiler, String.class);

                try {
                    return new ListType((Object[]) string.split(Pattern.quote(str)));
                } catch(IndexOutOfBoundsException x) {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
                    throw new EntryParsingException();
                }
            });
            members.put("length", string.length());
        }
    }

    @Override
    public Object cast(String object, Class targetType, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(targetType == String.class) return object;
        if(targetType == Entity.class) {
            if(object.isEmpty()) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Player names cannot be empty"));
                throw new EntryParsingException();
            } else if(object.contains(" ")) {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Player names may not contain whitespaces"));
                throw new EntryParsingException();
            } else {
                return new PlayerName(object);
            }
        }
        if(targetType == TextComponent.class) {
            return new StringTextComponent(object);
        }
        return null;
    }
}

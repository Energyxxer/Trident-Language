package com.energyxxer.trident.compiler.analyzers.type_handlers.constructors;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.commodore.textcomponents.events.ClickEvent;
import com.energyxxer.commodore.textcomponents.events.HoverEvent;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.extensions.EObject;

import java.util.HashMap;
import java.util.Map;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class ObjectConstructors {
    private static HashMap<String, VariableMethod> constructors = new HashMap<>();

    static {
        constructors.put("int_range",
                new MethodWrapper<>("new int_range", ((instance, params) -> {
                    if(params[0] == null && params[1] == null) {
                        throw new IllegalArgumentException("Both min and max bounds cannot be null");
                    }
                    return new NumberRange<>((Integer)params[0], (Integer)params[1]);
                }), Integer.class, Integer.class).setNullable(0).setNullable(1)
                        .createForInstance(null));

        constructors.put("real_range",
                new MethodWrapper<>("new real_range", ((instance, params) -> {
                    if(params[0] == null && params[1] == null) {
                        throw new IllegalArgumentException("Both min and max bounds cannot be null");
                    }
                    return new NumberRange<>((Double)params[0], (Double)params[1]);
                }), Double.class, Double.class).setNullable(0).setNullable(1)
                        .createForInstance(null));








        constructors.put("text_component", ObjectConstructors::constructTextComponent);
        constructors.put("nbt", ObjectConstructors::constructNBT);
    }

    private static NBTTag constructNBT(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file) {
        if(params.length == 0) return new TagCompound();
        EObject.assertNotNull(params[0], patterns[0], file);

        boolean skipIncompatibleTypes = false;
        if(params.length >= 2) {
            EObject.assertNotNull(params[1], patterns[1], file);
            skipIncompatibleTypes = assertOfType(params[1], patterns[1], file, Boolean.class);
        }

        if(params[0] instanceof NBTTag) return ((NBTTag) params[0]).clone();
        if(params[0] instanceof Number) {
            if(params[0] instanceof Double) {
                return new TagDouble(((double) params[0]));
            } else {
                return new TagInt((int) params[0]);
            }
        } else if(params[0] instanceof String || params[0] instanceof TridentUtil.ResourceLocation || params[0] instanceof TextComponent) {
            return new TagString(params[0].toString());
        } else if(params[0] instanceof Boolean) {
            return new TagByte((boolean)params[0] ? 1 : 0);
        } else if(params[0] instanceof DictionaryObject) {
            TagCompound compound = new TagCompound();

            for(Map.Entry<String, Symbol> obj : ((DictionaryObject) params[0]).entrySet()) {
                NBTTag content = constructNBT(new Object[] {obj.getValue().getValue(), skipIncompatibleTypes}, new TokenPattern[] {patterns[0], pattern}, pattern, file);
                if(content != null) {
                    content.setName(obj.getKey());
                    compound.add(content);
                }
            }

            return compound;
        } if(params[0] instanceof ListType) {
            TagList list = new TagList();

            for(Object obj : ((ListType) params[0])) {
                NBTTag content = constructNBT(new Object[] {obj, skipIncompatibleTypes}, new TokenPattern[] {patterns[0], pattern}, pattern, file);
                if(content != null) {
                    try {
                        list.add(content);
                    } catch(IllegalArgumentException x) {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Error while converting list object to nbt list: " + x.getMessage(), pattern, file);
                    }
                }
            }

            return list;
        } else if(!skipIncompatibleTypes) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot convert object of type '" + VariableTypeHandler.Static.getIdentifierForClass(params[0].getClass()) + "' to an nbt tag", pattern, file);
        } else return null;
    }

    public static VariableMethod getConstructor(String name) {
        return constructors.get(name);
    }

    public static TextComponent constructTextComponent(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file) {
        if (params.length == 0) return new StringTextComponent("");
        EObject.assertNotNull(params[0], patterns[0], file);
        if (params[0] instanceof String) {
            return new StringTextComponent(((String) params[0]));
        } else if (params[0] instanceof DictionaryObject) {
            DictionaryObject dict = ((DictionaryObject) params[0]);

            TextComponent tc = null;

            //region Main component
            if (dict.containsKey("text")) {
                Object textObj = dict.get("text");
                if (textObj != null) {
                    if (textObj instanceof String) {
                        tc = new StringTextComponent(((String) textObj));
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'text'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("translate")) {
                Object textObj = dict.get("translate");
                if (textObj != null) {
                    if (textObj instanceof String) {
                        tc = new TranslateTextComponent(((String) textObj));
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'translate'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("keybind")) {
                Object textObj = dict.get("keybind");
                if (textObj != null) {
                    if (textObj instanceof String) {
                        tc = new KeybindTextComponent(((String) textObj));
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'keybind'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("selector")) {
                Object textObj = dict.get("selector");
                if (textObj != null) {
                    if (textObj instanceof String) {
                        tc = new SelectorTextComponent(new PlayerName((String) textObj));
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'selector'", patterns[0], file);
                    }
                }
            }

            if (tc == null) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know how to turn this into a text component: " + dict.toString(), patterns[0], file);
            }
            //endregion

            //region Text Style
            TextStyle style = new TextStyle(0);
            style.setMask(0);
            if (dict.containsKey("color")) {
                Object colorObj = dict.get("color");
                if (colorObj != null) {
                    if (colorObj instanceof String) {
                        try {
                            style.setColor(TextColor.valueOf(((String) colorObj).toUpperCase()));
                        } catch (IllegalArgumentException x) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Illegal text color '" + colorObj + "'", patterns[0]));
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'color'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("bold")) {
                Object boldObj = dict.get("bold");
                if (boldObj != null) {
                    style.setMask(style.getMask() | TextStyle.BOLD);
                    if (boldObj instanceof Boolean) {
                        if (((Boolean) boldObj)) {
                            style.setFlags((byte) (style.getFlags() | TextStyle.BOLD));
                        } else {
                            style.setFlags((byte) (style.getFlags() & ~TextStyle.BOLD));
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected boolean in text component property 'bold'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("italic")) {
                Object boldObj = dict.get("italic");
                if (boldObj != null) {
                    style.setMask(style.getMask() | TextStyle.ITALIC);
                    if (boldObj instanceof Boolean) {
                        if (((Boolean) boldObj)) {
                            style.setFlags((byte) (style.getFlags() | TextStyle.ITALIC));
                        } else {
                            style.setFlags((byte) (style.getFlags() & ~TextStyle.ITALIC));
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected boolean in text component property 'italic'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("strikethrough")) {
                Object boldObj = dict.get("strikethrough");
                if (boldObj != null) {
                    style.setMask(style.getMask() | TextStyle.STRIKETHROUGH);
                    if (boldObj instanceof Boolean) {
                        if (((Boolean) boldObj)) {
                            style.setFlags((byte) (style.getFlags() | TextStyle.STRIKETHROUGH));
                        } else {
                            style.setFlags((byte) (style.getFlags() & ~TextStyle.STRIKETHROUGH));
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected boolean in text component property 'strikethrough'", patterns[0], file);
                    }
                }
            }
            if (dict.containsKey("obfuscated")) {
                Object boldObj = dict.get("obfuscated");
                if (boldObj != null) {
                    style.setMask(style.getMask() | TextStyle.OBFUSCATED);
                    if (boldObj instanceof Boolean) {
                        if (((Boolean) boldObj)) {
                            style.setFlags((byte) (style.getFlags() | TextStyle.OBFUSCATED));
                        } else {
                            style.setFlags((byte) (style.getFlags() & ~TextStyle.OBFUSCATED));
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected boolean in text component property 'obfuscated'", patterns[0], file);
                    }
                }
            }

            tc.setStyle(style);
            //endregion

            //region Events
            if (dict.containsKey("hoverEvent")) {
                Object evtObj = dict.get("hoverEvent");
                if (evtObj != null) {
                    if (evtObj instanceof DictionaryObject) {
                        HoverEvent.Action action;
                        Object actionObj = ((DictionaryObject) evtObj).get("action");
                        if (actionObj instanceof String) {
                            try {
                                action = HoverEvent.Action.valueOf(((String) actionObj).toUpperCase());
                            } catch (IllegalArgumentException x) {
                                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Illegal hoverEvent action '" + actionObj + "'", patterns[0], file);
                            }
                        } else if (actionObj == null) {
                            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Missing hoverEvent action", patterns[0], file);
                        } else {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in hoverEvent action", patterns[0], file);
                        }

                        TextComponent value = constructTextComponent(new Object[] {((DictionaryObject) evtObj).get("value")}, patterns, pattern, file);
                        tc.addEvent(new HoverEvent(action, value));
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'color'", patterns[0], file);
                    }
                }
            }

            if (dict.containsKey("clickEvent")) {
                Object evtObj = dict.get("clickEvent");
                if (evtObj != null) {
                    if (evtObj instanceof DictionaryObject) {
                        ClickEvent.Action action;
                        Object actionObj = ((DictionaryObject) evtObj).get("action");
                        if (actionObj instanceof String) {
                            try {
                                action = ClickEvent.Action.valueOf(((String) actionObj).toUpperCase());
                            } catch (IllegalArgumentException x) {
                                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Illegal clickEvent action '" + actionObj + "'", patterns[0], file);
                            }
                        } else if (actionObj == null) {
                            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Missing clickEvent action", patterns[0], file);
                        } else {
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in clickEvent action", patterns[0], file);
                        }

                        Object valueObj = ((DictionaryObject) evtObj).get("value");

                        if(valueObj instanceof String) {
                            tc.addEvent(new ClickEvent(action, ((String) valueObj)));
                        } else if(valueObj == null) {
                            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Missing clickEvent value", patterns[0], file);
                        } else {
                            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Expected string in clickEvent value", patterns[0], file);
                        }
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected string in text component property 'color'", patterns[0], file);
                    }
                }
            }
            //endregion

            return tc;
        } else if(params[0] instanceof ListType) {
            ListType list = ((ListType) params[0]);

            ListTextComponent tc = new ListTextComponent();

            for(Object obj : list) {
                tc.append(constructTextComponent(new Object[] {obj}, patterns, pattern, file));
            }

            return tc;
        } else if(params[0] instanceof TextComponent) {
            return ((TextComponent) params[0]);
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot turn a value of type " + VariableTypeHandler.Static.getIdentifierForClass(params[0].getClass()) + " into a text component", patterns[0], file);
        }
    }
}

package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.commodore.textcomponents.events.ClickEvent;
import com.energyxxer.commodore.textcomponents.events.HoverEvent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import static com.energyxxer.trident.compiler.util.Using.using;

public class TextParser {

    public static TextComponent parseTextComponent(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "TEXT_COMPONENT": {
                return parseTextComponent(JsonParser.parseJson(((TokenStructure) pattern).getContents(), compiler), compiler, pattern, TextComponentContext.CHAT);
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown text component production: '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
            }
        }
    }

    private static TextComponent component;

    private static TextComponent parseTextComponent(JsonElement elem, TridentCompiler compiler, TokenPattern<?> pattern, TextComponentContext context) {

        boolean strict = compiler.getProperties().has("strict-text-components") && compiler.getProperties().get("strict-text-components").getAsBooleanOrNull();

        ReportDelegate delegate = new ReportDelegate(compiler, strict, pattern);

        if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isString()) {
            return new StringTextComponent(elem.getAsStringOrNull());
        } else if(elem.isJsonArray()) {
            ListTextComponent list = new ListTextComponent();
            for(JsonElement sub : elem.getAsJsonArray()) {
                list.append(parseTextComponent(sub, compiler, pattern, context));
            }
            return list;
        } else if(elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObjectOrNull();

            component = null;

            if(obj.has("text")) {
                using(obj.get("text").getAsStringOrNull())
                        .notIfNull()
                        .run(t -> component = new StringTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'text'", obj.get("text")));
            } else if(obj.has("translate")) {
                using(obj.get("translate").getAsStringOrNull())
                        .notIfNull()
                        .run(t -> component = new TranslateTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'translate'", obj.get("translate")));
            } else if(obj.has("keybind")) {
                using(obj.get("keybind").getAsStringOrNull())
                        .notIfNull()
                        .run(t -> component = new KeybindTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'keybind'", obj.get("keybind")));
            } else if(obj.has("score")) {
                using(obj.get("score").getAsJsonObjectOrNull()).notIfNull().run(s -> {
                    String name = s.get("name").getAsStringOrNull();
                    if(name == null) delegate.report("Missing 'name' string for 'score' text component", s);
                    String objectiveName = s.get("objective").getAsStringOrNull();
                    if(objectiveName == null) delegate.report("Missing 'objective' string for 'score' text component", s);
                    Objective objective = compiler.getModule().getObjectiveManager().create(objectiveName);
                    component = new ScoreTextComponent(new LocalScore(objective, new PlayerName(name)));
                }).otherwise(v -> delegate.report("Expected object in 'score'", obj.get("score")));
            } else if(obj.has("selector")) {
                using(obj.get("selector").getAsStringOrNull())
                        .notIfNull()
                        .run(t -> component = new SelectorTextComponent(new GenericEntity(new Selector(Selector.BaseSelector.SENDER))))
                        .otherwise(t -> delegate.report("Expected string in 'selector'", obj.get("selector")));
            }
            if(component == null) {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Don't know how to turn this into a text component", pattern));
                throw new EntryParsingException();
            }

            TextStyle style = new TextStyle(0);
            style.setMask(0);
            if(obj.has("color")) {
                try {
                    using(obj.get("color").getAsStringOrNull())
                            .notIfNull()
                            .run(t -> style.setColor(TextColor.valueOf(t.toUpperCase())))
                            .otherwise(t -> delegate.report("Expected string in 'color'", obj.get("color")));
                } catch(IllegalArgumentException x) {
                    delegate.report("Illegal text color '" + obj.get("color").getAsStringOrNull() + "'",
                            "Unknown text color '" + obj.get("color").getAsStringOrNull() + "'", obj.get("color"));
                }
            }
            if(obj.has("bold")) {
                using(obj.get("bold").getAsBooleanOrNull()).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.BOLD);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.BOLD));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.BOLD));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'bold'", obj.get("bold")));
            }
            if(obj.has("italic")) {
                using(obj.get("italic").getAsBooleanOrNull()).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.ITALIC);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.ITALIC));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.ITALIC));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'italic'", obj.get("italic")));
            }
            if(obj.has("strikethrough")) {
                using(obj.get("strikethrough").getAsBooleanOrNull()).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.STRIKETHROUGH);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.STRIKETHROUGH));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.STRIKETHROUGH));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'strikethrough'", obj.get("strikethrough")));
            }
            if(obj.has("obfuscated")) {
                using(obj.get("obfuscated").getAsBooleanOrNull()).notIfNull()
                        .run(v -> {
                            style.setMask(style.getMask() | TextStyle.OBFUSCATED);
                            if(v) {
                                style.setFlags((byte) (style.getFlags() | TextStyle.OBFUSCATED));
                            } else {
                                style.setFlags((byte) ~(~style.getFlags() | TextStyle.OBFUSCATED));
                            }
                        }).otherwise(v -> delegate.report("Expected boolean in 'obfuscated'", obj.get("obfuscated")));
            }
            component.setStyle(style);

            if(obj.has("hoverEvent")) {
                using(obj.getAsJsonObject("hoverEvent")).notIfNull().run(e -> {
                    if(!context.isHoverEnabled()) delegate.report("Hover events are not allowed in this context", "Hover events are not used in this context", e);

                    using(e.get("action").getAsStringOrNull()).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal hover event action '$a'", "Unknown hover event action '$a'", e.get("action")))
                            .run(a -> {
                        HoverEvent.Action action = HoverEvent.Action.valueOf(a.toUpperCase());
                        using(e.get("value")).notIfNull().run(v -> {
                            String value;
                            if(v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                                value = v.getAsStringOrNull();
                            } else {
                                value = (parseTextComponent(v, compiler, pattern, TextComponentContext.TOOLTIP)).toString();
                            }
                            component.addEvent(new HoverEvent(action, value));
                        }).otherwise(v -> delegate.report("Missing hover event value", e));
                    }).otherwise(a -> delegate.report("Missing hover event action", e));
                });
            }
            if(obj.has("clickEvent")) {
                using(obj.getAsJsonObject("clickEvent")).notIfNull().run(e -> {
                    if(!context.isClickEnabled()) delegate.report("Click events are not allowed in this context", "Click events are not used in this context", e);

                    using(e.get("action").getAsStringOrNull()).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal click event action '$a'", "Unknown click event action '$a'", e.get("action")))
                            .run(a -> {
                                ClickEvent.Action action = ClickEvent.Action.valueOf(a.toUpperCase());
                                using(e.get("value")).notIfNull().run(v -> {
                                    String value = v.getAsStringOrNull();
                                    if(value == null) delegate.report("Missing click event value", e);
                                    else component.addEvent(new ClickEvent(action, value));
                                }).otherwise(v -> delegate.report("Missing click event value", e));
                            }).otherwise(a -> delegate.report("Missing click event action", e));
                });
            }

            return component;
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Don't know how to turn this into a text component", pattern));
            throw new EntryParsingException();
        }
    }

    /**
     * TextParser must not be instantiated.
     */
    private TextParser() {

    }
}

class ReportDelegate {
    private TridentCompiler compiler;
    private boolean strict;
    private TokenPattern<?> pattern;

    public ReportDelegate(TridentCompiler compiler, boolean strict, TokenPattern<?> pattern) {
        this.compiler = compiler;
        this.strict = strict;
        this.pattern = pattern;
    }

    public void report(String message) {
        report(message, this.pattern);
    }

    public void report(String message, JsonElement element) {
        report(message, message, JsonParser.getPatternFor(element));
    }

    public void report(String message, TokenPattern<?> pattern) {
        report(message, message, pattern);
    }

    public void report(String strict, String notStrict) {
        report(strict, notStrict, this.pattern);
    }

    public void report(String strict, String notStrict, JsonElement element) {
        report(strict, notStrict, JsonParser.getPatternFor(element));
    }

    public void report(String strict, String notStrict, TokenPattern<?> pattern) {
        if(pattern == null) pattern = this.pattern;
        compiler.getReport().addNotice(new Notice(
                this.strict ? NoticeType.ERROR : NoticeType.WARNING,
                this.strict ? strict : notStrict,
                pattern));
        if(this.strict) throw new EntryParsingException();
    }
}
package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.commodore.textcomponents.events.ClickEvent;
import com.energyxxer.commodore.textcomponents.events.HoverEvent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import static com.energyxxer.trident.extensions.EJsonElement.*;
import static com.energyxxer.trident.compiler.util.Using.using;

public class TextParser {

    public static TextComponent parseTextComponent(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "TEXT_COMPONENT": {
                return parseTextComponent(((TokenStructure)pattern).getContents(), file);
            }
            case "INTERPOLATION_BLOCK": {
                TextComponent result = InterpolationManager.parse(pattern, file, TextComponent.class);
                EObject.assertNotNull(result, pattern, file);
                return result;
            }
            case "JSON_ROOT":
            case "JSON_ELEMENT": {
                return parseTextComponent(JsonParser.parseJson(pattern, file), file, pattern, TextComponentContext.CHAT);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown text component production: '" + pattern.getName() + "'", pattern, file);
            }
        }
    }

    private static TextComponent component;

    public static TextComponent parseTextComponent(JsonElement elem, TridentFile file, TokenPattern<?> pattern, TextComponentContext context) {

        boolean strict = file.getCompiler().getProperties().has("strict-text-components") && getAsBooleanOrNull(file.getCompiler().getProperties().get("strict-text-components"));

        ReportDelegate delegate = new ReportDelegate(file.getCompiler(), strict, pattern, file);

        if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isString()) {
            return new StringTextComponent(getAsStringOrNull(elem));
        } else if(elem.isJsonArray()) {
            ListTextComponent list = new ListTextComponent();
            for(JsonElement sub : elem.getAsJsonArray()) {
                list.append(parseTextComponent(sub, file, pattern, context));
            }
            return list;
        } else if(elem.isJsonObject()) {
            JsonObject obj = getAsJsonObjectOrNull(elem);

            component = null;

            if(obj.has("text")) {
                using(getAsStringOrNull(obj.get("text")))
                        .notIfNull()
                        .run(t -> component = new StringTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'text'", obj.get("text")));
            } else if(obj.has("translate")) {
                using(getAsStringOrNull(obj.get("translate")))
                        .notIfNull()
                        .run(t -> component = new TranslateTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'translate'", obj.get("translate")));
            } else if(obj.has("keybind")) {
                using(getAsStringOrNull(obj.get("keybind")))
                        .notIfNull()
                        .run(t -> component = new KeybindTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'keybind'", obj.get("keybind")));
            } else if(obj.has("score")) {
                using(getAsJsonObjectOrNull(obj.get("score"))).notIfNull().run(s -> {
                    String name = getAsStringOrNull(s.get("name"));
                    if(name == null) delegate.report("Missing 'name' string for 'score' text component", s);
                    String objectiveName = getAsStringOrNull(s.get("objective"));
                    if(objectiveName == null) delegate.report("Missing 'objective' string for 'score' text component", s);
                    Objective objective;
                    if(file.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) objective = file.getCompiler().getModule().getObjectiveManager().get(objectiveName);
                    else objective = file.getCompiler().getModule().getObjectiveManager().create(objectiveName, true);
                    component = new ScoreTextComponent(new LocalScore(objective, new PlayerName(name)));
                }).otherwise(v -> delegate.report("Expected object in 'score'", obj.get("score")));
            } else if(obj.has("selector")) {
                using(getAsStringOrNull(obj.get("selector")))
                        .notIfNull()
                        .run(t -> component = new SelectorTextComponent(new PlayerName(t)))
                        .otherwise(t -> delegate.report("Expected string in 'selector'", obj.get("selector")));
            }
            if(component == null) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know how to turn this into a text component", pattern, file);
            }

            TextStyle style = new TextStyle(0);
            style.setMask(0);
            if(obj.has("color")) {
                try {
                    using(getAsStringOrNull(obj.get("color")))
                            .notIfNull()
                            .run(t -> style.setColor(TextColor.valueOf(t.toUpperCase())))
                            .otherwise(t -> delegate.report("Expected string in 'color'", obj.get("color")));
                } catch(IllegalArgumentException x) {
                    delegate.report("Illegal text color '" + getAsStringOrNull(obj.get("color")) + "'",
                            "Unknown text color '" + getAsStringOrNull(obj.get("color")) + "'", obj.get("color"));
                }
            }
            if(obj.has("bold")) {
                using(getAsBooleanOrNull(obj.get("bold"))).notIfNull()
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
                using(getAsBooleanOrNull(obj.get("italic"))).notIfNull()
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
                using(getAsBooleanOrNull(obj.get("strikethrough"))).notIfNull()
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
                using(getAsBooleanOrNull(obj.get("obfuscated"))).notIfNull()
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

                    using(getAsStringOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal hover event action '$a'", "Unknown hover event action '$a'", e.get("action")))
                            .run(a -> {
                        HoverEvent.Action action = HoverEvent.Action.valueOf(a.toUpperCase());
                        using(e.get("value")).notIfNull().run(v -> {
                            String value;
                            if(v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                                value = getAsStringOrNull(v);
                            } else {
                                value = (parseTextComponent(v, file, pattern, TextComponentContext.TOOLTIP)).toString();
                            }
                            component.addEvent(new HoverEvent(action, value));
                        }).otherwise(v -> delegate.report("Missing hover event value", e));
                    }).otherwise(a -> delegate.report("Missing hover event action", e));
                });
            }
            if(obj.has("clickEvent")) {
                using(obj.getAsJsonObject("clickEvent")).notIfNull().run(e -> {
                    if(!context.isClickEnabled()) delegate.report("Click events are not allowed in this context", "Click events are not used in this context", e);

                    using(getAsStringOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal click event action '$a'", "Unknown click event action '$a'", e.get("action")))
                            .run(a -> {
                                ClickEvent.Action action = ClickEvent.Action.valueOf(a.toUpperCase());
                                using(e.get("value")).notIfNull().run(v -> {
                                    String value = getAsStringOrNull(v);
                                    if(value == null) delegate.report("Missing click event value", e);
                                    else component.addEvent(new ClickEvent(action, value));
                                }).otherwise(v -> delegate.report("Missing click event value", e));
                            }).otherwise(a -> delegate.report("Missing click event action", e));
                });
            }

            return component;
        } else {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know how to turn this into a text component", pattern, file);
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
    private TridentFile file;

    public ReportDelegate(TridentCompiler compiler, boolean strict, TokenPattern<?> pattern, TridentFile file) {
        this.compiler = compiler;
        this.strict = strict;
        this.pattern = pattern;
        this.file = file;
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
        if(this.strict) {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, strict, pattern, file);
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.WARNING, notStrict, pattern));
        }
    }
}
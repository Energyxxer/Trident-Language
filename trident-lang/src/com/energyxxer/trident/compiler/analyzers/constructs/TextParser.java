package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.commodore.textcomponents.events.ClickEvent;
import com.energyxxer.commodore.textcomponents.events.HoverEvent;
import com.energyxxer.commodore.textcomponents.events.InsertionEvent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.default_libs.JsonLib;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.extensions.EObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import static com.energyxxer.trident.compiler.util.Using.using;
import static com.energyxxer.trident.extensions.EJsonElement.*;

public class TextParser {

    public static TextComponent parseTextComponent(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        switch(pattern.getName()) {
            case "TEXT_COMPONENT": {
                return parseTextComponent(((TokenStructure)pattern).getContents(), ctx);
            }
            case "INTERPOLATION_BLOCK": {
                TextComponent result = InterpolationManager.parse(pattern, ctx, TextComponent.class);
                EObject.assertNotNull(result, pattern, ctx);
                return result;
            }
            case "JSON_ROOT":
            case "JSON_ELEMENT": {
                try {
                    return parseTextComponent(JsonParser.parseJson(pattern, ctx), ctx, pattern, TextComponentContext.CHAT);
                } finally {
                    JsonParser.clearCache();
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown text component production: '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    public static TextComponent parseTextComponent(JsonElement elem, ISymbolContext ctx, TokenPattern<?> pattern, TextComponentContext context) {
        if(elem instanceof TextComponentJsonElement) return ((TextComponentJsonElement) elem).getWrapped();

        boolean strict = ctx.getCompiler().getProperties().has("strict-text-components") && getAsBooleanOrNull(ctx.getCompiler().getProperties().get("strict-text-components"));

        ReportDelegate delegate = new ReportDelegate(ctx.getCompiler(), strict, pattern, ctx);

        final TextComponent[] component = new TextComponent[1];

        if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isString()) {
            return new StringTextComponent(getAsStringOrNull(elem));
        } else if(elem.isJsonArray()) {
            ListTextComponent list = new ListTextComponent();
            for(JsonElement sub : elem.getAsJsonArray()) {
                list.append(parseTextComponent(sub, ctx, pattern, context));
            }
            return list;
        } else if(elem.isJsonObject()) {
            JsonObject obj = getAsJsonObjectOrNull(elem);

            component[0] = null;

            if(obj.has("text")) {
                using(getAsStringOrNull(obj.get("text")))
                        .notIfNull()
                        .run(t -> component[0] = new StringTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'text'", obj.get("text")));
            } else if(obj.has("translate")) {
                using(getAsStringOrNull(obj.get("translate")))
                        .notIfNull()
                        .run(t -> {
                            component[0] = new TranslateTextComponent(t);
                            if(obj.has("with")) {
                                using(getAsJsonArrayOrNull(obj.get("with"))).notIfNull().run(
                                        a -> a.forEach(e -> ((TranslateTextComponent) component[0]).addWith(parseTextComponent(e, ctx, pattern, context)))
                                ).otherwise(v -> delegate.report("Expected array in 'with'", obj.get("with")));
                            }
                        }).otherwise(t -> delegate.report("Expected string in 'translate'", obj.get("translate")));
            } else if(obj.has("keybind")) {
                using(getAsStringOrNull(obj.get("keybind")))
                        .notIfNull()
                        .run(t -> component[0] = new KeybindTextComponent(t))
                        .otherwise(t -> delegate.report("Expected string in 'keybind'", obj.get("keybind")));
            } else if(obj.has("score")) {
                using(getAsJsonObjectOrNull(obj.get("score"))).notIfNull().run(s -> {
                    String name = getAsStringOrNull(s.get("name"));
                    if(name == null) delegate.report("Missing 'name' string for 'score' text component", s);
                    String objectiveName = getAsStringOrNull(s.get("objective"));
                    if(objectiveName == null) delegate.report("Missing 'objective' string for 'score' text component", s);
                    if(name != null && objectiveName != null) {
                        Objective objective = ctx.getCompiler().getModule().getObjectiveManager().getOrCreate(objectiveName);
                        component[0] = new ScoreTextComponent(new LocalScore(objective, new RawEntity(name)));
                    }
                }).otherwise(v -> delegate.report("Expected object in 'score'", obj.get("score")));
            } else if(obj.has("selector")) {
                using(getAsStringOrNull(obj.get("selector")))
                        .notIfNull()
                        .run(t -> component[0] = new SelectorTextComponent(new RawEntity(t)))
                        .otherwise(t -> delegate.report("Expected string in 'selector'", obj.get("selector")));
            } else if(obj.has("nbt")) {
                using(getAsStringOrNull(obj.get("nbt"))).notIfNull().run(s -> {
                    Boolean rawInterpret = getAsBooleanOrNull(obj.get("interpret"));
                    boolean interpret = rawInterpret != null && rawInterpret;

                    using(getAsStringOrNull(obj.get("entity"))).notIfNull()
                            .run(e -> component[0] = new RawNBTTextComponent(s, "entity", e, interpret))
                            .otherwise(
                                    v -> using(getAsStringOrNull(obj.get("block"))).notIfNull().run(b ->
                                            component[0] = new RawNBTTextComponent(s, "block", b, interpret))
                                            .otherwise(w -> using(getAsStringOrNull(obj.get("storage"))).notIfNull().run(b ->
                                            component[0] = new RawNBTTextComponent(s, "storage", b, interpret)
                                                    ).otherwise(x -> delegate.report("Expected either 'entity', 'block' or 'storage' in nbt text component, got neither.", obj)))
                    );
                }).otherwise(v -> delegate.report("Expected object in 'nbt'", obj.get("nbt")));
            }
            if(component[0] == null) {
                throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know how to turn this into a text component: " + elem, pattern, ctx);
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
            component[0].setStyle(style);

            if(obj.has("hoverEvent")) {
                using(obj.getAsJsonObject("hoverEvent")).notIfNull().run(e -> {
                    if(!context.isHoverEnabled()) delegate.report("Hover events are not allowed in this context", "Hover events are not used in this context", e);

                    using(getAsStringOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal hover event action '" + a + "'", "Unknown hover event action '" + a + "'", e.get("action")))
                            .run(a -> {
                        HoverEvent.Action action = HoverEvent.Action.valueOf(a.toUpperCase());
                        using(e.get("value")).notIfNull().run(v -> {
                            if(v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                                String value = getAsStringOrNull(v);
                                component[0].addEvent(new HoverEvent(action, value));
                            } else {
                                TextComponent value = (parseTextComponent(v, ctx, pattern, TextComponentContext.TOOLTIP));
                                component[0].addEvent(new HoverEvent(action, value));
                            }
                        }).otherwise(v -> delegate.report("Missing hover event value", e));
                    }).otherwise(a -> delegate.report("Missing hover event action", e));
                });
            }
            if(obj.has("clickEvent")) {
                using(obj.getAsJsonObject("clickEvent")).notIfNull().run(e -> {
                    if(!context.isClickEnabled()) delegate.report("Click events are not allowed in this context", "Click events are not used in this context", e);

                    using(getAsStringOrNull(e.get("action"))).notIfNull()
                            .except(IllegalArgumentException.class, (x, a) -> delegate.report("Illegal click event action '" + a + "'", "Unknown click event action '" + a + "'", e.get("action")))
                            .run(a -> {
                                ClickEvent.Action action = ClickEvent.Action.valueOf(a.toUpperCase());
                                using(e.get("value")).notIfNull().run(v -> {
                                    String value = getAsStringOrNull(v);
                                    if(value == null) delegate.report("Missing click event value", e);
                                    else component[0].addEvent(new ClickEvent(action, value));
                                }).otherwise(v -> delegate.report("Missing click event value", e));
                            }).otherwise(a -> delegate.report("Missing click event action", e));
                });
            }
            if(obj.has("insertion")) {
                using(getAsStringOrNull(obj.get("insertion"))).notIfNull().run(
                        t -> component[0].addEvent(new InsertionEvent(t))
                ).otherwise(v -> delegate.report("Expected string in 'insertion'", obj.get("insertion")));
            }

            if(obj.has("extra")) {
                using(getAsJsonArrayOrNull(obj.get("extra"))).notIfNull().run(
                        a -> a.forEach(e -> component[0].addExtra(parseTextComponent(e, ctx, pattern, context)))
                ).otherwise(v -> delegate.report("Expected array in 'extra'", obj.get("extra")));
            }

            return component[0];
        } else {
            throw new TridentException(TridentException.Source.COMMAND_ERROR, "Don't know how to turn this into a text component: " + elem, pattern, ctx);
        }
    }

    /**
     * TextParser must not be instantiated.
     */
    private TextParser() {

    }

    public static class RawNBTTextComponent extends TextComponent {
        @NotNull
        private final String path;
        @NotNull
        private final String key;
        @NotNull
        private final String toPrint;
        private final boolean interpret;

        RawNBTTextComponent(@NotNull String path, @NotNull String key, @NotNull String toPrint, boolean interpret) {
            this.path = path;
            this.key = key;
            this.toPrint = toPrint;
            this.interpret = interpret;
        }

        @Override
        public boolean supportsProperties() {
            return true;
        }

        @Override
        public String toString(TextStyle parentStyle) {
            String baseProperties = this.getBaseProperties(parentStyle);

            String extra = "\"" + key + "\":\"" + CommandUtils.escape(toPrint) + "\"";
            if(interpret) extra += ",\"interpret\":true";
            return "{\"nbt\":\"" + CommandUtils.escape(path) + "\"," +
                    extra +
                    (baseProperties != null ? "," + baseProperties : "") +
                    '}';
        }
    }

    public static class TextComponentJsonElement extends JsonLib.WrapperJsonElement<TextComponent> {
        public TextComponentJsonElement(TextComponent wrapped) {
            super(wrapped, TextComponent.class);
        }
    }

    static class ReportDelegate {
        private TridentCompiler compiler;
        private boolean strict;
        private TokenPattern<?> pattern;
        private ISymbolContext ctx;

        public ReportDelegate(TridentCompiler compiler, boolean strict, TokenPattern<?> pattern, ISymbolContext ctx) {
            this.compiler = compiler;
            this.strict = strict;
            this.pattern = pattern;
            this.ctx = ctx;
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
                throw new TridentException(TridentException.Source.COMMAND_ERROR, strict, pattern, ctx);
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, notStrict, pattern));
            }
        }
    }

    static class RawEntity extends PlayerName {
        public RawEntity(@NotNull String name) {
            super(name);
        }

        @Override
        public void assertPlayer() {

        }

        @Override
        public void assertEntityFriendly() {

        }

        @Override
        public void assertScoreHolderFriendly() {

        }

        @Override
        public void assertPlayer(String causeKey) {

        }

        @Override
        public void assertGameProfile() {

        }

        @Override
        public void assertSingle() {

        }

        @Override
        public void assertSingle(String causeKey) {

        }

        @Override
        public void assertAvailable() {

        }
    }
}

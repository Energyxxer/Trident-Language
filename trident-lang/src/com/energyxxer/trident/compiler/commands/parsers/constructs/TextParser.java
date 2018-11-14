package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.textcomponents.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.EntryParsingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TextParser {

    public static TextComponent parseTextComponent(TokenPattern<?> pattern, TridentCompiler compiler) {
        switch(pattern.getName()) {
            case "TEXT_COMPONENT": {
                return parseTextComponent(JsonParser.parseJson(((TokenStructure) pattern).getContents(), compiler), compiler, pattern);
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown text component production: '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
            }
        }
    }

    private static TextComponent parseTextComponent(JsonElement elem, TridentCompiler compiler, TokenPattern<?> pattern) {
        if(elem.isJsonPrimitive() && ((JsonPrimitive)elem).isString()) {
            return new StringTextComponent(elem.getAsString());
        } else if(elem.isJsonArray()) {
            ListTextComponent list = new ListTextComponent();
            for(JsonElement sub : elem.getAsJsonArray()) {
                list.append(parseTextComponent(sub, compiler, pattern));
            }
            return list;
        } else if(elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();

            TextComponent component;

            if(obj.has("text")) {
                component = new StringTextComponent(obj.get("text").getAsString());
            } else if(obj.has("translate")) {
                //component = new TranslateTextComponent(obj.get("translate").getAsString()); TODO: TranslateTextComponent
                component = new StringTextComponent("");
            } else if(obj.has("score")) {
                //TODO
                component = new StringTextComponent("");
            } else if(obj.has("selector")) {
                //TODO
                component = new StringTextComponent("");
            } else {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Don't know how to turn this into a text component", pattern));
                throw new EntryParsingException();
            }


            TextStyle style = new TextStyle(0);
            style.setMask(0);
            if(obj.has("color")) {
                try {
                    style.setColor(TextColor.valueOf(obj.get("color").getAsString().toUpperCase()));
                } catch(IllegalArgumentException x) {
                    compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Unknown text color '" + obj.get("color").getAsString() + "'", pattern));
                }
            }
            if(obj.has("bold")) {
                style.setMask(style.getMask() | TextStyle.BOLD);
                if(obj.get("bold").getAsBoolean()) {
                    style.setFlags((byte) (style.getFlags() | TextStyle.BOLD));
                } else {
                    style.setFlags((byte) ~(~style.getFlags() | TextStyle.BOLD));
                }
            }
            if(obj.has("italic")) {
                style.setMask(style.getMask() | TextStyle.ITALIC);
                if(obj.get("italic").getAsBoolean()) {
                    style.setFlags((byte) (style.getFlags() | TextStyle.ITALIC));
                } else {
                    style.setFlags((byte) ~(~style.getFlags() | TextStyle.ITALIC));
                }
            }
            if(obj.has("strikethrough")) {
                style.setMask(style.getMask() | TextStyle.STRIKETHROUGH);
                if(obj.get("strikethrough").getAsBoolean()) {
                    style.setFlags((byte) (style.getFlags() | TextStyle.STRIKETHROUGH));
                } else {
                    style.setFlags((byte) ~(~style.getFlags() | TextStyle.STRIKETHROUGH));
                }
            }
            if(obj.has("obfuscated")) {
                style.setMask(style.getMask() | TextStyle.OBFUSCATED);
                if(obj.get("obfuscated").getAsBoolean()) {
                    style.setFlags((byte) (style.getFlags() | TextStyle.OBFUSCATED));
                } else {
                    style.setFlags((byte) ~(~style.getFlags() | TextStyle.OBFUSCATED));
                }
            }
            component.setStyle(style);
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

package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.bossbar.BossbarAddCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.BossbarCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.BossbarListCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.BossbarRemoveCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.get.BossbarGetMaxCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.get.BossbarGetPlayersCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.get.BossbarGetValueCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.get.BossbarGetVisibleCommand;
import com.energyxxer.commodore.functionlogic.commands.bossbar.set.*;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.BossbarReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "bossbar")
public class BossbarParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "LIST": return new BossbarListCommand();
            case "ADD": return parseAdd(inner, file.getCompiler());
            case "GET": return parseGet(inner, file.getCompiler());
            case "REMOVE": return parseRemove(inner, file.getCompiler());
            case "SET": return parseSet(inner, file.getCompiler());
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }

    private Command parseAdd(TokenPattern<?> inner, TridentCompiler compiler) {
        TridentUtil.ResourceLocation id = new TridentUtil.ResourceLocation(inner.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
        TextComponent name = TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), compiler);
        BossbarReference ref = new BossbarReference(compiler.getModule().getNamespace(id.namespace), id.body);
        return new BossbarAddCommand(ref, name);
    }

    private Command parseGet(TokenPattern<?> inner, TridentCompiler compiler) {
        TridentUtil.ResourceLocation id = new TridentUtil.ResourceLocation(inner.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
        BossbarReference ref = new BossbarReference(compiler.getModule().getNamespace(id.namespace), id.body);

        String rawVariable = inner.find("CHOICE").flatten(false);
        switch(rawVariable) {
            case "max": return new BossbarGetMaxCommand(ref);
            case "value": return new BossbarGetValueCommand(ref);
            case "players": return new BossbarGetPlayersCommand(ref);
            case "visible": return new BossbarGetVisibleCommand(ref);
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown bossbar get branch '" + rawVariable + "'"));
                return null;
            }
        }
    }

    private Command parseRemove(TokenPattern<?> inner, TridentCompiler compiler) {
        TridentUtil.ResourceLocation id = new TridentUtil.ResourceLocation(inner.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
        BossbarReference ref = new BossbarReference(compiler.getModule().getNamespace(id.namespace), id.body);
        return new BossbarRemoveCommand(ref);
    }

    private Command parseSet(TokenPattern<?> pattern, TridentCompiler compiler) {
        TridentUtil.ResourceLocation id = new TridentUtil.ResourceLocation(pattern.search(TridentTokens.RESOURCE_LOCATION).get(0).value);
        BossbarReference ref = new BossbarReference(compiler.getModule().getNamespace(id.namespace), id.body);

        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "SET_COLOR":
                return new BossbarSetColorCommand(ref, BossbarCommand.BossbarColor.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            case "SET_MAX":
                return new BossbarSetMaxCommand(ref, Integer.parseInt(inner.find("INTEGER").flatten(false)));
            case "SET_NAME":
                return new BossbarSetNameCommand(ref, TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), compiler));
            case "SET_PLAYERS":
                return new BossbarSetPlayersCommand(ref, EntityParser.parseEntity(inner.find("ENTITY"), compiler));
            case "SET_STYLE":
                return new BossbarSetStyleCommand(ref, BossbarCommand.BossbarStyle.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            case "SET_VALUE":
                return new BossbarSetValueCommand(ref, Integer.parseInt(inner.find("INTEGER").flatten(false)));
            case "SET_VISIBLE":
                return new BossbarSetVisibleCommand(ref, inner.search(TridentTokens.BOOLEAN).get(0).value.equals("true"));
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown bossbar set branch '" + inner.getName() + "'"));
                return null;
            }
        }
    }
}

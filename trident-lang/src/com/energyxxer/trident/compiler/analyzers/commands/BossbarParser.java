package com.energyxxer.trident.compiler.analyzers.commands;

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
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "bossbar")
public class BossbarParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "LIST": return new BossbarListCommand();
            case "ADD": return parseAdd(inner, ctx);
            case "GET": return parseGet(inner, ctx);
            case "REMOVE": return parseRemove(inner, ctx);
            case "SET": return parseSet(inner, ctx);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private Command parseAdd(TokenPattern<?> inner, ISymbolContext ctx) {
        TridentUtil.ResourceLocation id = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
        id.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
        TextComponent name = TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), ctx);
        BossbarReference ref = new BossbarReference(ctx.getCompiler().getModule().getNamespace(id.namespace), id.body);
        return new BossbarAddCommand(ref, name);
    }

    private Command parseGet(TokenPattern<?> inner, ISymbolContext ctx) {
        TridentUtil.ResourceLocation id = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
        id.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
        BossbarReference ref = new BossbarReference(ctx.getCompiler().getModule().getNamespace(id.namespace), id.body);

        String rawVariable = inner.find("CHOICE").flatten(false);
        switch(rawVariable) {
            case "max": return new BossbarGetMaxCommand(ref);
            case "value": return new BossbarGetValueCommand(ref);
            case "players": return new BossbarGetPlayersCommand(ref);
            case "visible": return new BossbarGetVisibleCommand(ref);
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + rawVariable + "'", inner, ctx);
            }
        }
    }

    private Command parseRemove(TokenPattern<?> inner, ISymbolContext ctx) {
        TridentUtil.ResourceLocation id = CommonParsers.parseResourceLocation(inner.find("RESOURCE_LOCATION"), ctx);
        id.assertStandalone(inner.find("RESOURCE_LOCATION"), ctx);
        BossbarReference ref = new BossbarReference(ctx.getCompiler().getModule().getNamespace(id.namespace), id.body);
        return new BossbarRemoveCommand(ref);
    }

    private Command parseSet(TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUtil.ResourceLocation id = CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION"), ctx);
        id.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);
        BossbarReference ref = new BossbarReference(ctx.getCompiler().getModule().getNamespace(id.namespace), id.body);

        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "SET_COLOR":
                return new BossbarSetColorCommand(ref, BossbarCommand.BossbarColor.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            case "SET_MAX":
                return new BossbarSetMaxCommand(ref, CommonParsers.parseInt(inner.find("INTEGER"), ctx));
            case "SET_NAME":
                return new BossbarSetNameCommand(ref, TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), ctx));
            case "SET_PLAYERS":
                return new BossbarSetPlayersCommand(ref, EntityParser.parseEntity(inner.find("OPTIONAL_ENTITY.ENTITY"), ctx));
            case "SET_STYLE":
                return new BossbarSetStyleCommand(ref, BossbarCommand.BossbarStyle.valueOf(inner.find("CHOICE").flatten(false).toUpperCase()));
            case "SET_VALUE":
                return new BossbarSetValueCommand(ref, CommonParsers.parseInt(inner.find("INTEGER"), ctx));
            case "SET_VISIBLE":
                return new BossbarSetVisibleCommand(ref, inner.search(TridentTokens.BOOLEAN).get(0).value.equals("true"));
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}

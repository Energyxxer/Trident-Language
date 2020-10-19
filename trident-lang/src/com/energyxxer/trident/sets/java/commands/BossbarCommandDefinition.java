package com.energyxxer.trident.sets.java.commands;

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
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.BossbarReference;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class BossbarCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"bossbar"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return PrismarineProductions.group(
                TridentProductions.commandHeader("bossbar"),
                choice(
                        literal("list").setEvaluator((p, d) -> new BossbarListCommand()),
                        group(
                                literal("add"),
                                TridentProductions.noToken().addTags("cspn:Bossbar"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION"),
                                productions.getOrCreateStructure("TEXT_COMPONENT")
                        ).setEvaluator(BossbarCommandDefinition::parseAdd),
                        group(
                                literal("get"),
                                TridentProductions.noToken().addTags("cspn:Bossbar"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION"),
                                choice(
                                        literal("max").setEvaluator((p, d) -> new BossbarGetMaxCommand((BossbarReference) d[0])),
                                        literal("players").setEvaluator((p, d) -> new BossbarGetValueCommand((BossbarReference) d[0])),
                                        literal("value").setEvaluator((p, d) -> new BossbarGetPlayersCommand((BossbarReference) d[0])),
                                        literal("visible").setEvaluator((p, d) -> new BossbarGetVisibleCommand((BossbarReference) d[0]))
                                ).setName("WHAT_TO_GET")
                        ).setEvaluator(BossbarCommandDefinition::parseGet),
                        group(
                                literal("remove"),
                                TridentProductions.noToken().addTags("cspn:Bossbar"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION")
                        ).setEvaluator(BossbarCommandDefinition::parseRemove),
                        group(
                                literal("set"),
                                TridentProductions.noToken().addTags("cspn:Bossbar"),
                                productions.getOrCreateStructure("RESOURCE_LOCATION"),
                                choice(
                                        group(
                                                literal("color"),
                                                enumChoice(BossbarCommand.BossbarColor.class).setName("BOSSBAR_COLOR").addTags("cspn:Bossbar Color")
                                        ).setEvaluator((p, d) -> new BossbarSetColorCommand((BossbarReference) d[1], (BossbarCommand.BossbarColor) p.find("BOSSBAR_COLOR").evaluate((ISymbolContext) d[0]))),
                                        group(
                                                literal("max"),
                                                TridentProductions.integer(productions).addTags("cspn:Max Value")
                                        ).setEvaluator((p, d) -> new BossbarSetMaxCommand((BossbarReference) d[1], (int) p.find("INTEGER").evaluate((ISymbolContext) d[0]))),
                                        group(
                                                literal("name"),
                                                TridentProductions.noToken().addTags("cspn:Display Name"),
                                                productions.getOrCreateStructure("TEXT_COMPONENT")
                                        ).setEvaluator((p, d) -> new BossbarSetNameCommand((BossbarReference) d[1], (TextComponent) p.find("TEXT_COMPONENT").evaluate((ISymbolContext) d[0]))),
                                        group(
                                                literal("players"), optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setSimplificationFunctionContentIndex(1).setName("VIEWING_PLAYERS")
                                        ).setEvaluator((p, d) -> new BossbarSetPlayersCommand((BossbarReference) d[1], (Entity) p.findThenEvaluate("VIEWING_PLAYERS", null, (ISymbolContext) d[0]))),
                                        group(
                                                literal("style"),
                                                enumChoice(BossbarCommand.BossbarStyle.class).setName("BOSSBAR_STYLE")
                                        ).setEvaluator((p, d) -> new BossbarSetStyleCommand((BossbarReference) d[1], (BossbarCommand.BossbarStyle) p.find("BOSSBAR_STYLE").evaluate((ISymbolContext) d[0]))),
                                        group(
                                                literal("value"),
                                                TridentProductions.integer(productions).addTags("cspn:Value")
                                        ).setEvaluator((p, d) -> new BossbarSetValueCommand((BossbarReference) d[1], (int) p.find("INTEGER").evaluate((ISymbolContext) d[0]))),
                                        group(
                                                literal("visible"),
                                                TridentProductions.rawBoolean().addTags(SuggestionTags.ENABLED, "cspn:Visible?")
                                        ).setEvaluator((p, d) -> new BossbarSetVisibleCommand((BossbarReference) d[1], (boolean) p.find("BOOLEAN").evaluate((ISymbolContext) d[0])))
                                ).setName("INNER")
                        ).setEvaluator(BossbarCommandDefinition::parseSet)
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }

    private static Command parseAdd(TokenPattern<?> pattern, Object... data) {
        ISymbolContext ctx = (ISymbolContext) data[0];

        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx);
        TextComponent name = (TextComponent) pattern.find("TEXT_COMPONENT").evaluate(ctx);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return new BossbarAddCommand(ref, name);
    }

    private static Command parseGet(TokenPattern<?> pattern, Object... data) {
        ISymbolContext ctx = (ISymbolContext) data[0];

        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return (Command) pattern.find("WHAT_TO_GET").evaluate(ref);
    }

    private static Command parseRemove(TokenPattern<?> pattern, Object... data) {
        ISymbolContext ctx = (ISymbolContext) data[0];

        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);
        return new BossbarRemoveCommand(ref);
    }

    private static Command parseSet(TokenPattern<?> pattern, Object... data) {
        ISymbolContext ctx = (ISymbolContext) data[0];

        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx);
        id.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return (Command) pattern.find("INNER").evaluate(ctx, ref);
    }
}

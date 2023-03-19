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
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class BossbarCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"bossbar"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return PrismarineProductions.group(
                TridentProductions.commandHeader("bossbar"),
                choice(
                        literal("list").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarListCommand()),
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
                                        literal("max").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarGetMaxCommand((BossbarReference) d[0])),
                                        literal("players").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarGetValueCommand((BossbarReference) d[0])),
                                        literal("value").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarGetPlayersCommand((BossbarReference) d[0])),
                                        literal("visible").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarGetVisibleCommand((BossbarReference) d[0]))
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
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetColorCommand((BossbarReference) d[0], (BossbarCommand.BossbarColor) p.find("BOSSBAR_COLOR").evaluate(ctx, null))),
                                        group(
                                                literal("max"),
                                                TridentProductions.integer(productions).addTags("cspn:Max Value")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetMaxCommand((BossbarReference) d[0], (int) p.find("INTEGER").evaluate(ctx, null))),
                                        group(
                                                literal("name"),
                                                TridentProductions.noToken().addTags("cspn:Display Name"),
                                                productions.getOrCreateStructure("TEXT_COMPONENT")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetNameCommand((BossbarReference) d[0], (TextComponent) p.find("TEXT_COMPONENT").evaluate(ctx, null))),
                                        group(
                                                literal("players"), optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setSimplificationFunctionContentIndex(1).setName("VIEWING_PLAYERS")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetPlayersCommand((BossbarReference) d[0], (Entity) p.findThenEvaluate("VIEWING_PLAYERS", null, ctx, null))),
                                        group(
                                                literal("style"),
                                                enumChoice(BossbarCommand.BossbarStyle.class).setName("BOSSBAR_STYLE")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetStyleCommand((BossbarReference) d[0], (BossbarCommand.BossbarStyle) p.find("BOSSBAR_STYLE").evaluate(ctx, null))),
                                        group(
                                                literal("value"),
                                                TridentProductions.integer(productions).addTags("cspn:Value")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetValueCommand((BossbarReference) d[0], (int) p.find("INTEGER").evaluate(ctx, null))),
                                        group(
                                                literal("visible"),
                                                TridentProductions.rawBoolean().addTags(SuggestionTags.ENABLED, "cspn:Visible?")
                                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new BossbarSetVisibleCommand((BossbarReference) d[0], (boolean) p.find("BOOLEAN").evaluate(ctx, null)))
                                ).setName("INNER")
                        ).setEvaluator(BossbarCommandDefinition::parseSet)
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }

    private static Command parseAdd(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null);
        TextComponent name = (TextComponent) pattern.find("TEXT_COMPONENT").evaluate(ctx, null);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return new BossbarAddCommand(ref, name);
    }

    private static Command parseGet(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return (Command) pattern.find("WHAT_TO_GET").evaluate(ctx, new Object[] {ref});
    }

    private static Command parseRemove(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);
        return new BossbarRemoveCommand(ref);
    }

    private static Command parseSet(TokenPattern<?> pattern, ISymbolContext ctx, Object[] data) {
        ResourceLocation id = (ResourceLocation) pattern.find("RESOURCE_LOCATION").evaluate(ctx, null);
        id.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);
        BossbarReference ref = new BossbarReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(id.namespace), id.body);

        return (Command) pattern.find("INNER").evaluate(ctx, new Object[] {ref});
    }
}

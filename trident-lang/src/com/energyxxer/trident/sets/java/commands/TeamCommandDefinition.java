package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.team.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.PatternEvaluator;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TeamCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"team"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        PatternEvaluator<ISymbolContext> teamOptionEvaluator = (TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            TeamReference team = (TeamReference) d[0];

            TokenPattern<?>[] contents = ((TokenGroup) p).getContents();
            TeamModifyCommand.TeamModifyKey key = (TeamModifyCommand.TeamModifyKey) contents[0].evaluate(ctx, null);
            Object value = contents[1].evaluate(ctx, null);
            return new TeamModifyCommand(team, key, value);
        };

        TokenStructureMatch teamOptions = choice(
                group(
                        literal("collisionRule").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.COLLISION_RULE),
                        choice(
                                literal("always").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("pushOtherTeams").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("pushOwnTeam").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("color").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.COLOR),
                        productions.getOrCreateStructure("TEXT_COLOR")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("deathMessageVisibility").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.DEATH_MESSAGE_VISIBILITY),
                        choice(
                                literal("always").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("hideForOwnTeam").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("hideForOtherTeams").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("displayName").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.DISPLAY_NAME),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("friendlyFire").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.FRIENDLY_FIRE),
                        TridentProductions.rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Friendly Fire?")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("nametagVisibility").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.NAMETAG_VISIBILITY),
                        choice(
                                literal("always").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("hideForOwnTeam").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("hideForOtherTeams").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("prefix").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.PREFIX),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("suffix").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.SUFFIX),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("seeFriendlyInvisibles").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> TeamModifyCommand.TeamModifyKey.SEE_FRIENDLY_INVISIBLES),
                        TridentProductions.rawBoolean().setName("BOOLEAN").addTags("cspn:See Friendly Invisibles?")
                ).setEvaluator(teamOptionEvaluator)
        ).setName("TEAM_OPTIONS");

        TokenGroupMatch teamMatch = (TokenGroupMatch) group(TridentProductions.identifierA(productions)).setName("TEAM").addTags("cspn:Team").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
            String teamName = (String) ((TokenGroup) p).getContents()[0].evaluate(ctx, null);
            return new TeamReference(teamName);
        });

        return group(
                TridentProductions.commandHeader("team"),
                choice(
                        group(literal("add"), teamMatch, wrapperOptional(productions.getOrCreateStructure("TEXT_COMPONENT")).setName("DISPLAY_NAME").addTags("cspn:Display Name")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx, null);
                            TextComponent displayName = (TextComponent) p.findThenEvaluate("DISPLAY_NAME", null, ctx, null);
                            return new TeamCreateCommand(team, displayName);
                        }),
                        group(literal("empty"), teamMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx, null);
                            return new TeamEmptyCommand(team);
                        }),
                        group(literal("join"), teamMatch, optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setSimplificationFunctionContentIndex(1).setName("SUBJECT")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx, null);
                            Entity entity = (Entity) p.findThenEvaluate("SUBJECT", null, ctx, null);
                            return new TeamJoinCommand(team, entity);
                        }),
                        group(literal("leave"), productions.getOrCreateStructure("ENTITY")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx, null);
                            return new TeamLeaveCommand(entity);
                        }),
                        group(literal("list"), optional(TridentProductions.sameLine(), teamMatch).setSimplificationFunctionContentIndex(1).setName("TEAM").addTags("cspn:Team")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            TeamReference team = (TeamReference) p.findThenEvaluate("TEAM", null, ctx, null);
                            return new TeamListCommand(team);
                        }),
                        group(literal("modify"), teamMatch, teamOptions).setSimplificationFunction(d -> {
                            TokenPattern<?> pattern = d.pattern;
                            ISymbolContext ctx = (ISymbolContext) d.ctx;

                            d.unlock(); d = null;
                            TeamReference team = (TeamReference) pattern.find("TEAM").evaluate(ctx, null);

                            TokenPattern.SimplificationDomain.get(pattern.find("TEAM_OPTIONS"), ctx, new Object[] {team});
                        }),
                        group(literal("remove"), teamMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx, null);
                            return new TeamRemoveCommand(team);
                        })
                ).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            return (Command) pattern.find("INNER").evaluate(ctx, null);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}

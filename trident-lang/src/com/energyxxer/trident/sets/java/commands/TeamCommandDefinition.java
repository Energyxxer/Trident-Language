package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.team.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.PatternEvaluator;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TeamCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"team"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        PatternEvaluator teamOptionEvaluator = (p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            TeamReference team = (TeamReference) d[1];

            TokenPattern<?>[] contents = ((TokenGroup) p).getContents();
            TeamModifyCommand.TeamModifyKey key = (TeamModifyCommand.TeamModifyKey) contents[0].evaluate(ctx);
            Object value = contents[1].evaluate(ctx);
            return new TeamModifyCommand(team, key, value);
        };

        TokenStructureMatch teamOptions = choice(
                group(
                        literal("collisionRule").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.COLLISION_RULE),
                        choice(
                                literal("always").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("pushOtherTeams").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("pushOwnTeam").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("color").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.COLOR),
                        productions.getOrCreateStructure("TEXT_COLOR")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("deathMessageVisibility").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.DEATH_MESSAGE_VISIBILITY),
                        choice(
                                literal("always").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("hideForOwnTeam").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("hideForOtherTeams").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("displayName").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.DISPLAY_NAME),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("friendlyFire").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.FRIENDLY_FIRE),
                        TridentProductions.rawBoolean().setName("BOOLEAN").addTags(SuggestionTags.ENABLED, "cspn:Friendly Fire?")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("nametagVisibility").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.NAMETAG_VISIBILITY),
                        choice(
                                literal("always").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.ALL),
                                literal("never").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.NONE),
                                literal("hideForOwnTeam").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OTHER_TEAMS),
                                literal("hideForOtherTeams").setEvaluator((p, d) -> TeamModifyCommand.AppliesTo.OWN_TEAM)
                        )
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("prefix").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.PREFIX),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("suffix").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.SUFFIX),
                        productions.getOrCreateStructure("TEXT_COMPONENT")
                ).setEvaluator(teamOptionEvaluator),
                group(
                        literal("seeFriendlyInvisibles").setEvaluator((p, d) -> TeamModifyCommand.TeamModifyKey.SEE_FRIENDLY_INVISIBLES),
                        TridentProductions.rawBoolean().setName("BOOLEAN").addTags("cspn:See Friendly Invisibles?")
                ).setEvaluator(teamOptionEvaluator)
        ).setName("TEAM_OPTIONS");

        TokenGroupMatch teamMatch = (TokenGroupMatch) group(TridentProductions.identifierA(productions)).setName("TEAM").addTags("cspn:Team").setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            String teamName = (String) ((TokenGroup) p).getContents()[0].evaluate(ctx);
            return new TeamReference(teamName);
        });

        return group(
                TridentProductions.commandHeader("team"),
                choice(
                        group(literal("add"), teamMatch, wrapperOptional(productions.getOrCreateStructure("TEXT_COMPONENT")).setName("DISPLAY_NAME").addTags("cspn:Display Name")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx);
                            TextComponent displayName = (TextComponent) p.findThenEvaluate("DISPLAY_NAME", null, ctx);
                            return new TeamCreateCommand(team, displayName);
                        }),
                        group(literal("empty"), teamMatch).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx);
                            return new TeamEmptyCommand(team);
                        }),
                        group(literal("join"), teamMatch, optional(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setSimplificationFunctionContentIndex(1).setName("SUBJECT")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx);
                            Entity entity = (Entity) p.findThenEvaluate("SUBJECT", null, ctx);
                            return new TeamJoinCommand(team, entity);
                        }),
                        group(literal("leave"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            return new TeamLeaveCommand(entity);
                        }),
                        group(literal("list"), optional(TridentProductions.sameLine(), teamMatch).setSimplificationFunctionContentIndex(1).setName("TEAM").addTags("cspn:Team")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TeamReference team = (TeamReference) p.findThenEvaluate("TEAM", null, ctx);
                            return new TeamListCommand(team);
                        }),
                        group(literal("modify"), teamMatch, teamOptions).setSimplificationFunction(d -> {
                            ISymbolContext ctx = (ISymbolContext) d.data[0];
                            TeamReference team = (TeamReference) d.pattern.find("TEAM").evaluate(ctx);
                            d.data = new Object[]{ctx, team};
                            d.pattern = d.pattern.find("TEAM_OPTIONS");
                        }),
                        group(literal("remove"), teamMatch).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TeamReference team = (TeamReference) p.find("TEAM").evaluate(ctx);
                            return new TeamRemoveCommand(team);
                        })
                ).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            return (Command) pattern.find("INNER").evaluate(ctx);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}

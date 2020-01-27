package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;

@AnalyzerGroup(
        classes="AdvancementParser,BanIpParser,BanlistParser,BanParser,BossbarParser,ClearParser,CloneParser,ComponentParser,DatapackParser,DataParser,DebugParser,DefaultGamemodeParser,DeopParser,DifficultyParser,EffectParser,EnchantParser,EventParser,ExecuteParser,ExpandParser,ExperienceParser,FillParser,ForceloadParser,FunctionParser,GameLogParser,GamemodeParser,GameruleParser,GiveParser,HelpParser,KickParser,KillParser,ListParser,LocateParser,LootParser,MeParser,MsgParser,OpParser,PardonIpParser,PardonParser,ParticleParser,PlaySoundParser,RecipeParser,ReloadParser,ReplaceItemParser,SaveAllParser,SaveOffParser,SaveOnParser,SayParser,ScheduleParser,ScoreboardParser,SeedParser,SetblockParser,SetParser,SetWorldSpawnParser,SpawnpointParser,SpectateParser,SpreadPlayersParser,StopParser,StopSoundParser,SummonParser,TagParser,TeamMsgParser,TeamParser,TeleportParser,TellrawParser,TimeParser,TitleParser,TriggerParser,VerbatimParser,WeatherParser,WhitelistParser,WorldBorderParser"
)
public interface CommandParser {
    Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx);
}

package com.energyxxer.trident.sets.java;

import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenSwitchMatch;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderSet;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.sets.java.commands.*;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMMAND_HEADER;

public class JavaCommandSet extends PatternSwitchProviderSet {
    public JavaCommandSet() {
        super("COMMAND", COMMAND_HEADER);
        importUnits(
                AdvancementCommandDefinition.class,
                AttributeCommandDefinition.class,
                BanIpCommandDefinition.class,
                BanlistCommandDefinition.class,
                BanCommandDefinition.class,
                BossbarCommandDefinition.class,
                ClearCommandDefinition.class,
                CloneCommandDefinition.class,
                ComponentCommandDefinition.class,
                DatapackCommandDefinition.class,
                DataCommandDefinition.class,
                DebugCommandDefinition.class,
                DefaultGamemodeCommandDefinition.class,
                DeopCommandDefinition.class,
                DifficultyCommandDefinition.class,
                EffectCommandDefinition.class,
                EnchantCommandDefinition.class,
                EventCommandDefinition.class,
                ExecuteCommandDefinition.class,
                ExpandCommandDefinition.class,
                ExperienceCommandDefinition.class,
                FillCommandDefinition.class,
                ForceloadCommandDefinition.class,
                FunctionCommandDefinition.class,
                GameLogCommandDefinition.class,
                GamemodeCommandDefinition.class,
                GameruleCommandDefinition.class,
                GiveCommandDefinition.class,
                HelpCommandDefinition.class,
                ItemCommandDefinition.class,
                KickCommandDefinition.class,
                KillCommandDefinition.class,
                ListCommandDefinition.class,
                LocateCommandDefinition.class,
                LocateBiomeCommandDefinition.class,
                LootCommandDefinition.class,
                MeCommandDefinition.class,
                MsgCommandDefinition.class,
                OpCommandDefinition.class,
                PardonIpCommandDefinition.class,
                PardonCommandDefinition.class,
                ParticleCommandDefinition.class,
                PlaceCommandDefinition.class,
                PlaySoundCommandDefinition.class,
                RecipeCommandDefinition.class,
                ReloadCommandDefinition.class,
                ReplaceItemCommandDefinition.class,
                SaveAllCommandDefinition.class,
                SaveOffCommandDefinition.class,
                SaveOnCommandDefinition.class,
                SayCommandDefinition.class,
                ScheduleCommandDefinition.class,
                ScoreboardCommandDefinition.class,
                SeedCommandDefinition.class,
                SetblockCommandDefinition.class,
                SetCommandDefinition.class,
                SetWorldSpawnCommandDefinition.class,
                SpawnpointCommandDefinition.class,
                SpectateCommandDefinition.class,
                SpreadPlayersCommandDefinition.class,
                StopCommandDefinition.class,
                StopSoundCommandDefinition.class,
                SummonCommandDefinition.class,
                TagCommandDefinition.class,
                TeamMsgCommandDefinition.class,
                TeamCommandDefinition.class,
                TeleportCommandDefinition.class,
                TellrawCommandDefinition.class,
                TimeCommandDefinition.class,
                TitleCommandDefinition.class,
                TriggerCommandDefinition.class,
                WeatherCommandDefinition.class,
                WhitelistCommandDefinition.class,
                WorldBorderCommandDefinition.class
        );
    }

    @Override
    protected void switchCreated(TokenSwitchMatch switchMatch) {
        switchMatch.addTags(TridentSuggestionTags.TAG_COMMAND);
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure, PrismarineProjectWorker worker) {
        super.installUtilityProductions(productions, providerStructure, worker);
        productions.getOrCreateStructure("COMMAND").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.CONTEXT_COMMAND);
    }
}

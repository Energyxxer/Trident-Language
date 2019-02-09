package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.commands.data.DataGetCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteStoreScore;
import com.energyxxer.commodore.functionlogic.commands.kill.KillCommand;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScorePlayersOperation;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreSet;
import com.energyxxer.commodore.functionlogic.commands.summon.SummonCommand;
import com.energyxxer.commodore.functionlogic.commands.tellraw.TellrawCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeQueryCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathIndex;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.LimitArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.textcomponents.ListTextComponent;
import com.energyxxer.commodore.textcomponents.TextColor;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.textcomponents.TextStyle;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.energyxxer.commodore.textcomponents.TextComponent.ShorthandConstructors.*;

public class GameLogFetcherFile extends SpecialFile {

    private boolean timeEnabled = true;
    private boolean posEnabled = true;
    private boolean lineNumEnabled = false;
    private boolean compact = false;

    private LocalScore dd;
    private LocalScore hh;
    private LocalScore mm;
    private LocalScore ss;
    private LocalScore t;
    private LocalScore op;
    private LocalScore x;
    private LocalScore y;
    private LocalScore z;

    private static HashMap<String, TextColor> colors = new HashMap<>();
    private static HashMap<String, String> shorthands = new HashMap<>();
    private static List<String> levelOrder = Arrays.asList("info, debug, warning, error, fatal".split(", "));

    private Objective logLevelObjective;
    private static final String logObjectiveName = "trident_logger";

    static {
        colors.put("info", TextColor.GREEN);
        colors.put("debug", TextColor.AQUA);
        colors.put("warning", TextColor.GOLD);
        colors.put("error", TextColor.RED);
        colors.put("fatal", TextColor.DARK_RED);

        shorthands.put("info", "INFO");
        shorthands.put("debug", "DEBUG");
        shorthands.put("warning", "WARN");
        shorthands.put("error", "ERROR");
        shorthands.put("fatal", "FATAL");
    }

    public GameLogFetcherFile(SpecialFileManager parent) {
        super(parent, "debug_fetch");
    }

    @Override
    public boolean shouldForceCompile() {
        return false;
    }

    @Override
    protected void compile() {

        JsonObject preferences = parent.getCompiler().getProperties();
        if(preferences.has("game-logger") && preferences.get("game-logger").isJsonObject()) {
            JsonObject loggerPreferences = preferences.getAsJsonObject("game-logger");
            if(loggerPreferences.has("compact") && loggerPreferences.get("compact").isJsonPrimitive() && loggerPreferences.get("compact").getAsBoolean()) {
                compact = true;
            }
            if(loggerPreferences.has("timestamp-enabled") && loggerPreferences.get("timestamp-enabled").isJsonPrimitive() && !loggerPreferences.get("timestamp-enabled").getAsBoolean()) {
                timeEnabled = false;
            }
            if(loggerPreferences.has("pos-enabled") && loggerPreferences.get("pos-enabled").isJsonPrimitive() && !loggerPreferences.get("pos-enabled").getAsBoolean()) {
                posEnabled = false;
            }
            if(loggerPreferences.has("line-number-enabled") && loggerPreferences.get("line-number-enabled").isJsonPrimitive() && loggerPreferences.get("line-number-enabled").getAsBoolean()) {
                lineNumEnabled = true;
            }
        }




        Objective objective = parent.getGlobalObjective();
        if(!parent.getCompiler().getModule().getObjectiveManager().contains(logObjectiveName)) {
            parent.getCompiler().getModule().getObjectiveManager().create(logObjectiveName);
        }
        logLevelObjective = parent.getCompiler().getModule().getObjectiveManager().get(logObjectiveName);

        op = new LocalScore(new PlayerName("#OPERATION"), objective);

        if(timeEnabled) {
            dd = new LocalScore(new PlayerName("#DAYS"), objective);
            hh = new LocalScore(new PlayerName("#HOURS"), objective);
            mm = new LocalScore(new PlayerName("#MINUTES"), objective);
            ss = new LocalScore(new PlayerName("#SECONDS"), objective);
            t = new LocalScore(new PlayerName("#TICKS"), objective);
            function.append(new ExecuteCommand(new TimeQueryCommand(TimeQueryCommand.TimeCounter.GAMETIME), new ExecuteStoreScore(t)));
            function.append(new ScoreSet(op, 20));
            function.append(new ScorePlayersOperation(ss, ScorePlayersOperation.Operation.ASSIGN, t));
            function.append(new ScorePlayersOperation(t, ScorePlayersOperation.Operation.MODULO, op));
            function.append(new ScorePlayersOperation(ss, ScorePlayersOperation.Operation.DIVIDE, op));
            function.append(new ScoreSet(op, 60));
            function.append(new ScorePlayersOperation(mm, ScorePlayersOperation.Operation.ASSIGN, ss));
            function.append(new ScorePlayersOperation(ss, ScorePlayersOperation.Operation.MODULO, op));
            function.append(new ScorePlayersOperation(mm, ScorePlayersOperation.Operation.DIVIDE, op));
            function.append(new ScoreSet(op, 60));
            function.append(new ScorePlayersOperation(hh, ScorePlayersOperation.Operation.ASSIGN, mm));
            function.append(new ScorePlayersOperation(mm, ScorePlayersOperation.Operation.MODULO, op));
            function.append(new ScorePlayersOperation(hh, ScorePlayersOperation.Operation.DIVIDE, op));
            if(!compact) {
                function.append(new ScoreSet(op, 24));
                function.append(new ScorePlayersOperation(dd, ScorePlayersOperation.Operation.ASSIGN, hh));
                function.append(new ScorePlayersOperation(hh, ScorePlayersOperation.Operation.MODULO, op));
                function.append(new ScorePlayersOperation(dd, ScorePlayersOperation.Operation.DIVIDE, op));
            }
        }

        Type aec = parent.getCompiler().getModule().minecraft.types.entity.get("area_effect_cloud");
        Selector tempSelector = new Selector(Selector.BaseSelector.ALL_ENTITIES, new TypeArgument(aec), new TagArgument("trident-gamelog"), new LimitArgument(1));

        x = new LocalScore(new PlayerName("#X"), objective);
        y = new LocalScore(new PlayerName("#Y"), objective);
        z = new LocalScore(new PlayerName("#Z"), objective);
        if(posEnabled) {
            function.append(new ExecuteCommand(new SummonCommand(aec, new CoordinateSet(), new TagCompound(new TagList("Tags",new TagString("trident-gamelog"))))));
            function.append(new ExecuteCommand(new DataGetCommand(tempSelector, new NBTPath(new NBTPathKey("Pos"), new NBTPathIndex(0))), new ExecuteStoreScore(x)));
            function.append(new ExecuteCommand(new DataGetCommand(tempSelector, new NBTPath(new NBTPathKey("Pos"), new NBTPathIndex(1))), new ExecuteStoreScore(y)));
            function.append(new ExecuteCommand(new DataGetCommand(tempSelector, new NBTPath(new NBTPathKey("Pos"), new NBTPathIndex(2))), new ExecuteStoreScore(z)));
        }
        function.append(new KillCommand(tempSelector));
    }

    public TextComponent getComponentFor(String key, TextComponent message, TokenPattern<?> pattern, ISymbolContext ctx) {
        TextStyle separation = new TextStyle(TextColor.GRAY);
        TextStyle colored = new TextStyle(colors.get(key));

        ListTextComponent fullLine = list();
        fullLine.append(text("", colored));
        if(timeEnabled) {
            if(!compact) {
                fullLine.append(score(dd, colored));
                fullLine.append(text("/", colored));
            }
            fullLine.append(score(hh, colored));
            fullLine.append(text(":", colored));
            fullLine.append(score(mm, colored));
            fullLine.append(text(":", colored));
            fullLine.append(score(ss, colored));
            fullLine.append(text(".", colored));
            fullLine.append(score(t, colored));
            fullLine.append(text(" "));
        }
        fullLine.append(text("[", separation));
        if(!compact) {
            fullLine.append(text(ctx.getCompiler().getModule().getName(), colored));
            fullLine.append(text("/", separation));
        }
        fullLine.append(text(shorthands.get(key), colored));
        if(!compact) {
            fullLine.append(text("]\n    In ", separation));
        } else {
            fullLine.append(text("] In ", separation));
        }
        fullLine.append(text(ctx.getWritingFile().getResourceLocation().toString(), colored));
        if(lineNumEnabled) {
            fullLine.append(text(":", separation));
            fullLine.append(text(String.valueOf(pattern.getStringBounds().start.line), colored));
        }
        if(!compact) {
            fullLine.append(text("\n    "));
        } else {
            fullLine.append(text(" "));
        }
        if(posEnabled) {
            fullLine.append(text("at ", separation));
            fullLine.append(score(x, colored));
            fullLine.append(text(" "));
            fullLine.append(score(y, colored));
            fullLine.append(text(" "));
            fullLine.append(score(z, colored));
            fullLine.append(text(" "));
        }
        fullLine.append(text("as ", separation));
        fullLine.append(selector(new Selector(Selector.BaseSelector.SENDER), colored));
        fullLine.append(text(": ", separation));
        fullLine.append(message);
        return fullLine;
    }

    public TellrawCommand getTellrawCommandFor(String key, TextComponent message, TokenPattern<?> pattern, ISymbolContext ctx) {
        ScoreArgument scores = new ScoreArgument();
        scores.put(logLevelObjective, new NumberRange<>(levelOrder.indexOf(key)+1, null));
        return new TellrawCommand(new Selector(Selector.BaseSelector.ALL_PLAYERS, scores), getComponentFor(key, message, pattern, ctx));
    }

    public Objective getLogLevelObjective() {
        return logLevelObjective;
    }
}

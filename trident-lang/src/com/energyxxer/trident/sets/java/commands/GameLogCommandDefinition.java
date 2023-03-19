package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.ListTextComponent;
import com.energyxxer.commodore.textcomponents.SelectorTextComponent;
import com.energyxxer.commodore.textcomponents.StringTextComponent;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.commands.CommandDefinition;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.semantics.custom.special.GameLogFetcherFile;
import com.energyxxer.trident.worker.tasks.SetupBuildConfigTask;
import com.energyxxer.trident.worker.tasks.SetupSpecialFileManagerTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import static com.energyxxer.prismarine.PrismarineProductions.choice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class GameLogCommandDefinition implements CommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"gamelog"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(TridentProductions.commandHeader("gamelog"), choice("info", "debug", "warning", "error", "fatal").setName("DEBUG_GROUP"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"));
    }

    private static Stack<Object> toStringRecursion = new Stack<>();

    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        if (!ctx.get(SetupBuildConfigTask.INSTANCE).exportGameLog) return null;
        TridentUtil.assertLanguageLevel(ctx, 3, "The gamelog command is", pattern);

        ArrayList<Command> commands = new ArrayList<>();

        GameLogFetcherFile fetchFile = (GameLogFetcherFile) ctx.get(SetupSpecialFileManagerTask.INSTANCE).get("debug_fetch");
        fetchFile.startCompilation();

        commands.add(new FunctionCommand(fetchFile.getFunction()));

        TextComponent message = objectToTextComponent(pattern.find("INTERPOLATION_VALUE").evaluate(ctx, null), pattern, ctx);
        String key = pattern.find("DEBUG_GROUP").flatten(false);

        commands.add(fetchFile.getTellrawCommandFor(key, message, pattern, ctx));

        return commands;
    }

    private static TextComponent objectToTextComponent(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        if (toStringRecursion.contains(obj)) {
            return new StringTextComponent("...recursive...");
        }
        if (obj instanceof TextComponent) {
            return ((TextComponent) obj);
        } else if (obj instanceof String) {
            return new StringTextComponent(((String) obj));
        } else if (obj instanceof Entity) {
            return new SelectorTextComponent(((Entity) obj));
        } else if (obj instanceof ListObject) {
            toStringRecursion.push(obj);
            ListTextComponent list = new ListTextComponent();
            for (Object inner : ((ListObject) obj)) {
                list.append(objectToTextComponent(inner, pattern, ctx));
            }
            return list;
        } else {
            return new StringTextComponent(ctx.getTypeSystem().castToString(obj, pattern, ctx));
        }
    }
}

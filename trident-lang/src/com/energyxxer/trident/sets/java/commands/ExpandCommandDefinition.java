package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.trident.compiler.analyzers.commands.CommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class ExpandCommandDefinition implements CommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"expand"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                commandHeader("expand"),
                productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")
        );
    }

    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        TridentFile writingFile = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile();

        int lengthBefore = writingFile.getWritingModifiers().size();
        try {
            if (modifiers != null) {
                writingFile.getWritingModifiers().addAll(modifiers);
            }
            TridentFile.resolveInnerFileIntoSection(pattern.find("ANONYMOUS_INNER_FUNCTION"), new SymbolContext(ctx), writingFile.getFunction());
            return Collections.emptyList();
        } finally {
            while (writingFile.getWritingModifiers().size() > lengthBefore) {
                writingFile.getWritingModifiers().remove(lengthBefore);
            }
        }
    }
}

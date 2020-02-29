package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.Collection;
import java.util.Collections;

@AnalyzerMember(key = "expand")
public class ExpandParser implements CommandParser {
    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> modifiers) {
        TridentFile writingFile = ctx.getWritingFile();

        int lengthBefore = writingFile.getWritingModifiers().size();
        try {
            writingFile.getWritingModifiers().addAll(modifiers);
            TridentFile.resolveInnerFileIntoSection(pattern.find("ANONYMOUS_INNER_FUNCTION"), new SymbolContext(ctx), writingFile.getFunction());
        } finally {
            while(writingFile.getWritingModifiers().size() > lengthBefore) {
                writingFile.getWritingModifiers().remove(lengthBefore);
            }
        }

        return Collections.emptyList();
    }
}

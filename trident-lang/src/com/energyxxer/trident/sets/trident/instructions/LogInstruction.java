package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.choice;
import static com.energyxxer.prismarine.PrismarineProductions.group;

public class LogInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(TridentProductions.instructionKeyword("log"), choice("info", "warning", "error").setName("NOTICE_GROUP"), TridentProductions.noToken().addTags("cspn:Value"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"));
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = ctx.getTypeSystem().castToString(pattern.find("INTERPOLATION_VALUE").evaluate(ctx), pattern, ctx);
        NoticeType type;

        switch(pattern.find("NOTICE_GROUP").flatten(false)) {
            case "info": type = NoticeType.INFO; break;
            case "warning": type = NoticeType.WARNING; break;
            case "error": type = NoticeType.ERROR; break;
            default: throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
        }

        ctx.getCompiler().getReport().addNotice(new Notice(type, message, pattern));
    }
}

package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "log")
public class LogInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = InterpolationManager.castToString(InterpolationManager.parse(pattern.find("LINE_SAFE_INTERPOLATION_VALUE"), ctx), pattern, ctx);
        NoticeType type;

        switch(pattern.find("NOTICE_GROUP").flatten(false)) {
            case "info": type = NoticeType.INFO; break;
            case "warning": type = NoticeType.WARNING; break;
            case "error": type = NoticeType.ERROR; break;
            default: throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
        }

        ctx.getCompiler().getReport().addNotice(new Notice(type, message, pattern));
    }
}

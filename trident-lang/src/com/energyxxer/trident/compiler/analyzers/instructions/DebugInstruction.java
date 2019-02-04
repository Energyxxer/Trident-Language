package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

@AnalyzerMember(key = "tdndebug")
public class DebugInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        Object obj = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), ctx);

        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "" + obj, pattern));
        Debug.log(obj);
    }
}

package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "log")
public class LogInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        String message = String.valueOf(InterpolationManager.parse(pattern.find("LINE_SAFE_INTERPOLATION_VALUE"), file));
        NoticeType type;

        switch(pattern.find("NOTICE_GROUP").flatten(false)) {
            case "info": type = NoticeType.INFO; break;
            case "warning": type = NoticeType.WARNING; break;
            case "error": type = NoticeType.ERROR; break;
            default: throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, file);
        }

        file.getCompiler().getReport().addNotice(new Notice(type, message, pattern));
    }
}

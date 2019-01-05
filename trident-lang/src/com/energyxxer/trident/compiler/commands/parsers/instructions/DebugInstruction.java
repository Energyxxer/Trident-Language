package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

@ParserMember(key = "tdndebug")
public class DebugInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        Object obj = InterpolationManager.parse(pattern.find("INTERPOLATION_BLOCK"), file);

        file.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, "" + obj, pattern));
        Debug.log(obj);
    }
}

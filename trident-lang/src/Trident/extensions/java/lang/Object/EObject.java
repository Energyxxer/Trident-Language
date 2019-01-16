package Trident.extensions.java.lang.Object;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class EObject {
    public static void assertNotNull(Object thiz, TokenPattern<?> pattern, TridentFile file) {
        if(thiz == null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unexpected null value", pattern));
            throw new EntryParsingException();
        }
    }
}
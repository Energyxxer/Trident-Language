package Trident.extensions.java.lang.Object;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.util.Using;
import manifold.ext.api.Extension;
import manifold.ext.api.Self;
import manifold.ext.api.This;

@Extension
public class EObject {
    public static void ifNull(@This Object thiz, Runnable action) {
        if(thiz == null) action.run();
    }

    public static <T> Using<T> using( T object) {
        return new Using<>(object);
    }

    public static @Self Object assertNotNull(@This Object thiz, TokenPattern<?> pattern, TridentFile file) {
        if(thiz == null) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unexpected null value", pattern));
            throw new EntryParsingException();
        }
        return thiz;
    }
}
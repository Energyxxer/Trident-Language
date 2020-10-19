package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class ExceptionTypeHandler implements TypeHandler<PrismarineException> {

    private final PrismarineTypeSystem typeSystem;

    public ExceptionTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(PrismarineException exception, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        switch(member) {
            case "message": return exception.getNotice().getMessage();
            case "extendedMessage": return exception.getNotice().getExtendedMessage();
            case "line": return exception.getCausePattern().getStringLocation().line;
            case "column": return exception.getCausePattern().getStringLocation().column;
            case "index": return exception.getCausePattern().getStringLocation().index;
            case "type": return exception.getType().toString();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(PrismarineException object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(PrismarineException object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<PrismarineException> getHandledClass() {
        return PrismarineException.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "exception";
    }
}

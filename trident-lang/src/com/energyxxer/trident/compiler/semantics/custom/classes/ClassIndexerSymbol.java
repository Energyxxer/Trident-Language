package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.Nullable;

public class ClassIndexerSymbol extends Symbol {

    ClassIndexer indexer;
    CustomClassObject thisObject;
    Object index;

    public ClassIndexerSymbol(ClassIndexer indexer, CustomClassObject thisObject, Object index) {
        super("Indexer for class " + indexer.getDefiningClass().getTypeIdentifier());
        this.indexer = indexer;
        this.thisObject = thisObject;
        this.index = index;
    }

    @Override
    public @Nullable Object getValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUserFunction getter = indexer.getGetterFunction();
        if(getter == null) throw new TridentException(TridentException.Source.TYPE_ERROR, "This indexer does not have a getter", pattern, ctx);

        if(!indexer.getDefiningClass().hasAccess(ctx, indexer.getGetterVisibility())) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Getter for indexer has " + indexer.getGetterVisibility().toString().toLowerCase() + " access in " + indexer.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }

        getter.setThisObject(thisObject);
        Object returnValue = getter.safeCall(new Object[] {this.index}, new TokenPattern[] {pattern}, pattern, ctx);
        getter.setThisObject(null);
        return returnValue;
    }

    @Override
    public void safeSetValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUserFunction setter = indexer.getSetterFunction();
        if(setter == null) throw new TridentException(TridentException.Source.TYPE_ERROR, "This indexer does not have a setter", pattern, ctx);

        if(!indexer.getDefiningClass().hasAccess(ctx, indexer.getSetterVisibility())) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Setter for indexer has " + indexer.getSetterVisibility().toString().toLowerCase() + " access in " + indexer.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }
        setter.setThisObject(thisObject);
        setter.safeCall(new Object[] {this.index, value}, new TokenPattern[] {pattern, pattern}, pattern, ctx);
        setter.setThisObject(null);
    }
}

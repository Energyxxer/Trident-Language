package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import org.jetbrains.annotations.Nullable;

public class ClassIndexerSymbol extends Symbol {

    ClassIndexer indexer;
    CustomClassObject thisObject;
    Object index;

    public ClassIndexerSymbol(ClassIndexer indexer, CustomClassObject thisObject, Object index) {
        super("Indexer for class " + indexer.getDefiningClass().getTypeIdentifier(), TridentSymbolVisibility.LOCAL);
        this.indexer = indexer;
        this.thisObject = thisObject;
        this.index = index;
    }

    @Override
    public @Nullable Object getValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        PrismarineFunction getter = indexer.getGetterFunction();
        if(getter == null) throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This indexer does not have a getter", pattern, ctx);

        if(!indexer.getDefiningClass().hasAccess(ctx, indexer.getGetterVisibility())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Getter for indexer has " + indexer.getGetterVisibility().toString().toLowerCase() + " access in " + indexer.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }

        return getter.safeCall(new Object[] {this.index}, new TokenPattern[] {pattern}, pattern, ctx, this.thisObject);
    }

    @Override
    public void safeSetValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        PrismarineFunction setter = indexer.getSetterFunction();
        if(setter == null) throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This indexer does not have a setter", pattern, ctx);

        if(!indexer.getDefiningClass().hasAccess(ctx, indexer.getSetterVisibility())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Setter for indexer has " + indexer.getSetterVisibility().toString().toLowerCase() + " access in " + indexer.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }
        setter.safeCall(new Object[] {this.index, value}, new TokenPattern[] {pattern, pattern}, pattern, ctx, this.thisObject);
    }
}

package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import org.jetbrains.annotations.Nullable;

public class ClassPropertySymbol extends Symbol {

    ClassProperty property;
    CustomClassObject thisObject;

    public ClassPropertySymbol(ClassProperty property, CustomClassObject thisObject) {
        super(property.getName(), SymbolVisibility.max(property.getGetterVisibility(), property.getSetterVisibility()));
        this.property = property;
        this.thisObject = thisObject;
    }

    @Override
    public @Nullable Object getValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        PrismarineFunction getter = property.getGetterFunction();
        if(getter == null) throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Property '" + this.getName() + "' does not have a getter", pattern, ctx);

        if(!property.getDefiningClass().hasAccess(ctx, property.getGetterVisibility())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Getter for property '" + this.getName() + "' has " + property.getGetterVisibility().toString().toLowerCase() + " access in " + property.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }

        return getter.safeCall(new ActualParameterList(new Object[] {}, null, pattern), ctx, this.thisObject);
    }

    @Override
    public void safeSetValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        PrismarineFunction setter = property.getSetterFunction();
        if(setter == null) throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Property '" + this.getName() + "' does not have a setter", pattern, ctx);

        if(!property.getDefiningClass().hasAccess(ctx, property.getSetterVisibility())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Setter for property '" + this.getName() + "' has " + property.getSetterVisibility().toString().toLowerCase() + " access in " + property.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }
        setter.safeCall(new ActualParameterList(new Object[] {value}, null, pattern), ctx, this.thisObject);
    }
}

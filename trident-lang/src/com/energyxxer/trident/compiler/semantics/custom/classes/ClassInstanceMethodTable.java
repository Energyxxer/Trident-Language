package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;

public class ClassInstanceMethodTable {
    private final ClassMethodTable methodTable;
    private final CustomClassObject thisObject;

    public ClassInstanceMethodTable(CustomClassObject thisObject) {
        this.methodTable = thisObject.getType().instanceMethods;
        this.thisObject = thisObject;
    }

    public Object findAndWrap(String memberName, ActualParameterList params, ISymbolContext ctx) {
        return methodTable.findAndWrap(memberName, params, ctx, thisObject);
    }

    public ClassMethodTable getMethodTable() {
        return methodTable;
    }
}

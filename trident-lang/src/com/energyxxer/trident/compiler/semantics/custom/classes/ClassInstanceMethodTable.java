package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class ClassInstanceMethodTable {
    private final ClassMethodTable methodTable;
    private final CustomClassObject thisObject;

    public ClassInstanceMethodTable(CustomClassObject thisObject) {
        this.methodTable = thisObject.getType().instanceMethods;
        this.thisObject = thisObject;
    }

    public Object findAndWrap(String memberName, ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx) {
        return methodTable.findAndWrap(memberName, params, pattern, ctx, thisObject);
    }

    public ClassMethodTable getMethodTable() {
        return methodTable;
    }
}

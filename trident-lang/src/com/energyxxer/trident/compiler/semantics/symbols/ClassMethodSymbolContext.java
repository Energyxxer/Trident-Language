package com.energyxxer.trident.compiler.semantics.symbols;

import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ClassMethodSymbolContext extends SymbolContext {
    private HashMap<String, ClassMethodFamily> classMethods = null;
    private CustomClassObject thisObject;

    public ClassMethodSymbolContext(PrismarineCompiler compiler) {
        super(compiler);
    }

    public ClassMethodSymbolContext(ISymbolContext parentScope, CustomClassObject thisObject) {
        super(parentScope);
        this.thisObject = thisObject;
    }

    public Symbol search(@NotNull String name, ISymbolContext from, ActualParameterList params) {
        if (params != null && classMethods != null && classMethods.containsKey(name)) {
            ClassMethodFamily method = classMethods.get(name);
            return method.pickOverloadSymbol(params, ((ActualParameterList) params).getPattern(), from, thisObject);
        }
        return super.search(name, from, params);
    }

    public void putClassFunction(ClassMethodFamily func) {
        if(classMethods == null) classMethods = new HashMap<>();
        classMethods.put(func.getName(), func);
    }

    public void putMethod(ClassMethodFamily family) {
        if(classMethods == null) classMethods = new HashMap<>();
        classMethods.put(family.getName(), family);
    }
}

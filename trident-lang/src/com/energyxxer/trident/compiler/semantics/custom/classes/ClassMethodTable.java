package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.HashMap;

public class ClassMethodTable {
    private final CustomClass type;
    private final HashMap<String, ClassMethodFamily> methods = new HashMap<>();

    public ClassMethodTable(CustomClass type) {
        this.type = type;
    }

    public TridentFunction find(String memberName, ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx) {
        ClassMethodFamily family = methods.get(memberName);
        if(family != null) {
            return family.pickOverload(params, pattern, ctx);
        }
        return null;
    }

    public Object findAndWrap(String memberName, ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx, CustomClassObject thisObject) {
        TridentFunction function = find(memberName, params, pattern, ctx);
        if(function == null) return null;
        if(function instanceof TridentUserFunction) {
            return new ClassMethodFamily.ClassMethodSymbol(this.getFamily(memberName), ((TridentUserFunction) function), thisObject);
        } else {
            return function;
        }
    }

    public void put(ClassMethod method, CustomClass.MemberParentMode mode, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(!methods.containsKey(method.getName())) {
            methods.put(method.getName(), new ClassMethodFamily(method.getName()));
        }
        methods.get(method.getName()).putOverload(method, mode, pattern, ctx);
    }

    public ClassMethodFamily getFamily(String name) {
        return methods.get(name);
    }

    public Collection<ClassMethodFamily> getAllFamilies() {
        return methods.values();
    }

    public void putAll(ClassMethodTable otherTable) {
        for(ClassMethodFamily methodFamily : otherTable.methods.values()) {
            for(ClassMethod method : methodFamily.getImplementations()) {
                put(method, CustomClass.MemberParentMode.FORCE, null, null);
            }
        }
    }
}

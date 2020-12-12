package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import java.util.Collection;
import java.util.HashMap;

public class ClassMethodTable {
    private final CustomClass type;
    private final HashMap<String, ClassMethodFamily> methods = new HashMap<>();

    public ClassMethodTable(CustomClass type) {
        this.type = type;
    }

    public PrimitivePrismarineFunction find(String memberName, ActualParameterList params, ISymbolContext ctx, CustomClassObject thisObject) {
        ClassMethodFamily family = methods.get(memberName);
        if(family != null) {
            return family.pickOverload(params, ctx, thisObject);
        }
        return null;
    }

    public Object findAndWrap(String memberName, ActualParameterList params, ISymbolContext ctx, CustomClassObject thisObject) {
        PrimitivePrismarineFunction function = find(memberName, params, ctx, thisObject);
        if(function == null) return null;
        if(function instanceof PrismarineFunction) {
            return new PrismarineFunction.FixedThisFunctionSymbol(memberName, ((PrismarineFunction) function), thisObject);
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

    public void putAll(ClassMethodTable otherTable, TokenPattern<?> blame, ISymbolContext ctx) {
        for(ClassMethodFamily methodFamily : otherTable.methods.values()) {
            for(ClassMethod method : methodFamily.getImplementations()) {
                put(method, CustomClass.MemberParentMode.INHERIT, blame, ctx);
            }
        }
    }

    public void checkClashingInheritedMethodsResolved(TokenPattern<?> pattern, ISymbolContext ctx) {
        for(ClassMethodFamily family : methods.values()) {
            family.checkClashingInheritedMethodsResolved(type, pattern, ctx);
        }
    }
}

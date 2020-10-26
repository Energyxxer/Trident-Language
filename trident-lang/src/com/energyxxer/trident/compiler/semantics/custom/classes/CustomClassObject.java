package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.ContextualToString;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;

import java.util.Collection;
import java.util.HashMap;

public class CustomClassObject implements TypeHandler<CustomClassObject>, ParameterizedMemberHolder, ContextualToString {
    private final CustomClass type;
    final HashMap<String, Symbol> instanceMembers = new HashMap<>();
    private HashMap<String, Object> hiddenData = null;
    final ClassInstanceMethodTable instanceMethods;

    public CustomClassObject(CustomClass type) {
        this.type = type;
        this.instanceMethods = new ClassInstanceMethodTable(this);
    }

    @Override
    public Object getMember(CustomClassObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(instanceMembers.containsKey(member)) {
            Symbol sym = instanceMembers.get(member);

            CustomClass fieldDefiningClass = type.getInstanceMemberSupplier(member).getDefiningClass();

            if(fieldDefiningClass.hasAccess(ctx, sym.getVisibility())) {
                return keepSymbol ? sym : sym.getValue(pattern, ctx);
            } else {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + fieldDefiningClass.getClassTypeIdentifier(), pattern, ctx);
            }
        }
        throw new MemberNotFoundException();
    }

    public boolean containsMember(String name) {
        return instanceMembers.containsKey(name);
    }

    @Override
    public Object getMemberForParameters(String memberName, TokenPattern<?> pattern, ActualParameterList params, ISymbolContext ctx, boolean keepSymbol) {
        Object foundClassMethod = instanceMethods.findAndWrap(memberName, params, ctx);
        if(foundClassMethod == null) {
            try {
                foundClassMethod = getMember(this, memberName, pattern, ctx, keepSymbol);
            } catch(MemberNotFoundException ignore) {
            }
        }
        if(foundClassMethod == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot resolve function or method '" + memberName + "' of " + ctx.getTypeSystem().getTypeIdentifierForObject(this), pattern, ctx);
        }
        return foundClassMethod;
    }

    @Override
    public Object getIndexer(CustomClassObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(type.getIndexer() != null) {
            ClassIndexerSymbol sym = type.getIndexer().createSymbol(this, index);
            if(keepSymbol) {
                return sym;
            } else {
                return sym.getValue(pattern, ctx);
            }
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(CustomClassObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        for(CustomClass type : type.getInheritanceTree()) {
            PrismarineFunction castMethod = type.explicitCasts.get(targetType);
            if(castMethod != null) {
                return castMethod.safeCall(new ActualParameterList(pattern), ctx, object);
            }
        }
        throw new ClassCastException();
    }

    @Override
    public Object coerce(CustomClassObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        for(CustomClass type : type.getInheritanceTree()) {
            PrismarineFunction castMethod = type.implicitCasts.get(targetType);
            if(castMethod != null) {
                return castMethod.safeCall(new ActualParameterList(pattern), ctx, object);
            }
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into, ISymbolContext ctx) {
        return type.isInstance(object) && type.implicitCasts.containsKey(into);
    }

    @Override
    public Class<CustomClassObject> getHandledClass() {
        return CustomClassObject.class;
    }

    @Override
    public String getTypeIdentifier() {
        return type.getClassTypeIdentifier();
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isSelfHandler() {
        return true;
    }

    @Override
    public boolean isInstance(Object obj) {
        return false;
    }

    public CustomClass getType() {
        return type;
    }

    public void putMember(Symbol sym) {
        instanceMembers.put(sym.getName(), sym);
    }

    public void putMemberIfAbsent(Symbol sym) {
        instanceMembers.putIfAbsent(sym.getName(), sym);
    }

    @Override
    public String toString() {
        return "<instance of " + type + "; " + System.identityHashCode(this) + ">";
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        Object foundClassMethod = instanceMethods.findAndWrap("toString", new ActualParameterList(pattern), ctx);
        if(foundClassMethod == null) return toString();
        if(foundClassMethod instanceof PrismarineFunction.FixedThisFunctionSymbol) {
            return (String) ((PrismarineFunction.FixedThisFunctionSymbol) foundClassMethod).safeCall(new ActualParameterList(pattern), ctx);
        } else {
            return (String) ((PrimitivePrismarineFunction) foundClassMethod).safeCall(new ActualParameterList(pattern), ctx, this);
        }
    }

    @Override
    public boolean isStaticHandler() {
        return false;
    }

    @Override
    public TypeHandler getStaticHandler() {
        return type;
    }

    public Symbol getSymbol(String name) {
        return instanceMembers.get(name);
    }

    public ClassInstanceMethodTable getInstanceMethods() {
        return instanceMethods;
    }

    public Collection<Symbol> getMemberSymbols() {
        return instanceMembers.values();
    }

    public Object forceGetMember(String key) {
        return instanceMembers.get(key).getValue(null, null);
    }

    public void putHidden(String key, Object value) {
        if(hiddenData == null) hiddenData = new HashMap<>();
        hiddenData.put(key, value);
    }

    public Object getHidden(String key) {
        return hiddenData.get(key);
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return type.getTypeSystem();
    }
}

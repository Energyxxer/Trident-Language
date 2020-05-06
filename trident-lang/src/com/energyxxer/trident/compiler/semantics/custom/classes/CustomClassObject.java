package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

public class CustomClassObject implements TypeHandler<CustomClassObject>, ContextualToString {
    private final CustomClass type;
    final HashMap<String, Symbol> instanceMembers = new HashMap<>();

    public CustomClassObject(CustomClass type) {
        this.type = type;
    }

    @Override
    public Object getMember(CustomClassObject object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(instanceMembers.containsKey(member)) {
            Symbol sym = instanceMembers.get(member);

            if(type.hasAccess(ctx, sym.getVisibility())) {
                return keepSymbol ? sym : sym.getValue();
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "'" + sym.getName() + "' has " + sym.getVisibility().toString().toLowerCase() + " access in " + type.getClassTypeIdentifier(), pattern, ctx);
            }
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(CustomClassObject object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(CustomClassObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUserMethod castMethod = type.explicitCasts.get(targetType);
        if(castMethod != null) {
            castMethod.setThisObject(object);
            Object rv = castMethod.safeCall(new Object[0], new TokenPattern[0], pattern, ctx);
            castMethod.setThisObject(null);
            TridentMethod.HelperMethods.assertOfType(rv, ((TridentUserMethodBranch) castMethod.getBranches().get(0)).getFunctionPattern(), ctx, false, targetType);
            return rv;
        }
        throw new ClassCastException();
    }

    @Override
    public Object coerce(CustomClassObject object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentUserMethod castMethod = type.implicitCasts.get(targetType);
        if(castMethod != null) {
            castMethod.setThisObject(object);
            Object rv = castMethod.safeCall(new Object[0], new TokenPattern[0], pattern, ctx);
            castMethod.setThisObject(null);
            TridentMethod.HelperMethods.assertOfType(rv, ((TridentUserMethodBranch) castMethod.getBranches().get(0)).getFunctionPattern(), ctx, false, targetType);
            return rv;
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into) {
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
        return super.toString();
    }

    public String contextualToString(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(instanceMembers.containsKey("toString")) {
            Symbol toStringSymbol = instanceMembers.get("toString");
            if(toStringSymbol.getValue() instanceof TridentMethod) {
                return String.valueOf(((TridentMethod) toStringSymbol.getValue()).safeCall(new Object[0], new TokenPattern<?>[0], pattern, ctx));
            }
        }
        return toString();
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
}

package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.StringTypeHandler;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class TypeHandlerTypeHandler implements TypeHandler<TypeHandler> {

    private final Object of;

    public TypeHandlerTypeHandler() {
        of = new MethodWrapper<>("of", ((instance, params) -> of(params[0])), Object.class).createForInstance(null);
    }

    @Override
    public Object getMember(TypeHandler object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if("of".equals(member)) {
            return of;
        } else if("isInstance".equals(member)) {
            return new MethodWrapper<>("isInstance", ((instance, params) -> object.isInstance(params[0])), Object.class).createForInstance(null);
        }
        throw new MemberNotFoundException();
    }

    public static TypeHandler of(Object obj) {
        return TridentTypeManager.getStaticHandlerForObject(obj);
    }

    @Override
    public Object getIndexer(TypeHandler object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TypeHandler object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Object coerce(TypeHandler object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(isInstance(object) && targetType instanceof StringTypeHandler) {
            return object.getTypeIdentifier();
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into) {
        //Backwards compatibility with checking typeOf() against a string
        return isInstance(object) && into instanceof StringTypeHandler;
    }

    @Override
    public Class<TypeHandler> getHandledClass() {
        return TypeHandler.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "type_definition";
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler();
    }
}

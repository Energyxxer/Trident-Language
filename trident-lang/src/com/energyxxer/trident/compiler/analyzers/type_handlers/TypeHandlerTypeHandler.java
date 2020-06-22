package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

public class TypeHandlerTypeHandler implements TypeHandler<TypeHandler> {

    private TridentUserFunction of;
    private TridentUserFunction is;

    public TypeHandlerTypeHandler() {
        try {
            of = nativeMethodsToFunction(null, TypeHandlerTypeHandler.class.getMethod("of", Object.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TypeHandler object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if("of".equals(member)) {
            return of;
        } else if("is".equals(member)) {
            return new NativeMethodWrapper<>("is", ((instance, params) -> object.isInstance(params[0])), Object.class).setNullable(0).createForInstance(null);
        }
        throw new MemberNotFoundException();
    }

    public static TypeHandler of(@NativeMethodWrapper.TridentNullableArg Object obj) {
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

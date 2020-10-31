package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

import static com.energyxxer.prismarine.typesystem.functions.natives.PrismarineNativeFunctionBranch.nativeMethodsToFunction;

public class TypeHandlerTypeHandler implements TypeHandler<TypeHandler> {

    private PrismarineFunction of;
    private PrismarineFunction is;

    private final PrismarineTypeSystem typeSystem;

    public TypeHandlerTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        try {
            of = nativeMethodsToFunction(typeSystem, null, TypeHandlerTypeHandler.class.getMethod("of", Object.class, ISymbolContext.class));
            is = nativeMethodsToFunction(typeSystem, null, TypeHandlerTypeHandler.class.getMethod("is", Object.class, TypeHandler.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TypeHandler object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if("of".equals(member)) {
            return of;
        } else if("is".equals(member)) {
            return new PrismarineFunction.FixedThisFunction(is, object);
        }
        throw new MemberNotFoundException();
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


    public static TypeHandler of(@NativeFunctionAnnotations.NullableArg Object obj, ISymbolContext ctx) {
        return ctx.getTypeSystem().getStaticHandlerForObject(obj);
    }

    public static boolean is(Object object, @NativeFunctionAnnotations.ThisArg TypeHandler handler) {
        return handler.isInstance(object);
    }
}

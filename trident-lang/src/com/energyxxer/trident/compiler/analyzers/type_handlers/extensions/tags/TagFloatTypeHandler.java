package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagFloat;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;

public class TagFloatTypeHandler implements TypeHandler<TagFloat> {
    private TypeHandlerMemberCollection<TagFloat> members;

    private final PrismarineTypeSystem typeSystem;

    public TagFloatTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(TagFloatTypeHandler.class.getMethod("construct", Double.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagFloat object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagFloat object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagFloat object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (typeSystem.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(int)":
                return object.getValue().intValue();
            case "primitive(real)":
                return object.getValue().doubleValue();
        }
        return null;
    }

    @Override
    public Class<TagFloat> getHandledClass() {
        return TagFloat.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_float";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static TagFloat construct(@NativeFunctionAnnotations.NullableArg Double value) {
        if(value == null) value = 0d;
        return new TagFloat(value.floatValue());
    }
}

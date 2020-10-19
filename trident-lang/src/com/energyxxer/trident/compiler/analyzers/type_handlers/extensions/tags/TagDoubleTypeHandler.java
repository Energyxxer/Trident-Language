package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagDouble;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

public class TagDoubleTypeHandler implements TypeHandler<TagDouble> {
    private TypeHandlerMemberCollection<TagDouble> members;

    private final PrismarineTypeSystem typeSystem;

    public TagDoubleTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(TagDoubleTypeHandler.class.getMethod("construct", Double.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagDouble object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagDouble object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagDouble object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (targetType.getTypeIdentifier()) {
            case "primitive(int)":
                return object.getValue().intValue();
            case "primitive(real)":
                return object.getValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagDouble> getHandledClass() {
        return TagDouble.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_double";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return members.getConstructor();
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static TagDouble construct(@NativeFunctionAnnotations.NullableArg Double value) {
        if(value == null) value = 0d;
        return new TagDouble(value);
    }
}

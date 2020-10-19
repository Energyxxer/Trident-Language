package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

public class TagByteTypeHandler implements TypeHandler<TagByte> {
    private TypeHandlerMemberCollection<TagByte> members;

    private final PrismarineTypeSystem typeSystem;

    public TagByteTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(TagByteTypeHandler.class.getMethod("construct", Integer.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagByte object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagByte object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagByte object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (targetType.getTypeIdentifier()) {
            case "primitive(int)":
                return object.getValue().intValue();
            case "primitive(real)":
                return object.getValue().doubleValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagByte> getHandledClass() {
        return TagByte.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_byte";
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
    public static TagByte construct(@NativeFunctionAnnotations.NullableArg Integer value) {
        if(value == null) value = 0;
        return new TagByte(value);
    }
}

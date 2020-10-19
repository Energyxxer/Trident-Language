package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

public class TagIntTypeHandler implements TypeHandler<TagInt> {
    private TypeHandlerMemberCollection<TagInt> members;

    private final PrismarineTypeSystem typeSystem;

    public TagIntTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(TagIntTypeHandler.class.getMethod("construct", Integer.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagInt object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagInt object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagInt object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch (targetType.getTypeIdentifier()) {
            case "primitive(int)":
                return object.getValue();
            case "primitive(real)":
                return object.getValue().doubleValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagInt> getHandledClass() {
        return TagInt.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_int";
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
    public static TagInt construct(@NativeFunctionAnnotations.NullableArg Integer value) {
        return new TagInt(value);
    }
}

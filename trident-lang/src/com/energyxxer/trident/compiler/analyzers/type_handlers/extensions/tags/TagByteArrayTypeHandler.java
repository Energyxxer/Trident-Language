package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagByteArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;

import static com.energyxxer.prismarine.typesystem.PrismarineTypeSystem.assertOfClass;

public class TagByteArrayTypeHandler implements TypeHandler<TagByteArray> {
    private static final PrimitivePrismarineFunction CONSTRUCTOR = (params, ctx, thisObject) -> constructTagByteArray(params, ctx);

    private final PrismarineTypeSystem typeSystem;

    public TagByteArrayTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TagByteArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagByteArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagByteArray object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(list)".equals(typeSystem.getInternalTypeIdentifierForType(targetType))) {
            return new ListObject(typeSystem, object.getAllTags());
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagByteArray> getHandledClass() {
        return TagByteArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_byte_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static TagByteArray constructTagByteArray(ActualParameterList params, ISymbolContext ctx) {
        if(params.size() == 0 || params.getValue(0) == null) return new TagByteArray();
        ListObject list = PrismarineTypeSystem.assertOfClass(params.getValue(0), params.getPattern(0), ctx, ListObject.class);

        TagByteArray arr = new TagByteArray();

        for(Object obj : list) {
            Object checked = assertOfClass(obj, params.getPattern(0), ctx, Integer.class, TagByte.class);
            if(checked instanceof TagByte) {
                arr.add((TagByte) checked);
            } else {
                arr.add(new TagByte((int) checked));
            }
        }

        return arr;
    }
}

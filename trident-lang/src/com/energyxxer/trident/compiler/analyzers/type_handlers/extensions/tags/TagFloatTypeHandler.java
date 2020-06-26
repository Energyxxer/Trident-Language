package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagFloat;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class TagFloatTypeHandler implements TypeHandler<TagFloat> {
    private static final TridentFunction CONSTRUCTOR = new NativeMethodWrapper<>(
            "new tag_float",
            ((instance, params) -> new TagFloat(params[0] == null ? 0 : ((Double) params[0]).floatValue())),
            Double.class
    ).setNullable(0).createForInstance(null);

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
        switch (targetType.getTypeIdentifier()) {
            case "primitive(int)":
                return object.getValue().intValue();
            case "primitive(real)":
                return object.getValue().doubleValue();
        }
        throw new ClassCastException();
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
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }
}

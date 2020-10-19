package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

public class EntityTypeHandler implements TypeHandler<Entity> {
    private TypeHandlerMemberCollection<Entity> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        try {
            members.putMethod(EntityTypeHandler.class.getMethod("isPlayerName", Entity.class));
            members.putMethod(Entity.class.getMethod("isPlayer"));
            members.putMethod(Entity.class.getMethod("isUnknownType"));
            members.putMethod(Entity.class.getMethod("getLimit"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final PrismarineTypeSystem typeSystem;

    public EntityTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Entity object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(Entity object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Entity object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<Entity> getHandledClass() {
        return Entity.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "entity";
    }

    public static boolean isPlayerName(@NativeFunctionAnnotations.ThisArg Entity entity) {
        return entity instanceof PlayerName;
    }
}

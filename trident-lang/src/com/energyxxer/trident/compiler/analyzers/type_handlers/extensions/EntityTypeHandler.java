package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.entity.Entity")
public class EntityTypeHandler implements TypeHandler<Entity> {
    private static HashMap<String, MemberWrapper<Entity>> members = new HashMap<>();

    static {
        try {
            members.put("isPlayerName", new NativeMethodWrapper<>("isPlayerName", (instance, params) -> instance instanceof PlayerName));
            members.put("isPlayer", new NativeMethodWrapper<>(Entity.class.getMethod("isPlayer")));
            members.put("isUnknownType", new NativeMethodWrapper<>(Entity.class.getMethod("isUnknownType")));
            members.put("getLimit", new NativeMethodWrapper<>(Entity.class.getMethod("getLimit")));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(Entity object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<Entity> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
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
}

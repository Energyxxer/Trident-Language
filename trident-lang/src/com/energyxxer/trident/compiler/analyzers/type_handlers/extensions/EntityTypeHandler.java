package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.entity.Entity")
public class EntityTypeHandler implements VariableTypeHandler<Entity> {
    private static HashMap<String, MemberWrapper<Entity>> members = new HashMap<>();

    static {
        try {
            members.put("isPlayerName", new MethodWrapper<>("isPlayerName", (instance, params) -> instance instanceof PlayerName));
            members.put("isPlayer", new MethodWrapper<>(Entity.class.getMethod("isPlayer")));
            members.put("isUnknownType", new MethodWrapper<>(Entity.class.getMethod("isUnknownType")));
            members.put("getLimit", new MethodWrapper<>(Entity.class.getMethod("getLimit")));
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
    public <F> F cast(Entity object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}

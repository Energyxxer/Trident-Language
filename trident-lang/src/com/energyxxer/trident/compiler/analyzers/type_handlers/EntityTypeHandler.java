package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.entity.Entity")
public class EntityTypeHandler implements VariableTypeHandler<Entity> {
    @Override
    public Object getMember(Entity object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        return null;
    }

    @Override
    public Object getIndexer(Entity object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        return null;
    }

    @Override
    public <F> F cast(Entity object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        return null;
    }
}

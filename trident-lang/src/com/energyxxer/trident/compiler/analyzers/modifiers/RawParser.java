package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListType;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@AnalyzerMember(key = "raw")
public class RawParser implements ModifierParser {
    @NotNull
    @Override
    public Collection<ExecuteModifier> parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure) pattern.find("RAW_MODIFIER_VALUE")).getContents();
        Object obj = inner.getName().equals("STRING") ?
                CommonParsers.parseStringLiteral(inner, file) :
                InterpolationManager.parse(inner, file, String.class, ListType.class);
        if(obj instanceof String) return Collections.singletonList(new RawExecuteModifier(((String) obj)));
        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
        ListType list = ((ListType) obj);
        for(Object elem : list) {
            if(elem instanceof String) {
                modifiers.add(new RawExecuteModifier(((String) elem)));
            } else {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot turn an object of type " + VariableTypeHandler.Static.getShorthandForObject(elem) + " into a string for a raw execute modifier", pattern.find("RAW_MODIFIER_VALUE"), file);
            }
        }
        return modifiers;
    }
}

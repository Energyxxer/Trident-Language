package com.energyxxer.trident.compiler.semantics.custom;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.HashMap;

public class CustomClass implements VariableTypeHandler<CustomClass> {

    private final String name;
    private final HashMap<String, Symbol> members = new HashMap<>();
    private final CustomClass superClass = null;

    public CustomClass(String name) {
        this.name = name;
    }

    public static void defineClass(TokenPattern<?> pattern, ISymbolContext ctx) {
        Symbol.SymbolVisibility visibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.GLOBAL);
        String className = pattern.find("CLASS_NAME").flatten(false);

        CustomClass classObject = new CustomClass(className);

        ctx.putInContextForVisibility(visibility, new Symbol(className, visibility, classObject));

        TokenList bodyEntryList = (TokenList) pattern.find("CLASS_DECLARATION_BODY.CLASS_BODY_ENTRIES");

        if(bodyEntryList != null) {
            for(TokenPattern<?> entry : bodyEntryList.getContents()) {
                entry = ((TokenStructure)entry).getContents();
                switch(entry.getName()) {
                    case "CLASS_MEMBER": {
                        String memberName = entry.find("MEMBER_NAME").flatten(false);
                        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(entry.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);
                        Symbol symbol = new Symbol(memberName, memberVisibility, InterpolationManager.parse(entry.find("INTERPOLATION_VALUE"), ctx));
                        classObject.members.put(memberName, symbol);
                        break;
                    }
                    case "COMMENT": {
                        break;
                    }
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + entry.getName() + "'", entry, ctx);
                    }
                }
            }
        }

        Debug.log(classObject.members);
    }

    @Override
    public Object getMember(CustomClass object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return null;
    }

    @Override
    public Object getIndexer(CustomClass object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return null;
    }

    @Override
    public <F> F cast(CustomClass object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Class<CustomClass> getHandledClass() {
        return CustomClass.class;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public String getPrimitiveShorthand() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableTypeHandler<?> getSuperType() {
        return superClass;
    }
}

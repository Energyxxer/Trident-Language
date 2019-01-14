package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;
import java.util.Collection;

public class FunctionMethod implements VariableTypeHandler<FunctionMethod>, VariableMethod {
    TokenPattern<?> functionPattern;
    TridentFile parent;
    ArrayList<String> formalParameters = new ArrayList<>();

    public FunctionMethod(TokenPattern<?> functionPattern, TridentFile parent, Collection<String> formalParameters) {
        this.functionPattern = functionPattern;
        this.parent = parent;
        this.formalParameters.addAll(formalParameters);
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file) {
        SymbolTable innerFrame = new SymbolTable(file);
        file.getCompiler().getStack().push(innerFrame);

        for(int i = 0; i < formalParameters.size(); i++) {
            innerFrame.put(new Symbol(formalParameters.get(i), Symbol.SymbolAccess.GLOBAL, i < params.length ? params[i] : null));
        }

        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, file, file.getFunction());
        } catch(ReturnException x) {
            return x.getValue();
        } finally {
            file.getCompiler().getStack().pop();
        }
        return null;
    }

    @Override
    public Object getMember(FunctionMethod object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("formalParameters")) return new ListType(formalParameters);
        if(member.equals("definingFile")) return parent.getResourceLocation();
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(FunctionMethod object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(FunctionMethod object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return "<function(" + formalParameters.join(", ") + ")>";
    }
}

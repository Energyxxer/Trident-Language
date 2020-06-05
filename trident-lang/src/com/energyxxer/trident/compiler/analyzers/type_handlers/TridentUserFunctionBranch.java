package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.ReturnException;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.compiler.semantics.symbols.ClassMethodSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.Collection;

public class TridentUserFunctionBranch extends TridentFunctionBranch {
    private TokenPattern<?> functionPattern;
    private TokenPattern<?> voidReturnPattern;

    public TridentUserFunctionBranch(Collection<FormalParameter> formalParameters, TokenPattern<?> functionPattern, TypeConstraints returnConstraints) {
        super(formalParameters);
        this.functionPattern = functionPattern;
        this.returnConstraints = returnConstraints;

        TokenPattern<?>[] innerFunctContent = ((TokenGroup)((TokenStructure)functionPattern).getContents()).getContents();
        voidReturnPattern = innerFunctContent[innerFunctContent.length-1];
    }

    @Override
    public TokenPattern<?> getFunctionPattern() {
        return functionPattern;
    }

    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext declaringCtx, ISymbolContext callingCtx, Object thisObject) {
        SymbolContext innerFrame;

        if(thisObject instanceof CustomClassObject) {
            innerFrame = new ClassMethodSymbolContext(declaringCtx, (CustomClassObject) thisObject);
            for(ClassMethodFamily family : ((CustomClassObject) thisObject).getInstanceMethods().getMethodTable().getAllFamilies()) {
                ((ClassMethodSymbolContext) innerFrame).putClassFunction(family);
            }
            for(Symbol sym : ((CustomClassObject) thisObject).getMemberSymbols()) {
                innerFrame.put(sym);
            }
        } else {
            innerFrame = new SymbolContext(declaringCtx);
        }

        for(int i = 0; i < formalParameters.size(); i++) {
            FormalParameter param = formalParameters.get(i);
            Symbol sym = new Symbol(param.getName(), Symbol.SymbolVisibility.PRIVATE);
            sym.setTypeConstraints(param.getConstraints());
            sym.safeSetValue(i < params.length ? params[i] : null, i < params.length ? patterns[i] : pattern, callingCtx);
            innerFrame.put(sym);
        }
        if(thisObject != null) {
            innerFrame.put(new Symbol("this", Symbol.SymbolVisibility.PRIVATE, thisObject));
        }

        Object returnValue = null;
        TokenPattern<?> returnPattern = voidReturnPattern;
        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, innerFrame, callingCtx.getWritingFile().getFunction());
        } catch(ReturnException x) {
            returnValue = x.getValue();
            returnPattern = x.getPattern();
        }

        if(returnConstraints != null) {
            if(shouldCoerce) {
                returnConstraints.validate(returnValue, returnPattern, callingCtx);
                returnValue = returnConstraints.adjustValue(returnValue, pattern, callingCtx);
            } else {
                returnConstraints.validateExact(returnValue, returnPattern, callingCtx);
            }
        }
        return returnValue;
    }
}
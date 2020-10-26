package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.controlflow.ReturnException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunctionBranch;
import com.energyxxer.prismarine.typesystem.functions.typed.TypedFunction;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.compiler.semantics.symbols.ClassMethodSymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.Collection;

public class TridentUserFunctionBranch extends PrismarineFunctionBranch {
    private TokenPattern<?> functionPattern;
    private TokenPattern<?> voidReturnPattern;

    public TridentUserFunctionBranch(PrismarineTypeSystem typeSystem, Collection<FormalParameter> formalParameters, TokenPattern<?> functionPattern, TypeConstraints returnConstraints) {
        super(typeSystem, formalParameters);
        this.functionPattern = functionPattern;
        this.returnConstraints = returnConstraints;

        TokenPattern<?>[] innerFunctContent = ((TokenGroup)((TokenStructure)functionPattern).getContents()).getContents();
        voidReturnPattern = innerFunctContent[innerFunctContent.length-1];
    }

    @Override
    public TokenPattern<?> getFunctionPattern() {
        return functionPattern;
    }

    public Object call(ActualParameterList params, ISymbolContext declaringCtx, ISymbolContext callingCtx, Object thisObject) {
        SymbolContext innerFrame;

        params.reportInvalidNames(formalParameters, callingCtx);

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
            Symbol sym = new Symbol(param.getName(), TridentSymbolVisibility.PRIVATE);
            sym.setTypeConstraints(param.getConstraints());
            Object[] actualValue = TypedFunction.getActualParameterByFormalIndex(i, formalParameters, params, callingCtx);
            TokenPattern<?> actualValuePattern = ((int) actualValue[1]) < params.size() ? params.getPattern(((int) actualValue[1])) : params.getPattern();

            sym.safeSetValue(
                    actualValue[0],
                    actualValuePattern,
                    callingCtx
            );
            innerFrame.put(sym);
        }
        if(thisObject != null) {
            innerFrame.put(new Symbol("this", TridentSymbolVisibility.PRIVATE, thisObject));
        }

        Object returnValue = null;
        TokenPattern<?> returnPattern = voidReturnPattern;
        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, innerFrame, callingCtx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
        } catch(ReturnException x) {
            returnValue = x.getValue();
            returnPattern = x.getPattern();
        }

        if(returnConstraints != null) {
            if(shouldCoerceReturn) {
                returnConstraints.validate(returnValue, returnPattern, callingCtx);
                returnValue = returnConstraints.adjustValue(returnValue, params.getPattern(), callingCtx);
            } else {
                returnConstraints.validateExact(returnValue, returnPattern, callingCtx);
            }
        }
        return returnValue;
    }
}

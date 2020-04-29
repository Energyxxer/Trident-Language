package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;

public class TridentNativeMethodBranch extends TridentMethodBranch {
    private Method method;

    public TridentNativeMethodBranch(Method method) {
        super(createFormalParameters(method));
        this.method = method;
    }

    private static Collection<FormalParameter> createFormalParameters(Method method) {
        ArrayList<FormalParameter> params = new ArrayList<>();

        Parameter[] parameterJavaTypes = method.getParameters();
        for(Parameter param : parameterJavaTypes) {
            Class<?> paramType = param.getType();
            paramType = TridentMethod.HelperMethods.sanitizeClass(paramType);
            TypeHandler correspondingHandler = TridentTypeManager.getHandlerForHandledClass(paramType);
            if(correspondingHandler == null && paramType != Object.class) {
                throw new IllegalArgumentException("Could not create formal parameter for type '" + paramType.getName() + "'; Did not find appropriate TypeHandler instance.");
            }
            params.add(new FormalParameter(param.getName(), new TypeConstraints(correspondingHandler, correspondingHandler == null)));
        }
        return params;
    }

    @Override
    public TokenPattern<?> getFunctionPattern() {
        return null;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext declaringCtx, ISymbolContext callingCtx, Object thisObject) {
        Object[] actualParams = new Object[formalParameters.size()];

        for(int i = 0; i < actualParams.length; i++) {
            FormalParameter formalParameter = formalParameters.get(i);
            if(i < params.length) {
                actualParams[i] = params[i];
            } else {
                actualParams[i] = null;
            }
            if(formalParameter.getConstraints() != null) {
                formalParameter.getConstraints().validate(actualParams[i], i < params.length ? patterns[i] : pattern, callingCtx);
                actualParams[i] = formalParameter.getConstraints().adjustValue(actualParams[i], i < params.length ? patterns[i] : pattern, callingCtx);
            }
        }

        try {
            return method.invoke(null, actualParams);
        } catch (IllegalAccessException x) {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, x.toString(), pattern, callingCtx);
        } catch (InvocationTargetException x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.getTargetException().getMessage(), pattern, callingCtx);
        }
    }

    public static TridentUserMethod createOverloadedNativeFunction(ISymbolContext ctx, Method... overloads) {
        String name = null;
        ArrayList<TridentMethodBranch> branches = new ArrayList<>();
        for(Method met : overloads) {
            name = met.getName();
            branches.add(new TridentNativeMethodBranch(met));
        }
        return new TridentUserMethod(name, branches, ctx, null);
    }
}

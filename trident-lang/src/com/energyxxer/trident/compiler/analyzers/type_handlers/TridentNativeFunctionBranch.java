package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClassObject;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;

public class TridentNativeFunctionBranch extends TridentFunctionBranch {
    private Method method;

    public TridentNativeFunctionBranch(Method method) {
        super(createFormalParameters(method));
        this.method = method;

        Class<?> returnType = TridentFunction.HelperMethods.sanitizeClass(method.getReturnType());
        TypeHandler correspondingHandler = TridentTypeManager.getHandlerForHandledClass(returnType);
        if(correspondingHandler == null && returnType != Object.class && returnType != null) {
            Debug.log("Could not create return constraints for method '" + method + "': Did not find appropriate TypeHandler instance for class: " + returnType);
        }
        boolean nullable = true;

        if(method.isAnnotationPresent(NativeMethodWrapper.TridentNotNullReturn.class)) {
            nullable = false;
        }

        this.returnConstraints = new TypeConstraints(correspondingHandler, nullable);
    }

    private static Collection<FormalParameter> createFormalParameters(Method method) {
        ArrayList<FormalParameter> params = new ArrayList<>();

        Parameter[] parameterJavaTypes = method.getParameters();
        for(Parameter param : parameterJavaTypes) {
            Class<?> paramType = param.getType();
            paramType = TridentFunction.HelperMethods.sanitizeClass(paramType);
            if(paramType == TokenPattern.class || paramType == ISymbolContext.class || param.isAnnotationPresent(NativeMethodWrapper.TridentThisArg.class)) {
                //Reserved for calling pattern, context and this
                continue;
            }
            TypeHandler correspondingHandler = TridentTypeManager.getHandlerForHandledClass(paramType);
            if(correspondingHandler == null && paramType != Object.class && paramType != CustomClassObject.class) {
                throw new IllegalArgumentException("Could not create formal parameter for type '" + paramType.getName() + "'; Did not find appropriate TypeHandler instance.");
            }

            boolean nullable = correspondingHandler == null;

            NativeMethodWrapper.TridentClassObjectArgument classConstraintAnnot = param.getAnnotation(NativeMethodWrapper.TridentClassObjectArgument.class);
            String classIdentifier = null;
            if(classConstraintAnnot != null) {
                classIdentifier = classConstraintAnnot.classIdentifier();
                nullable = false;
            }

            if(!nullable) {
                nullable = param.getAnnotation(NativeMethodWrapper.TridentNullableArg.class) != null;
            }

            if(classIdentifier == null) {
                params.add(new FormalParameter(param.getName(), new TypeConstraints(correspondingHandler, nullable)));
            } else {
                params.add(new FormalParameter(param.getName(), new TypeConstraints(classIdentifier, nullable)));
            }
        }
        return params;
    }

    @Override
    public TokenPattern<?> getFunctionPattern() {
        return null;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext declaringCtx, ISymbolContext callingCtx, Object thisObject) {
        Object[] actualParams = new Object[method.getParameterCount()];

        int j = 0;
        for(int i = 0; i < method.getParameterCount(); i++,j++) {
            if(method.getParameterTypes()[i] == ISymbolContext.class) {
                actualParams[i] = callingCtx;
                j--;
                continue;
            }
            if(method.getParameterTypes()[i] == TokenPattern.class) {
                actualParams[i] = pattern;
                j--;
                continue;
            }
            if(method.getParameters()[i].isAnnotationPresent(NativeMethodWrapper.TridentThisArg.class)) {
                actualParams[i] = thisObject;
                j--;
                continue;
            }
            FormalParameter formalParameter = formalParameters.get(j);
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

        Object returnValue;
        try {
            returnValue = method.invoke(null, actualParams);
        } catch (IllegalAccessException x) {
            throw new TridentException(TridentException.Source.IMPOSSIBLE, x.toString(), pattern, callingCtx);
        } catch (InvocationTargetException x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.getTargetException().getMessage(), pattern, callingCtx);
        }

        if(returnConstraints != null) {
            if(shouldCoerce) {
                returnConstraints.validate(returnValue, null, callingCtx);
                returnValue = returnConstraints.adjustValue(returnValue, pattern, callingCtx);
            } else {
                returnConstraints.validateExact(returnValue, null, callingCtx);
            }
        }
        return returnValue;
    }

    public static TridentUserFunction nativeMethodsToFunction(ISymbolContext ctx, Method met) {
        return nativeMethodsToFunction(ctx, null, met);
    }

    public static TridentUserFunction nativeMethodsToFunction(ISymbolContext ctx, String name, Method met) {
        if(name == null) name = met.getName();
        return new TridentUserFunction(name, new TridentNativeFunctionBranch(met), ctx, null);
    }
}

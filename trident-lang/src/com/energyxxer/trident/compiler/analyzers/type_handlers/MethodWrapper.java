package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.trident.compiler.semantics.CallStack;
import com.energyxxer.trident.compiler.semantics.TridentException;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodWrapper<T> implements MemberWrapper<T> {
    public interface Invoker<T> {
        Object invoke(T instance, Object... params) throws Exception;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface TridentNullable {
    }

    private final String methodName;
    private final Invoker<T> invoker;
    private final Class<?>[] paramTypes;
    private final boolean[] nullables;
    private int requiredSize;

    public MethodWrapper(Method method) {
        this(method.getName(), method);
    }

    public MethodWrapper(String methodName, Method method) {
        this.methodName = methodName;
        this.invoker = method::invoke;
        this.paramTypes = method.getParameterTypes();
        this.nullables = new boolean[paramTypes.length];
        requiredSize = paramTypes.length;

        int i = 0;
        for(Annotation[] param : method.getParameterAnnotations()) {
            for(Annotation annot : param) {
                if(annot.annotationType() == TridentNullable.class) {
                    nullables[i] = true;
                    break;
                }
            }
            i++;
        }
    }

    public MethodWrapper(String methodName, Invoker<T> invoker, Class<?>... paramTypes) {
        this.methodName = methodName;
        this.invoker = invoker;
        this.paramTypes = paramTypes;
        this.nullables = new boolean[paramTypes.length];
        requiredSize = paramTypes.length;
    }

    public MethodWrapper<T> setNullable(int index) {
        nullables[index] = true;
        requiredSize = paramTypes.length;
        for(int i = requiredSize - 1; i >= 0; i--) {
            if(nullables[i]) requiredSize = i;
            else break;
        }
        return this;
    }

    public VariableMethod createForInstance(T instance) {
        return (params, patterns, pattern, file) -> {
            file.getCompiler().getCallStack().push(new CallStack.Call(methodName, null, null, pattern));
            try {
                if (params.length < requiredSize) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method '" + methodName + "' requires " + (requiredSize != paramTypes.length ? "at least " : "") + requiredSize + " parameter" + (requiredSize == 1 ? "" : "s") + ", instead found " + params.length, pattern, file);
                }
                int i = 0;
                for (Class<?> cls : paramTypes) {
                    if (i < requiredSize) {
                        if (params[i] != null || !nullables[i])
                            params[i] = VariableMethod.HelperMethods.assertOfType(params[i], patterns[i], file, cls);
                    } else { //if we reach this, the remaining parameter types are nullable
                        if (i >= params.length) break;
                        if (i < paramTypes.length) { //param is present
                            if (params[i] != null) {
                                params[i] = VariableMethod.HelperMethods.assertOfType(params[i], patterns[i], file, cls);
                            }
                        } else break;
                    }
                    i++;
                }

                try {
                    Object[] actualParams = new Object[paramTypes.length];
                    for (i = 0; i < paramTypes.length; i++) {
                        if(i < params.length) {
                            actualParams[i] = params[i];
                        }
                    }
                    return invoker.invoke(instance, actualParams);
                } catch(TridentException | TridentException.Grouped x) {
                    throw x;
                } catch (Exception x) {
                    if(x instanceof InvocationTargetException) {
                        if(((InvocationTargetException) x).getTargetException() instanceof CommodoreException) {
                            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, ((InvocationTargetException) x).getTargetException().getMessage(), pattern, file);
                        } else {
                            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, ((InvocationTargetException) x).getTargetException().toString(), pattern, file);
                        }
                    } else {
                        throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.toString(), pattern, file);
                    }
                }
            } finally {
                file.getCompiler().getCallStack().pop();
            }
        };
    }


    @Override
    public Object unwrap(T instance) {
        return createForInstance(instance);
    }
}

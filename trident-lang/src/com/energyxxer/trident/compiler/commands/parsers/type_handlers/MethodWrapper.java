package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import manifold.ext.api.Self;

import java.lang.reflect.Method;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class MethodWrapper<T> implements MemberWrapper<T> {
    public interface Invoker<T> {
        Object invoke(T instance, Object... params) throws Exception;
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
    }

    public MethodWrapper(String methodName, Invoker<T> invoker, Class<?>... paramTypes) {
        this.methodName = methodName;
        this.invoker = invoker;
        this.paramTypes = paramTypes;
        this.nullables = new boolean[paramTypes.length];
        requiredSize = paramTypes.length;
    }

    @Self
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
            if(params.length < requiredSize) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method '" + methodName + "' requires " + (requiredSize != paramTypes.length ? "at least " : "") + requiredSize + " parameter" + (requiredSize == 1 ? "" : "s") + ", instead found " + params.length, pattern));
                throw new EntryParsingException();
            }
            int i = 0;
            for(Class<?> cls : paramTypes) {
                if(i < requiredSize) {
                    if(params[i] != null || !nullables[i]) params[i] = assertOfType(params[i], patterns[i], file, cls);
                } else { //if we reach this, the remaining parameter types are nullable
                    if(i >= params.length) break;
                    if(i < paramTypes.length) { //param is present
                        if(params[i] != null) {
                            params[i] = assertOfType(params[i], patterns[i], file, cls);
                        }
                    } else break;
                }
                i++;
            }

            try {
                Object[] actualParams = new Object[paramTypes.length];
                for(i = 0; i < paramTypes.length; i++) {
                    actualParams[i] = params[i];
                }
                return invoker.invoke(instance, actualParams);
            } catch (Exception x) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), pattern));
                throw new EntryParsingException();
            }
        };
    }


    @Override
    public Object unwrap(T instance) {
        return createForInstance(instance);
    }
}

package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;

import java.lang.reflect.Method;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class MethodWrapper<T> implements MemberWrapper<T> {
    public interface Invoker<T> {
        Object invoke(T instance, Object... params) throws Exception;
    }

    private final String methodName;
    private final Invoker<T> invoker;
    private final Class<?>[] paramTypes;

    public MethodWrapper(Method method) {
        this(method.getName(), method);
    }

    public MethodWrapper(String methodName, Method method) {
        this.methodName = methodName;
        this.invoker = method::invoke;
        this.paramTypes = method.getParameterTypes();
    }

    public MethodWrapper(String methodName, Invoker<T> invoker, Class<?>... paramTypes) {
        this.methodName = methodName;
        this.invoker = invoker;
        this.paramTypes = paramTypes;
    }

    public VariableMethod createForInstance(T instance) {
        return (params, patterns, pattern, file) -> {
            if(params.length < paramTypes.length) {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method '" + methodName + "' requires " + paramTypes.length + " parameter" + (paramTypes.length == 1 ? "" : "s") + ", instead found " + params.length, pattern));
                throw new EntryParsingException();
            }
            int i = 0;
            for(Class<?> cls : paramTypes) {
                assertOfType(params[i], patterns[i], file, cls);
                i++;
            }

            try {
                return invoker.invoke(instance, params);
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

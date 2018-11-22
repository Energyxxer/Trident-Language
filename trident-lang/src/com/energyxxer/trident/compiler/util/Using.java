package com.energyxxer.trident.compiler.util;

import java.util.HashMap;
import java.util.Map;

public class Using<T> {
    public T using;
    private boolean runIfNull = true;
    private HashMap<Class<? extends Exception>, Catch<T>> catches = new HashMap<>();

    public Using(T using) {
        this.using = using;
    }

    public Using<T> run(Runnable<T> action) {
        if(using != null || runIfNull) {
            if(catches.isEmpty()) {
                action.run(using);
            } else {
                try {
                    action.run(using);
                } catch(Exception x) {
                    boolean caught = false;
                    for(Map.Entry<Class<? extends Exception>, Catch<T>> entry : catches.entrySet()) {
                        if(entry.getKey().isAssignableFrom(x.getClass())) {
                            entry.getValue()._catch(x, using);
                            caught = true;
                        }
                    }
                    if(!caught) throw x;
                }
            }
        }
        return this;
    }

    public Using<T> notIfNull() {
        runIfNull = false;
        return this;
    }

    public Using<T> otherwise(Runnable<T> action) {
        if(using == null && !runIfNull) {
            action.run(using);
        }
        return this;
    }

    public Using<T> except(Class<? extends Exception> exceptionCls, Catch<T> _catch) {
        this.catches.put(exceptionCls, _catch);
        return this;
    }

    public static <T> Using<T> using(T object) {
        return new Using<>(object);
    }

    public interface Catch<T> {
        void _catch(Exception ex, T obj);
    }
}
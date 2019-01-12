package com.energyxxer.trident.compiler.commands.parsers.type_handlers.constructors;

import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod;

import java.util.HashMap;

public class ObjectConstructors {
    private static HashMap<String, VariableMethod> constructors = new HashMap<>();

    static {
        constructors.put("int_range",
                new MethodWrapper<>("new int_range", ((instance, params) -> {
                    if(params[0] == null && params[1] == null) {
                        throw new IllegalArgumentException("Both min and max bounds cannot be null");
                    }
                    return new NumberRange<>((Integer)params[0], (Integer)params[1]);
                }), Integer.class, Integer.class).setNullable(0).setNullable(1)
                        .createForInstance(null));

        constructors.put("real_range",
                new MethodWrapper<>("new real_range", ((instance, params) -> {
                    if(params[0] == null && params[1] == null) {
                        throw new IllegalArgumentException("Both min and max bounds cannot be null");
                    }
                    return new NumberRange<>((Double)params[0], (Double)params[1]);
                }), Double.class, Double.class).setNullable(0).setNullable(1)
                        .createForInstance(null));
    }

    public static VariableMethod getConstructor(String name) {
        return constructors.get(name);
    }
}

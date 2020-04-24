package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;

public class FormalParameter {
    private String name;
    private TypeConstraints constraints;

    public FormalParameter(String name, TypeConstraints constraints) {
        this.name = name;
        this.constraints = constraints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(TypeConstraints constraints) {
        this.constraints = constraints;
    }

    @Override
    public String toString() {
        return name + (constraints != null ? " : " + constraints : "");
    }
}

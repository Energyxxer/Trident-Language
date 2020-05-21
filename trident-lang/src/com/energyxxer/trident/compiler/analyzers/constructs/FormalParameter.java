package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormalParameter that = (FormalParameter) o;
        return Objects.equals(constraints, that.constraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints);
    }

    public static boolean parameterListEquals(List<FormalParameter> a, List<FormalParameter> b) {
        if(a.size() != b.size()) return false;
        for(int i = 0; i < a.size(); i++) {
            if(!TypeConstraints.constraintsEqual(a.get(i).constraints, b.get(i).constraints)) {
                return false;
            }
        }
        return true;
    }
}

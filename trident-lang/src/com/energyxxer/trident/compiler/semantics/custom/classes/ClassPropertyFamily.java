package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassPropertyFamily {
    //Ready to add property overloading in the future when needed
    private ClassProperty property = null;
    private final ArrayList<ClashingInheritedProperties> clashingInheritedProperties = new ArrayList<>();

    private boolean superClassesRequireSetter = false;
    private SymbolVisibility superClassesGetterVisibility = TridentSymbolVisibility.PRIVATE;
    private SymbolVisibility superClassesSetterVisibility = TridentSymbolVisibility.PRIVATE;

    public void put(ClassProperty newProperty, CustomClass.MemberParentMode mode, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(newProperty == null) return;
        if(property == null || mode == CustomClass.MemberParentMode.FORCE) {
            this.property = newProperty;
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                superClassesRequireSetter = superClassesRequireSetter || newProperty.getSetterFunction() != null;
                superClassesGetterVisibility = SymbolVisibility.max(superClassesGetterVisibility, newProperty.getGetterVisibility());
                superClassesSetterVisibility = SymbolVisibility.max(superClassesSetterVisibility, newProperty.getSetterVisibility());
            }
            return;
        }

        ClassProperty oldProperty = property;

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            if(oldProperty.getDefiningClass() != newProperty.getDefiningClass() && oldProperty.getDefiningClass() != ((TridentTypeSystem) ctx.getTypeSystem()).getBaseClass()) {
                //Make note for later
                registerClashingProperties(oldProperty, newProperty);
            } else {
                Debug.log("Skipping " + newProperty + " since defined in same class: " + newProperty.getDefiningClass());
            }
        } else {
            if(oldProperty.getDefiningClass() == newProperty.getDefiningClass()) {
                throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicate definition for class property", pattern, ctx);
            } else if(mode != CustomClass.MemberParentMode.OVERRIDE) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Property " + oldProperty + " is already defined in inherited class " + newProperty.getDefiningClass().getTypeIdentifier() + " with the same parameter type. Use the 'override' keyword to replace it", pattern, ctx);
            }
        }

        if(!oldProperty.getDefiningClass().hasAccess(ctx, oldProperty.getGetterVisibility())) {
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + oldProperty + ": Getter has " + oldProperty.getGetterVisibility().toString().toLowerCase() + " access in " + oldProperty.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldProperty + ": Getter has " + oldProperty.getGetterVisibility().toString().toLowerCase() + " access in " + oldProperty.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }
        if(oldProperty.getSetterFunction() != null && !oldProperty.getDefiningClass().hasAccess(ctx, oldProperty.getSetterVisibility())) {
            if(mode == CustomClass.MemberParentMode.INHERIT) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + oldProperty + ": Setter has " + oldProperty.getSetterVisibility().toString().toLowerCase() + " access in " + oldProperty.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            } else {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldProperty + ": Setter has " + oldProperty.getSetterVisibility().toString().toLowerCase() + " access in " + oldProperty.getDefiningClass().getTypeIdentifier() + "; not accessible from " + newProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            //If inherited, just require the resolving implementation to specify a setter; done at the end of this method
        } else {
            if (superClassesRequireSetter && newProperty.getSetterFunction() == null) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + oldProperty + ": required setter not present in overriding property", pattern, ctx);
            }
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            //If inherited, just get the most general visibility; done at the end of this method
        } else {
            if(newProperty.getGetterVisibility().getVisibilityIndex() < superClassesGetterVisibility.getVisibilityIndex()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override getter for " + oldProperty + ": attempting to assign weaker access privileges '" + newProperty.getGetterVisibility().toString().toLowerCase() + "', was '" + superClassesGetterVisibility.toString().toLowerCase() + "'", pattern, ctx);
            }
            if(newProperty.getSetterFunction() != null && newProperty.getSetterVisibility().getVisibilityIndex() < superClassesSetterVisibility.getVisibilityIndex()) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override setter for " + oldProperty + ": attempting to assign weaker access privileges '" + newProperty.getSetterVisibility().toString().toLowerCase() + "', was '" + superClassesSetterVisibility.toString().toLowerCase() + "'", pattern, ctx);
            }
        }


        if(mode == CustomClass.MemberParentMode.INHERIT) {
            if(!TypeConstraints.constraintsEqual(oldProperty.getGetterFunction().getBranch().getReturnConstraints(), newProperty.getGetterFunction().getBranch().getReturnConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Clashing inherited implementations of " + oldProperty + ": incompatible getter return constraints.\n    Constraint:        " + newProperty.getGetterFunction().getBranch().getReturnConstraints() + " in " + newProperty.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldProperty.getGetterFunction().getBranch().getReturnConstraints() + " in " + oldProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
            if(oldProperty.getSetterFunction() != null && newProperty.getSetterFunction() != null && !TypeConstraints.constraintsEqual(newProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints(), oldProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Clashing inherited implementations of " + oldProperty + ": incompatible setter value constraints.\n    Constraint:        " + newProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + newProperty.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + oldProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        } else {
            if(!TypeConstraints.constraintAContainsB(oldProperty.getGetterFunction().getBranch().getReturnConstraints(), newProperty.getGetterFunction().getBranch().getReturnConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + oldProperty + " due to incompatible getter return constraints.\n    Constraint:        " + newProperty.getGetterFunction().getBranch().getReturnConstraints() + " in " + newProperty.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldProperty.getGetterFunction().getBranch().getReturnConstraints() + " in " + oldProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
            if(oldProperty.getSetterFunction() != null && newProperty.getSetterFunction() != null && !TypeConstraints.constraintAContainsB(newProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints(), oldProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints())) {
                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + oldProperty + " due to incompatible setter value constraints.\n    Constraint:        " + newProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + newProperty.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + oldProperty.getSetterFunction().getBranch().getFormalParameters().get(1).getConstraints() + " in " + oldProperty.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }

        if(mode == CustomClass.MemberParentMode.INHERIT) {
            superClassesRequireSetter = superClassesRequireSetter || newProperty.getSetterFunction() != null;
            superClassesGetterVisibility = SymbolVisibility.max(superClassesGetterVisibility, newProperty.getGetterVisibility());
            superClassesSetterVisibility = SymbolVisibility.max(superClassesSetterVisibility, newProperty.getSetterVisibility());
        } else {
            this.property = newProperty;
        }
    }

    public ClassProperty get() {
        return property;
    }

    public void putAll(ClassPropertyFamily others, TokenPattern<?> pattern, ISymbolContext ctx) {
        put(others.get(), CustomClass.MemberParentMode.INHERIT, pattern, ctx);
    }

    private void registerClashingProperties(ClassProperty existing, ClassProperty method) {
        for(ClashingInheritedProperties clashingProperties : clashingInheritedProperties) {
            clashingProperties.addProperty(method);
            return;
        }
        //Could not find registered clashes, add both existing and method
        clashingInheritedProperties.add(new ClashingInheritedProperties(existing, method));
    }

    public void checkClashingInheritedPropertiesResolved(CustomClass resolvingClass, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(property == null) return;
        if(!clashingInheritedProperties.isEmpty() && property.getDefiningClass() != resolvingClass) {
            //Did not change
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Property '" + property.getName() + "' is defined in multiple inherited classes: " + clashingInheritedProperties.get(0).getProperties().stream().map(m -> m.getDefiningClass().getTypeIdentifier()).collect(Collectors.joining(", ")) + ". Override it in the class body", pattern, ctx);
        }
    }

    private static class ClashingInheritedProperties {
        List<ClassProperty> properties;

        public ClashingInheritedProperties(ClassProperty a, ClassProperty b) {
            properties = new ArrayList<>();
            properties.add(a);
            properties.add(b);
        }

        public void addProperty(ClassProperty property) {
            properties.add(property);
        }

        public List<ClassProperty> getProperties() {
            return properties;
        }

        @Override
        public String toString() {
            return properties.get(0).toString();
        }
    }
}

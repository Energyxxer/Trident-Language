package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunctionBranch;
import com.energyxxer.prismarine.typesystem.functions.typed.TypedFunctionFamily;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.util.TridentTempFindABetterHome;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassMethodFamily extends TypedFunctionFamily<ClassMethod> {
    private final ArrayList<ClashingInheritedMethods> clashingInheritedMethods = new ArrayList<>();

    public ClassMethodFamily(String name) {
        super(name);
    }

    @Override
    protected void validatePickedOverload(ClassMethod bestMatch, ActualParameterList params, ISymbolContext ctx) {
        if(!bestMatch.getDefiningClass().hasAccess(ctx, bestMatch.getVisibility())) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, bestMatch + " has " + bestMatch.getVisibility().toString().toLowerCase() + " access in " + bestMatch.getDefiningClass().getClassTypeIdentifier(), params.getPattern(), ctx);
        }
        if(params.hasNames() && bestMatch.isNamedParameterCallsDisabled()) {
            throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, bestMatch + " has mismatched parameter names with its overriding method.\nNamed parameters have been disabled for calls to this method", params.getPattern(), ctx);
        }
    }

    public void putOverload(ClassMethod method, CustomClass.MemberParentMode mode, TokenPattern<?> pattern, ISymbolContext ctx) {
        ClassMethod existing = findOverloadForParameters(method.getFormalParameters());
        if(mode != CustomClass.MemberParentMode.FORCE) {
            if(mode == CustomClass.MemberParentMode.CREATE && existing != null) {
                if(existing.getDefiningClass() == method.getDefiningClass()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Duplicate method " + existing + ": it's already defined with the same parameter types in the same class", pattern, ctx);
                } else {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Method " + existing + " is already defined in inherited class " + existing.getDefiningClass().getTypeIdentifier() + " with the same parameter types. Use the 'override' keyword to replace it", pattern, ctx);
                }
            }
            if(mode == CustomClass.MemberParentMode.INHERIT && existing != null) {
                if(existing.getDefiningClass() != method.getDefiningClass() && existing.getDefiningClass() != ((TridentTypeSystem) ctx.getTypeSystem()).getBaseClass()) {
                    //Make note for later
                    registerClashingMethods(existing, method);
                }
            }
            if(existing == null && mode == CustomClass.MemberParentMode.OVERRIDE) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + method + ": couldn't find existing overload with parameter types: " + method.getFormalParameters().toString(), pattern, ctx);
            }

            if(existing != null) {
                if(mode == CustomClass.MemberParentMode.OVERRIDE && existing.getDefiningClass() == method.getDefiningClass()) {
                    throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "Cannot override " + existing + ": a branch with the same parameter types is already defined in the same class", pattern, ctx);
                }

                if(existing.getModifiers() != null && existing.getModifiers().hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL)) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": it's defined as final in " + existing.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    } else {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": it's defined as final in " + existing.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    }
                }

                //Check visibility
                SymbolVisibility existingVisibility = existing.getVisibility();
                SymbolVisibility newVisibility = method.getVisibility();

                if(!method.getDefiningClass().hasAccess(ctx, existingVisibility)) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": it has " + existingVisibility.toString().toLowerCase() + " access in " + existing.getDefiningClass().getTypeIdentifier() + "; not accessible from " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    } else {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": it has " + existingVisibility.toString().toLowerCase() + " access in " + existing.getDefiningClass().getTypeIdentifier() + "; not accessible from " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    }
                }
                if(newVisibility.getVisibilityIndex() < existingVisibility.getVisibilityIndex()) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": attempting to assign weaker access privileges '" + newVisibility.toString().toLowerCase() + "', was '" + existingVisibility.toString().toLowerCase() + "'", pattern, ctx);
                    } else {
                        throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": attempting to assign weaker access privileges '" + newVisibility.toString().toLowerCase() + "', was '" + existingVisibility.toString().toLowerCase() + "'", pattern, ctx);
                    }
                }

                //Check return types
                PrismarineFunctionBranch existingBranch = existing.getFunction().getBranch();
                PrismarineFunctionBranch newBranch = method.getFunction().getBranch();
                TypeConstraints existingReturnConstraints = existingBranch.getReturnConstraints();
                TypeConstraints newReturnConstraints = newBranch.getReturnConstraints();

                if(mode == CustomClass.MemberParentMode.INHERIT && !TypeConstraints.constraintsEqual(existingReturnConstraints, newReturnConstraints)) {
                    throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": Incompatible return constraints.\n    Constraint:        " + existingReturnConstraints + " in " + existing.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newReturnConstraints + " in " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                }

                if(!TypeConstraints.constraintAContainsB(existingReturnConstraints, newReturnConstraints)) {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot override " + existing + " due to incompatible return constraints.\n    Constraint:        " + existingReturnConstraints + " in " + existing.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newReturnConstraints + " in " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                }

                //Check parameter names (warn)

                for(int i = 0; i < existing.getFormalParameters().size(); i++) {
                    FormalParameter existingParam = existing.getFormalParameters().get(i);
                    FormalParameter newParam = method.getFormalParameters().get(i);

                    if(!Objects.equals(existingParam.getName(), newParam.getName())) {
                        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING,
                                "Method '" + method + "' has a different parameter name for position " + i + " to that of the method it is overriding." +
                                        "\nNamed function arguments will be unusable with this method." +
                                        "\n    Parameter name in " + existing.getDefiningClass().getTypeIdentifier() + ": " + existingParam.getName() + "" +
                                        "\n    Parameter name in " + method.getDefiningClass().getTypeIdentifier() + ": " + newParam.getName(), pattern)
                        );
                        method.disableNamedParameterCalls();
                    }

                }
            }


            //if(existing != null && existing.getFunction().getBranch())
        }
        if(existing != null) implementations.remove(existing);
        implementations.add(method);
    }

    private void registerClashingMethods(ClassMethod existing, ClassMethod method) {
        for(ClashingInheritedMethods clashingMethods : clashingInheritedMethods) {
            if(clashingMethods.matches(method)) {
                clashingMethods.addMethod(method);
                return;
            }
        }
        //Could not find registered clashes, add both existing and method
        clashingInheritedMethods.add(new ClashingInheritedMethods(existing, method));
    }

    public void checkClashingInheritedMethodsResolved(CustomClass resolvingClass, TokenPattern<?> pattern, ISymbolContext ctx) {
        for(ClashingInheritedMethods clashingMethods : clashingInheritedMethods) {
            if(findOverloadForParameters(clashingMethods.getFormalParameters()).getDefiningClass() != resolvingClass) {
                //Did not change
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Method '" + clashingMethods + "' is defined in multiple inherited classes: " + clashingMethods.getMethods().stream().map(m -> m.getDefiningClass().getTypeIdentifier()).collect(Collectors.joining(", ")) + ". Override it in the class body", pattern, ctx);
            }
        }
    }

    public ArrayList<ClassMethod> getImplementations() {
        return implementations;
    }

    private static class ClashingInheritedMethods {
        List<FormalParameter> formalParameters;
        List<ClassMethod> methods;

        public ClashingInheritedMethods(ClassMethod a, ClassMethod b) {
            methods = new ArrayList<>();
            methods.add(a);
            methods.add(b);
            formalParameters = a.getFormalParameters();
        }

        public void addMethod(ClassMethod method) {
            methods.add(method);
        }

        public boolean matches(ClassMethod method) {
            return FormalParameter.parameterListEquals(formalParameters, method.getFormalParameters());
        }

        public List<FormalParameter> getFormalParameters() {
            return formalParameters;
        }

        public List<ClassMethod> getMethods() {
            return methods;
        }

        @Override
        public String toString() {
            return methods.get(0).toString();
        }
    }

    public void setUseExternalThis(boolean useExternalThis) {
        this.useExternalThis = useExternalThis;
    }
}

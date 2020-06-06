package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunctionBranch;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassMethodFamily {
    private final String name;
    private final ArrayList<ClassMethod> implementations = new ArrayList<>();

    private final ArrayList<ClashingInheritedMethods> clashingInheritedMethods = new ArrayList<>();

    public ClassMethodFamily(String name) {
        this.name = name;
    }

    public ClassMethodSymbol pickOverloadSymbol(ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx, CustomClassObject thisObject) {
        return new ClassMethodSymbol(this, pickOverload(params, pattern, ctx), thisObject);
    }

    public TridentUserFunction pickOverload(ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<ClassMethod> bestScoreBranchMatches = new ArrayList<>();
        double bestScore = -1;

        //TridentFunctionBranch bestPick = null;
        //boolean bestPickFullyMatched = false;

        for(ClassMethod method : implementations) {
            List<FormalParameter> branchParams = method.getFormalParameters();
            boolean branchMatched = true;
            double score = 0;
            for(int i = 0; i < branchParams.size() && branchMatched; i++) {
                FormalParameter formalParam = branchParams.get(i);
                Object actualParam = null;
                if(i < params.getValues().size()) actualParam = params.getValues().get(i);
                int paramScore = 1;
                if(formalParam.getConstraints() != null) {
                    paramScore = formalParam.getConstraints().rateMatch(actualParam);
                }
                if(paramScore == 0) {
                    branchMatched = false;
                    score = 0;
                }
                score += paramScore;
            }
            if(!branchParams.isEmpty()) score /= branchParams.size();
            else {
                score = 4;
            }
            if(branchMatched && score >= bestScore) {
                if(score != bestScore) bestScoreBranchMatches.clear();
                bestScore = score;
                bestScoreBranchMatches.add(method);
            }
        }
        if(bestScoreBranchMatches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean any = false;
            for(Object obj : params.getValues()) {
                sb.append(TridentTypeManager.getTypeIdentifierForObject(obj));
                sb.append(", ");
                any = true;
            }
            if(any) {
                sb.setLength(sb.length()-2);
            }
            StringBuilder overloads = new StringBuilder();
            for(ClassMethod method : this.implementations) {
                overloads.append("\n    ").append(name).append("(");
                overloads.append(method.getFormalParameters().toString().substring(1));
                overloads.setLength(overloads.length()-1);
                overloads.append(")");
            }
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Overload not found for parameter types: (" + sb.toString() + ")\nValid overloads are:" + overloads.toString(), pattern, ctx);
        }
        ClassMethod bestMatch = bestScoreBranchMatches.get(0);
        if(bestScoreBranchMatches.size() > 1) {
            int sameLengthMatches = 0;
            for(ClassMethod branch : bestScoreBranchMatches) {
                if(branch.getFormalParameters().size() == params.size()) {
                    bestMatch = branch;
                    sameLengthMatches++;
                }
            }
            if(sameLengthMatches > 1) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Ambiguous call between: " + bestScoreBranchMatches.stream().filter(b->b.getFormalParameters().size() == params.size()).map(b -> b.getFormalParameters().toString()).collect(Collectors.joining(", ")), pattern, ctx);
            } else if(sameLengthMatches < 1) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, "Ambiguous call between: " + bestScoreBranchMatches.stream().map(b -> b.getFormalParameters().toString()).collect(Collectors.joining(", ")), pattern, ctx);
            }
        }

        if(!bestMatch.getDefiningClass().hasAccess(ctx, bestMatch.getVisibility())) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, bestMatch + " has " + bestMatch.getVisibility().toString().toLowerCase() + " access in " + bestMatch.getDefiningClass().getClassTypeIdentifier(), pattern, ctx);
        }

        return bestMatch.getFunction();
    }

    public String getName() {
        return name;
    }

    public void putOverload(ClassMethod method, CustomClass.MemberParentMode mode, TokenPattern<?> pattern, ISymbolContext ctx) {
        ClassMethod existing = findOverloadForParameters(method.getFormalParameters());
        if(mode != CustomClass.MemberParentMode.FORCE) {
            if(mode == CustomClass.MemberParentMode.CREATE && existing != null) {
                if(existing.getDefiningClass() == method.getDefiningClass()) {
                    throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Duplicate method " + existing + ": it's already defined with the same parameter types in the same class", pattern, ctx);
                } else {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Method " + existing + " is already defined in inherited class " + existing.getDefiningClass().getTypeIdentifier() + " with the same parameter types. Use the 'override' keyword to replace it", pattern, ctx);
                }
            }
            if(mode == CustomClass.MemberParentMode.INHERIT && existing != null) {
                if(existing.getDefiningClass() == method.getDefiningClass()) {
                    Debug.log("Skipping " + method + " since defined in same class: " + method.getDefiningClass());
                } else {
                    //Make note for later
                    registerClashingMethods(existing, method);
                }
            }
            if(existing == null && mode == CustomClass.MemberParentMode.OVERRIDE) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override " + method + ": couldn't find existing overload with parameter types: " + method.getFormalParameters().toString(), pattern, ctx);
            }

            if(existing != null) {
                if(mode == CustomClass.MemberParentMode.OVERRIDE && existing.getDefiningClass() == method.getDefiningClass()) {
                    throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Cannot override " + existing + ": a branch with the same parameter types is already defined in the same class", pattern, ctx);
                }

                if(existing.getModifiers() != null && existing.getModifiers().hasModifier(Symbol.SymbolModifier.FINAL)) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": it's defined as final in " + existing.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    } else {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": it's defined as final in " + existing.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    }
                }

                //Check visibility
                Symbol.SymbolVisibility existingVisibility = existing.getVisibility();
                Symbol.SymbolVisibility newVisibility = method.getVisibility();

                if(!method.getDefiningClass().hasAccess(ctx, existingVisibility)) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": it has " + existingVisibility.toString().toLowerCase() + " access in " + existing.getDefiningClass().getTypeIdentifier() + "; not accessible from " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    } else {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": it has " + existingVisibility.toString().toLowerCase() + " access in " + existing.getDefiningClass().getTypeIdentifier() + "; not accessible from " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                    }
                }
                if(newVisibility.getVisibilityIndex() < existingVisibility.getVisibilityIndex()) {
                    if(mode == CustomClass.MemberParentMode.INHERIT) {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": attempting to assign weaker access privileges '" + newVisibility.toString().toLowerCase() + "', was '" + existingVisibility.toString().toLowerCase() + "'", pattern, ctx);
                    } else {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override " + existing + ": attempting to assign weaker access privileges '" + newVisibility.toString().toLowerCase() + "', was '" + existingVisibility.toString().toLowerCase() + "'", pattern, ctx);
                    }
                }

                //Check return types
                TridentFunctionBranch existingBranch = existing.getFunction().getBranch();
                TridentFunctionBranch newBranch = method.getFunction().getBranch();
                TypeConstraints existingReturnConstraints = existingBranch.getReturnConstraints();
                TypeConstraints newReturnConstraints = newBranch.getReturnConstraints();

                if(mode == CustomClass.MemberParentMode.INHERIT && !TypeConstraints.constraintsEqual(existingReturnConstraints, newReturnConstraints)) {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Clashing inherited implementations of " + existing + ": Incompatible return constraints.\n    Constraint:        " + existingReturnConstraints + " in " + existing.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newReturnConstraints + " in " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                }

                if(!TypeConstraints.constraintAContainsB(existingReturnConstraints, newReturnConstraints)) {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot override " + existing + " due to incompatible return constraints.\n    Constraint:        " + existingReturnConstraints + " in " + existing.getDefiningClass().getTypeIdentifier() + "\n    clashes with:    " + newReturnConstraints + " in " + method.getDefiningClass().getTypeIdentifier(), pattern, ctx);
                }

            }


            //if(existing != null && existing.getFunction().getBranch())
        }
        if(existing != null) implementations.remove(existing);
        implementations.add(method);
    }

    public ClassMethod findOverloadForParameters(List<FormalParameter> types) {
        for(ClassMethod method : implementations) {
            if(FormalParameter.parameterListEquals(method.getFormalParameters(), types)) {
                return method;
            }
        }
        return null;
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
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Method '" + clashingMethods + "' is defined in multiple inherited classes: " + clashingMethods.getMethods().stream().map(m -> m.getDefiningClass().getTypeIdentifier()).collect(Collectors.joining(", ")) + ". Override it in the class body", pattern, ctx);
            }
        }
    }

    public ArrayList<ClassMethod> getImplementations() {
        return implementations;
    }

    public static class ClassMethodSymbol extends Symbol {
        private TridentUserFunction pickedOverload;
        private CustomClassObject thisObject;

        public ClassMethodSymbol(ClassMethodFamily classMethodFamily, TridentUserFunction pickedOverload, CustomClassObject thisObject) {
            super(classMethodFamily.name);
            this.pickedOverload = pickedOverload;
            this.thisObject = thisObject;
        }

        public TridentUserFunction getPickedOverload() {
            return pickedOverload;
        }

        public CustomClassObject getThisObject() {
            return thisObject;
        }

        public Object safeCall(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
            pickedOverload.setThisObject(thisObject);
            Object returnValue = pickedOverload.safeCall(params, patterns, pattern, ctx);
            pickedOverload.setThisObject(null);
            return returnValue;
        }
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
}

package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.ActualParameterList;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassMethodFamily {
    private final String name;
    private final ArrayList<ClassMethod> overloads = new ArrayList<>();

    public ClassMethodFamily(String name) {
        this.name = name;
    }

    public ClassMethodSymbol pickOverloadSymbol(ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx, CustomClassObject thisObject) {
        return new ClassMethodSymbol(this, pickOverload(params, pattern, ctx), thisObject);
    }

    public TridentUserFunction pickOverload(ActualParameterList params, TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<ClassMethod> bestScoreBranchMatches = new ArrayList<>();
        double bestScore = -1;
        boolean foundSameLengthMatch = false;

        //TridentFunctionBranch bestPick = null;
        //boolean bestPickFullyMatched = false;

        for(ClassMethod method : overloads) {
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
            /*if(!branchParams.isEmpty()) score /= branchParams.size();
            else {
                score = 4;
            }*/
            boolean firstSameLengthMatch = !foundSameLengthMatch && branchParams.size() == params.getValues().size();
            if(branchMatched && score >= bestScore || firstSameLengthMatch) {
                if(score != bestScore || firstSameLengthMatch) bestScoreBranchMatches.clear();
                if(!foundSameLengthMatch || branchParams.size() == params.getValues().size()) {
                    bestScore = score;
                    bestScoreBranchMatches.add(method);
                    if(firstSameLengthMatch) foundSameLengthMatch = true;
                }
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
            for(ClassMethod method : this.overloads) {
                overloads.append("\n    ").append(name).append("(");
                overloads.append(method.getFormalParameters().toString().substring(1));
                overloads.setLength(overloads.length()-1);
                overloads.append(")");
            }
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Overload not found for parameter types: (" + sb.toString() + ")\nValid overloads are:" + overloads.toString(), pattern, ctx);
        }
        if(bestScoreBranchMatches.size() > 1) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Ambiguous call between: " + bestScoreBranchMatches.stream().map(b -> b.getFormalParameters().toString()).collect(Collectors.joining(", ")), pattern, ctx);
        }

        ClassMethod bestMatch = bestScoreBranchMatches.get(0);
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
                    throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Duplicate method '" + name + "': it's already defined with the same parameter types in the same class", pattern, ctx);
                } else {
                    throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Method '" + name + "' is already defined in inherited class " + existing.getDefiningClass().getTypeIdentifier() + " with the same parameter types. Use the 'override' keyword to replace it", pattern, ctx);
                }
            }
            if(existing == null && mode == CustomClass.MemberParentMode.OVERRIDE) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override " + name + ": couldn't find existing overload with parameter types: " + method.getFormalParameters().toString(), pattern, ctx);
            }

            if(existing != null && mode == CustomClass.MemberParentMode.OVERRIDE && existing.getDefiningClass() == method.getDefiningClass()) {
                throw new TridentException(TridentException.Source.DUPLICATION_ERROR, "Cannot override " + name + ": a branch with the same parameter types is already defined in the same class: " + existing.getFormalParameters().toString(), pattern, ctx);
            }

            if(existing != null && existing.getModifiers().hasModifier(Symbol.SymbolModifier.FINAL)) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Cannot override function '" + name + "': it's defined as final in " + existing.getDefiningClass().getTypeIdentifier(), pattern, ctx);
            }
        }
        if(existing != null) overloads.remove(existing);
        overloads.add(method);

        //TODO check return type constraints of branches
        //TODO check visibility
    }

    public ClassMethod findOverloadForParameters(List<FormalParameter> types) {
        for(ClassMethod method : overloads) {
            if(FormalParameter.parameterListEquals(method.getFormalParameters(), types)) {
                return method;
            }
        }
        return null;
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
}

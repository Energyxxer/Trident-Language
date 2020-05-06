package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.CallStack;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TridentUserMethod implements TridentMethod {
    private List<TridentMethodBranch> branches;
    private ISymbolContext declaringContext;
    private Object thisObject;
    private String functionName = "<anonymous function>";

    public TridentUserMethod(TokenPattern<?> functionPattern, ISymbolContext declaringContext, Collection<FormalParameter> formalParameters, Object thisObject, String functionName, TypeConstraints returnConstraints) {
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;

        branches = Collections.singletonList(new TridentUserMethodBranch(formalParameters, functionPattern, returnConstraints));
    }
    public TridentUserMethod(String functionName, Collection<TridentMethodBranch> branches, ISymbolContext declaringContext, Object thisObject) {
        this.branches = new ArrayList<>(branches);
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        TridentMethodBranch branch = pickBranch(params, patterns, pattern, ctx);

        TokenPattern<?> functionPattern = branch.getFunctionPattern();

        ctx.getCompiler().getCallStack().push(new CallStack.Call(functionName, functionPattern, declaringContext.getStaticParentFile(), pattern));

        try {
            return branch.call(params, patterns, pattern, declaringContext, ctx, thisObject);
        } catch(StackOverflowError x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Stack Overflow Error", pattern, ctx);
        } finally {
            ctx.getCompiler().getCallStack().pop();
        }
    }

    private TridentMethodBranch pickBranch(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<TridentMethodBranch> bestScoreBranchMatches = new ArrayList<>();
        double bestScore = -1;
        boolean foundSameLengthMatch = false;

        //TridentMethodBranch bestPick = null;
        //boolean bestPickFullyMatched = false;

        for(TridentMethodBranch branch : branches) {
            List<FormalParameter> branchParams = branch.getFormalParameters();
            boolean branchMatched = true;
            double score = 0;
            for(int i = 0; i < branchParams.size() && branchMatched; i++) {
                FormalParameter formalParam = branchParams.get(i);
                Object actualParam = null;
                if(i < params.length) actualParam = params[i];
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
            boolean firstSameLengthMatch = !foundSameLengthMatch && branchParams.size() == params.length;
            if(branchMatched && score >= bestScore || firstSameLengthMatch) {
                if(score != bestScore || firstSameLengthMatch) bestScoreBranchMatches.clear();
                if(!foundSameLengthMatch || branchParams.size() == params.length) {
                    bestScore = score;
                    bestScoreBranchMatches.add(branch);
                    if(firstSameLengthMatch) foundSameLengthMatch = true;
                }
            }
        }
        if(bestScoreBranchMatches.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean any = false;
            for(Object obj : params) {
                sb.append(TridentTypeManager.getTypeIdentifierForObject(obj));
                sb.append(", ");
                any = true;
            }
            if(any) {
                sb.setLength(sb.length()-2);
            }
            StringBuilder overloads = new StringBuilder();
            for(TridentMethodBranch branch : branches) {
                overloads.append("\n    ").append(functionName).append("(");
                overloads.append(branch.getFormalParameters().toString().substring(1));
                overloads.setLength(overloads.length()-1);
                overloads.append(")");
            }
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Overload not found for parameter types: (" + sb.toString() + ")\nValid overloads are:" + overloads.toString(), pattern, ctx);
        }
        if(bestScoreBranchMatches.size() > 1) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Ambiguous call between: " + bestScoreBranchMatches.stream().map(b -> b.getFormalParameters().toString()).collect(Collectors.joining(", ")), pattern, ctx);
        }
        return bestScoreBranchMatches.get(0);
    }

    @Override
    public Object getMember(TridentMethod object, String member, TokenPattern<?> pattern, ISymbolContext file, boolean keepSymbol) {
        if(member.equals("formalParameters")) return branches.size() == 1 ? new ListObject(branches.get(0).getFormalParameters().stream().map(p -> p.getConstraints().getHandler()).collect(Collectors.toList())) : null;
        if(member.equals("declaringFile")) return declaringContext.getStaticParentFile().getResourceLocation();
        throw new MemberNotFoundException();
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setThisObject(Object thisObject) {
        this.thisObject = thisObject;
    }

    public List<TridentMethodBranch> getBranches() {
        return branches;
    }

    @Override
    public String toString() {
        return "<function(" + (branches.size() == 1 ? branches.get(0).getFormalParameters().stream().map(Object::toString).collect(Collectors.joining(", ")) : "?") + ")>";
    }
}

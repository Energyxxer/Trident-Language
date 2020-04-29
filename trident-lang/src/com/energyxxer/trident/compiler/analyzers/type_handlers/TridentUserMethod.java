package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TridentUserMethod implements TridentMethod {
    private List<TridentUserMethodBranch> branches;
    private ISymbolContext declaringContext;
    private Object thisObject;
    private String functionName = "<anonymous function>";

    public TridentUserMethod(TokenPattern<?> functionPattern, ISymbolContext declaringContext, Collection<FormalParameter> formalParameters, Object thisObject, String functionName, TypeConstraints returnConstraints) {
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;

        branches = Collections.singletonList(new TridentUserMethodBranch(formalParameters, functionPattern, returnConstraints));
    }
    public TridentUserMethod(Collection<TridentUserMethodBranch> branches, ISymbolContext declaringContext, Object thisObject, String functionName) {
        this.branches = new ArrayList<>(branches);
        this.declaringContext = declaringContext;
        this.thisObject = thisObject;
        if(functionName != null) this.functionName = functionName;
    }

    @Override
    public Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {

        TridentUserMethodBranch branch = pickBranch(params, patterns, pattern, ctx);

        TokenPattern<?> functionPattern = branch.getFunctionPattern();
        List<FormalParameter> formalParameters = branch.getFormalParameters();
        TypeConstraints returnConstraints = branch.getReturnConstraints();

        SymbolContext innerFrame = new SymbolContext(declaringContext);

        ctx.getCompiler().getCallStack().push(new CallStack.Call(functionName, functionPattern, declaringContext.getStaticParentFile(), pattern));

        for(int i = 0; i < formalParameters.size(); i++) {
            FormalParameter param = formalParameters.get(i);
            Symbol sym = new Symbol(param.getName(), Symbol.SymbolVisibility.PRIVATE);
            sym.setTypeConstraints(param.getConstraints());
            sym.safeSetValue(i < params.length ? params[i] : null, i < params.length ? patterns[i] : pattern, ctx);
            innerFrame.put(sym);
        }
        if(thisObject != null) innerFrame.put(new Symbol("this", Symbol.SymbolVisibility.PRIVATE, thisObject));

        try {
            TridentFile.resolveInnerFileIntoSection(functionPattern, innerFrame, ctx.getWritingFile().getFunction());
        } catch(StackOverflowError x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Stack Overflow Error", pattern, ctx);
        } catch(ReturnException x) {
            Object returnValue = x.getValue();
            if(returnConstraints != null) {
                returnConstraints.validate(returnValue, x.getPattern(), ctx);
                returnValue = returnConstraints.adjustValue(returnValue, pattern, ctx);
            }
            return returnValue;
        } finally {
            ctx.getCompiler().getCallStack().pop();
        }
        if(returnConstraints != null) {
            TokenPattern<?>[] innerFunctContent = ((TokenGroup)((TokenStructure)functionPattern).getContents()).getContents();
            TokenPattern<?> closingBracePattern = innerFunctContent[innerFunctContent.length-1];
            returnConstraints.validate(null, closingBracePattern, ctx);
        }
        return null;
    }

    private TridentUserMethodBranch pickBranch(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<TridentUserMethodBranch> bestScoreBranchMatches = new ArrayList<>();
        double bestScore = -1;
        boolean foundSameLengthMatch = false;

        //TridentUserMethodBranch bestPick = null;
        //boolean bestPickFullyMatched = false;

        for(TridentUserMethodBranch branch : branches) {
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
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Overload not found for parameter types: (" + sb.toString() + ")", pattern, ctx);
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

    @Override
    public String toString() {
        return "<function(" + (branches.size() == 1 ? branches.get(0).getFormalParameters().stream().map(Object::toString).collect(Collectors.joining(", ")) : "?") + ")>";
    }
}

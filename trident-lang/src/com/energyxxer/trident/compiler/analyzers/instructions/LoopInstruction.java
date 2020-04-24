package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.*;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class LoopInstruction implements Instruction {

    @AnalyzerMember(key = "for")
    public static class ForInstruction extends LoopInstruction implements Instruction {}
    @AnalyzerMember(key = "while")
    public static class WhileInstruction extends LoopInstruction implements Instruction {}

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolContext forFrame = new SymbolContext(ctx);

        LoopHeader header = parseHeader(pattern.find("LOOP_HEADER"), forFrame);
        if(header == null) return;
        TokenPattern<?> body = pattern.find("ANONYMOUS_INNER_FUNCTION");
        String label = getLabel(pattern);

        //ctx.getCompiler().getSymbolStack().push(forFrame);

        boolean wasEmpty = ctx.getCompiler().getTryStack().isEmpty();

        if(wasEmpty || ctx.getCompiler().getTryStack().isRecovering()) {
            ctx.getCompiler().getTryStack().pushRecovering();
        } else if(ctx.getCompiler().getTryStack().isBreaking()) {
            ctx.getCompiler().getTryStack().pushBreaking();
        }

        try {
            for (header.initialize(); header.condition(); header.iterate()) {
                try {
                    TridentFile.resolveInnerFileIntoSection(body, forFrame, ctx.getWritingFile().getFunction());
                } catch(BreakException x) {
                    if(x.getLabel() == null || x.getLabel().equals(label)) {
                        break;
                    } else throw x;
                } catch(ContinueException x) {
                    if (x.getLabel() != null && !x.getLabel().equals(label)) {
                        throw x;
                    }
                    // else continue; //pretend this is here
                }
            }
        } catch(TridentException | TridentException.Grouped x) {
            if(wasEmpty) {
                if(x instanceof TridentException) {
                    ((TridentException) x).expandToUncaught();
                    ctx.getCompiler().getReport().addNotice(((TridentException) x).getNotice());
                } else {
                    for(TridentException ex : ((TridentException.Grouped) x).getExceptions()) {
                        ctx.getCompiler().getReport().addNotice(ex.getNotice());
                    }
                }
            }
            else throw x;
        } finally {
            //ctx.getCompiler().getSymbolStack().pop();
            ctx.getCompiler().getTryStack().pop();
        }
    }

    public static String getLabel(TokenPattern<?> pattern) {
        TokenPattern<?> labelPattern = pattern.find("BLOCK_LABEL.LABEL");
        return labelPattern != null ? labelPattern.flatten(false) : null;
    }

    @SuppressWarnings("unchecked")
    private LoopHeader parseHeader(TokenPattern<?> pattern, ISymbolContext ctx) {
        while (true) {
            switch (pattern.getName()) {
                case "LOOP_HEADER":
                case "FOR_HEADER": {
                    pattern = ((TokenStructure) pattern).getContents();
                    continue;
                }
                case "CLASSICAL_FOR": {
                    TokenPattern<?> initialization = pattern.find("FOR_HEADER_INITIALIZATION.INTERPOLATION_VALUE");
                    TokenPattern<?> initializationVariableDecl = pattern.find("FOR_HEADER_INITIALIZATION.VARIABLE_DECLARATION");
                    TokenPattern<?> condition = pattern.find("FOR_HEADER_CONDITION.INTERPOLATION_VALUE");
                    TokenPattern<?> iteration = pattern.find("FOR_HEADER_ITERATION.INTERPOLATION_VALUE");
                    return new LoopHeader() {
                        @Override
                        public void initialize() {
                            if (initializationVariableDecl != null) {
                                new VariableInstruction().run(initializationVariableDecl, ctx);
                            } else {
                                InterpolationManager.parse(initialization, ctx);
                            }
                        }

                        @Override
                        public boolean condition() {
                            Object returnValue = InterpolationManager.parse(condition, ctx);
                            if (returnValue != null && returnValue.getClass() == Boolean.class)
                                return (boolean) returnValue;
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Required boolean in 'for' condition", condition, ctx);
                        }

                        @Override
                        public void iterate() {
                            InterpolationManager.parse(iteration, ctx);
                        }
                    };
                }
                case "ITERATOR_FOR": {
                    String varName = pattern.find("VARIABLE_NAME").flatten(false);
                    Object iterable = InterpolationManager.parse(pattern.find("INTERPOLATION_VALUE"), ctx);
                    TypeHandler handler = InterpolationManager.getHandlerForObject(iterable, pattern, ctx);
                    Iterator it;
                    if ((it = handler.getIterator(iterable)) != null) {
                        if (!it.hasNext()) return null;
                        TokenPattern<?> finalPattern = pattern;
                        return new LoopHeader() {
                            @Override
                            public void initialize() {
                                ctx.put(new Symbol(varName, Symbol.SymbolVisibility.GLOBAL, null));
                            }

                            @Override
                            public boolean condition() {
                                try {
                                    boolean hasNext = it.hasNext();
                                    if (hasNext) {
                                        ctx.search(varName, ctx).setValue(it.next());
                                    }
                                    return hasNext;
                                } catch (ConcurrentModificationException x) {
                                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Concurrent modification", finalPattern, ctx);
                                }
                            }

                            @Override
                            public void iterate() {
                            }
                        };
                    } else {
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Required iterable in 'for' iterator", pattern.find("INTERPOLATION_VALUE"), ctx);
                    }
                }
                case "WHILE_HEADER": {
                    TokenPattern<?> condition = pattern.find("INTERPOLATION_VALUE");
                    return new LoopHeader() {
                        @Override
                        public void initialize() {

                        }

                        @Override
                        public boolean condition() {
                            Object returnValue = InterpolationManager.parse(condition, ctx);
                            if (returnValue != null && returnValue.getClass() == Boolean.class)
                                return (boolean) returnValue;
                            throw new TridentException(TridentException.Source.TYPE_ERROR, "Required boolean in 'while' condition", condition, ctx);
                        }

                        @Override
                        public void iterate() {

                        }
                    };
                }
                default: {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
                }
            }
        }
    }

    private interface LoopHeader {
        void initialize();
        boolean condition();
        void iterate();
    }
}

package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.BreakException;
import com.energyxxer.trident.compiler.semantics.ContinueException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public abstract class LoopInstruction implements InstructionDefinition {

    public static class ForInstruction extends LoopInstruction {
        @Override
        public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
            TokenGroupMatch blockLabel = optional(TridentProductions.identifierX().setName("LABEL").setRecessive(), TridentProductions.colon()).setName("BLOCK_LABEL");

            TokenPatternMatch FOR_HEADER = choice(
                    group(TridentProductions.identifierX().setName("VARIABLE_NAME").addTags("cspn:Iterator Name"), TridentProductions.instructionKeyword("in", false), TridentProductions.noToken().addTags("cspn:Iterable"), productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("ITERATOR_FOR"),
                    group(
                            choice(productions.getOrCreateStructure("INTERPOLATION_VALUE"), group(productions.getOrCreateStructure("VARIABLE_DECLARATION"))).setOptional().setName("FOR_HEADER_INITIALIZATION").addTags("cspn:Initialization"),
                            TridentProductions.symbol(";"),
                            optional(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("FOR_HEADER_CONDITION").addTags("cspn:Loop Condition"),
                            TridentProductions.symbol(";"),
                            optional(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("FOR_HEADER_ITERATION").addTags("cspn:Iteration Expression")
                    ).setName("CLASSICAL_FOR")
            ).setName("LOOP_HEADER");

            return group(blockLabel, TridentProductions.instructionKeyword("for"), group(TridentProductions.brace("("), FOR_HEADER, TridentProductions.brace(")")).setName("LOOP_HEADER_WRAPPER").addProcessor((p, l) -> {
                if(l.getSummaryModule() != null) {
                    TokenPattern<?> iteratorName = p.find("LOOP_HEADER.VARIABLE_NAME");
                    if(iteratorName != null) {
                        productions.getProviderSet(ValueAccessExpressionSet.class).addPreBlockDeclaration(iteratorName);
                    }
                }
            }), wrapper(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("LOOP_BODY")).setName("FOR_STATEMENT");
        }
    }
    public static class WhileInstruction extends LoopInstruction {
        @Override
        public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
            TokenGroupMatch blockLabel = optional(TridentProductions.identifierX().setName("LABEL").setRecessive(), TridentProductions.colon()).setName("BLOCK_LABEL");

            TokenPatternMatch WHILE_HEADER = choice(
                    group(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("WHILE_HEADER").addTags("cspn:Loop Condition")
            ).setName("LOOP_HEADER");

            return group(
                    blockLabel,
                    TridentProductions.instructionKeyword("while"),
                    group(
                            TridentProductions.brace("("),
                            WHILE_HEADER,
                            TridentProductions.brace(")")
                    ).setName("LOOP_HEADER_WRAPPER"),
                    wrapper(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("LOOP_BODY")
            ).setName("WHILE_STATEMENT");
        }
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolContext forFrame = new SymbolContext(ctx);

        LoopHeader header = parseHeader(pattern.find("LOOP_HEADER_WRAPPER.LOOP_HEADER"), forFrame);
        if(header == null) return;
        TokenPattern<?> body = pattern.find("LOOP_BODY.ANONYMOUS_INNER_FUNCTION");
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
                    TridentFile.resolveInnerFileIntoSection(body, forFrame, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
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
        } catch(PrismarineException | PrismarineException.Grouped x) {
            if(wasEmpty) {
                if(x instanceof PrismarineException) {
                    ((PrismarineException) x).expandToUncaught();
                    ctx.getCompiler().getReport().addNotice(((PrismarineException) x).getNotice());
                } else {
                    for(PrismarineException ex : ((PrismarineException.Grouped) x).getExceptions()) {
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
                                initialization.evaluate(ctx);
                            }
                        }

                        @Override
                        public boolean condition() {
                            Object returnValue = condition.evaluate(ctx);
                            if (returnValue != null && returnValue.getClass() == Boolean.class)
                                return (boolean) returnValue;
                            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Required boolean in 'for' condition", condition, ctx);
                        }

                        @Override
                        public void iterate() {
                            iteration.evaluate(ctx);
                        }
                    };
                }
                case "ITERATOR_FOR": {
                    String varName = pattern.find("VARIABLE_NAME").flatten(false);
                    Object iterable = pattern.find("INTERPOLATION_VALUE").evaluate(ctx);
                    TypeHandler handler = ctx.getTypeSystem().getHandlerForObject(iterable, pattern, ctx);
                    Iterator it;
                    if ((it = handler.getIterator(iterable, pattern, ctx)) != null) {
                        if (!it.hasNext()) return null;
                        TokenPattern<?> finalPattern = pattern;
                        return new LoopHeader() {
                            @Override
                            public void initialize() {
                                ctx.put(new Symbol(varName, SymbolVisibility.GLOBAL, null));
                            }

                            @Override
                            public boolean condition() {
                                try {
                                    boolean hasNext = it.hasNext();
                                    if (hasNext) {
                                        ctx.search(varName, ctx, null).setValue(it.next());
                                    }
                                    return hasNext;
                                } catch (ConcurrentModificationException x) {
                                    throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Concurrent modification", finalPattern, ctx);
                                }
                            }

                            @Override
                            public void iterate() {
                            }
                        };
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Required iterable in 'for' iterator", pattern.find("INTERPOLATION_VALUE"), ctx);
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
                            Object returnValue = condition.evaluate(ctx);
                            if (returnValue != null && returnValue.getClass() == Boolean.class)
                                return (boolean) returnValue;
                            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Required boolean in 'while' condition", condition, ctx);
                        }

                        @Override
                        public void iterate() {

                        }
                    };
                }
                default: {
                    throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
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

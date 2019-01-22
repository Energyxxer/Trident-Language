package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.*;

import java.util.Iterator;

public class LoopInstruction implements Instruction {

    @ParserMember(key = "for")
    public static class ForInstruction extends LoopInstruction implements Instruction {}
    @ParserMember(key = "while")
    public static class WhileInstruction extends LoopInstruction implements Instruction {}

    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        LoopHeader header = parseHeader(pattern.find("LOOP_HEADER"), file);
        if(header == null) return;
        TokenPattern<?> body = pattern.find("ANONYMOUS_INNER_FUNCTION");

        SymbolTable forFrame = new SymbolTable(file);

        file.getCompiler().getSymbolStack().push(forFrame);

        boolean wasEmpty = file.getCompiler().getTryStack().isEmpty();

        if(wasEmpty || file.getCompiler().getTryStack().isRecovering()) {
            file.getCompiler().getTryStack().pushRecovering();
        } else if(file.getCompiler().getTryStack().isBreaking()) {
            file.getCompiler().getTryStack().pushBreaking();
        }

        try {
            for (header.initialize(); header.condition(); header.iterate()) {
                try {
                    TridentFile.resolveInnerFileIntoSection(body, file, file.getFunction());
                } catch(BreakException x) {
                    break;
                } catch(ContinueException x) {
                    // IntelliJ: 'continue' is unnecessary as the last statement in a loop
                    // Also IntelliJ: EmPtY 'cAtCh' BlOcK
                }
            }
        } catch(TridentException | TridentException.Grouped x) {
            if(wasEmpty) {
                if(x instanceof TridentException) {
                    file.getCompiler().getReport().addNotice(((TridentException) x).getNotice());
                } else {
                    for(TridentException ex : ((TridentException.Grouped) x).getExceptions()) {
                        file.getCompiler().getReport().addNotice(ex.getNotice());
                    }
                }
            }
            else throw x;
        } finally {
            file.getCompiler().getSymbolStack().pop();
            file.getCompiler().getTryStack().pop();
        }
    }

    private LoopHeader parseHeader(TokenPattern<?> pattern, TridentFile file) {
        switch(pattern.getName()) {
            case "LOOP_HEADER":
            case "FOR_HEADER": {
                return parseHeader(((TokenStructure) pattern).getContents(), file);
            }
            case "CLASSICAL_FOR": {
                TokenPattern<?> initialization = pattern.find("FOR_HEADER_INITIALIZATION.INTERPOLATION_VALUE");
                TokenPattern<?> condition = pattern.find("FOR_HEADER_CONDITION.INTERPOLATION_VALUE");
                TokenPattern<?> iteration = pattern.find("FOR_HEADER_ITERATION.INTERPOLATION_VALUE");
                return new LoopHeader() {
                    @Override
                    public void initialize() {
                        InterpolationManager.parse(initialization, file);
                    }

                    @Override
                    public boolean condition() {
                        Object returnValue = InterpolationManager.parse(condition, file);
                        if(returnValue != null && returnValue.getClass() == Boolean.class) return (boolean)returnValue;
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Required boolean in 'for' condition", condition, file);
                    }

                    @Override
                    public void iterate() {
                        InterpolationManager.parse(iteration, file);
                    }
                };
            }
            case "ITERATOR_FOR": {
                String varName = pattern.find("VARIABLE_NAME").flatten(false);
                Object iterable = InterpolationManager.parse(pattern.find("INTERPOLATION_VALUE"), file);
                if(iterable instanceof Iterable) {
                    Iterator it = ((Iterable) iterable).iterator();
                    if(!it.hasNext()) return null;
                    return new LoopHeader() {
                        @Override
                        public void initialize() {
                            file.getCompiler().getSymbolStack().peek().put(new Symbol(varName, Symbol.SymbolAccess.GLOBAL, null));
                        }

                        @Override
                        public boolean condition() {
                            boolean hasNext = it.hasNext();
                            if(hasNext) {
                                file.getCompiler().getSymbolStack().peek().get(varName).setValue(it.next());
                            }
                            return hasNext;
                        }

                        @Override
                        public void iterate() {
                        }
                    };
                } else {
                    throw new TridentException(TridentException.Source.TYPE_ERROR, "Required iterable in 'for' iterator", pattern.find("INTERPOLATION_VALUE"), file);
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
                        Object returnValue = InterpolationManager.parse(condition, file);
                        if(returnValue != null && returnValue.getClass() == Boolean.class) return (boolean)returnValue;
                        throw new TridentException(TridentException.Source.TYPE_ERROR, "Required boolean in 'while' condition", condition, file);
                    }

                    @Override
                    public void iterate() {

                    }
                };
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, file);
            }
        }
    }

    private interface LoopHeader {
        void initialize();
        boolean condition();
        void iterate();
    }
}

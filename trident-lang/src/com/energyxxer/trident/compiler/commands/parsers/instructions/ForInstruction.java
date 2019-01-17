package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

import java.util.Iterator;

@ParserMember(key = "for")
public class ForInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        ForHeader header = parseHeader(pattern.find("FOR_HEADER"), file);
        TokenPattern<?> body = pattern.find("ANONYMOUS_INNER_FUNCTION");

        SymbolTable forFrame = new SymbolTable(file);

        file.getCompiler().getStack().push(forFrame);

        try {
            for (header.initialize(); header.condition(); header.iterate()) {
                int errorsPre = file.getCompiler().getReport().getErrors().size();
                TridentFile.resolveInnerFileIntoSection(body, file, file.getFunction());
                if (file.getCompiler().getReport().getErrors().size() > errorsPre) {
                    //exit early to avoid multiple of the same error
                    throw new EntryParsingException();
                }
            }
        } catch(EntryParsingException x) {
            boolean a = true;
        } catch(Exception x) {
            boolean b = true;
            Debug.log("aaa");
        } finally {
            file.getCompiler().getStack().pop();
        }
    }

    private ForHeader parseHeader(TokenPattern<?> pattern, TridentFile file) {
        switch(pattern.getName()) {
            case "FOR_HEADER": {
                return parseHeader(((TokenStructure) pattern).getContents(), file);
            }
            case "CLASSICAL_FOR": {
                TokenPattern<?> initialization = pattern.find("FOR_HEADER_INITIALIZATION.INTERPOLATION_VALUE");
                TokenPattern<?> condition = pattern.find("FOR_HEADER_CONDITION.INTERPOLATION_VALUE");
                TokenPattern<?> iteration = pattern.find("FOR_HEADER_ITERATION.INTERPOLATION_VALUE");
                return new ForHeader() {
                    @Override
                    public void initialize() {
                        InterpolationManager.parse(initialization, file);
                    }

                    @Override
                    public boolean condition() {
                        Object returnValue = InterpolationManager.parse(condition, file);
                        if(returnValue != null && returnValue.getClass() == Boolean.class) return (boolean)returnValue;
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required boolean in 'for' condition", condition));
                        throw new EntryParsingException();
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
                    if(!it.hasNext()) throw new EntryParsingException();
                    return new ForHeader() {
                        @Override
                        public void initialize() {
                            file.getCompiler().getStack().peek().put(new Symbol(varName, Symbol.SymbolAccess.GLOBAL, null));
                        }

                        @Override
                        public boolean condition() {
                            boolean hasNext = it.hasNext();
                            if(hasNext) {
                                file.getCompiler().getStack().peek().get(varName).setValue(it.next());
                            }
                            return hasNext;
                        }

                        @Override
                        public void iterate() {
                        }
                    };
                } else {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Required iterable in 'for' iterator", pattern.find("INTERPOLATION_VALUE")));
                    throw new EntryParsingException();
                }
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                throw new EntryParsingException();
            }
        }
    }

    private interface ForHeader {
        void initialize();
        boolean condition();
        void iterate();
    }
}

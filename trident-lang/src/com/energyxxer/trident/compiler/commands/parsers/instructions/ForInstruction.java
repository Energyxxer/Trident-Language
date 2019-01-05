package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Iterator;
import java.util.List;

@ParserMember(key = "for")
public class ForInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        ForHeader header = parseHeader(pattern.find("FOR_HEADER"), file.getCompiler());
        TokenPattern<?> body = pattern.find("ANONYMOUS_INNER_FUNCTION");

        for(header.initialize(); header.condition(); header.iterate()) {
            int errorsPre = file.getCompiler().getReport().getErrors().size();
            TridentFile.resolveInnerFileIntoSection(body, file, file.getFunction());
            if(file.getCompiler().getReport().getErrors().size() > errorsPre) {
                //exit early to avoid multiple of the same error
                throw new EntryParsingException();
            }
        }
    }

    private ForHeader parseHeader(TokenPattern<?> pattern, TridentCompiler compiler) {
        switch(pattern.getName()) {
            case "FOR_HEADER": {
                return parseHeader(((TokenStructure) pattern).getContents(), compiler);
            }
            case "CLASSICAL_FOR": {
                List<TokenPattern<?>> parts = pattern.searchByName("INTERPOLATION_VALUE");
                TokenPattern<?> initialization = parts.get(0);
                TokenPattern<?> condition = parts.get(1);
                TokenPattern<?> iteration = parts.get(2);
                return new ForHeader() {
                    @Override
                    public void initialize() {
                        InterpolationManager.parse(initialization, compiler);
                    }

                    @Override
                    public boolean condition() {
                        Object returnValue = InterpolationManager.parse(condition, compiler);
                        if(returnValue.getClass() == Boolean.class) return (boolean)returnValue;
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Required boolean in 'for' condition", condition));
                        throw new EntryParsingException();
                    }

                    @Override
                    public void iterate() {
                        InterpolationManager.parse(iteration, compiler);
                    }
                };
            }
            case "ITERATOR_FOR": {
                String varName = pattern.find("VARIABLE_NAME").flatten(false);
                Object iterable = InterpolationManager.parse(pattern.find("INTERPOLATION_VALUE"), compiler);
                if(iterable instanceof Iterable) {
                    Iterator it = ((Iterable) iterable).iterator();
                    if(!it.hasNext()) throw new EntryParsingException();
                    return new ForHeader() {
                        @Override
                        public void initialize() {
                            compiler.getStack().peek().put(new Symbol(varName, Symbol.SymbolAccess.GLOBAL, it.next()));
                        }

                        @Override
                        public boolean condition() {
                            return it.hasNext();
                        }

                        @Override
                        public void iterate() {
                            compiler.getStack().peek().get(varName).setValue(it.next());
                        }
                    };
                } else {
                    compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Required iterable in 'for' iterator", pattern.find("INTERPOLATION_VALUE")));
                    throw new EntryParsingException();
                }
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
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
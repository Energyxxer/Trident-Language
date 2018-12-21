package com.energyxxer.trident.compiler.commands.parsers.compilation;

import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class CompilationBlock {
    final TokenPattern<?> pattern;
    final TridentCompiler compiler;
    final TridentFile file;
    final FunctionSection function;

    public CompilationBlock(TokenPattern<?> pattern, TridentFile file) {
        this.pattern = pattern;
        this.compiler = file.getCompiler();
        this.file = file;
        this.function = file.getFunction();
    }

    public void execute() {
        if(pattern == null) return;

        for(TokenPattern<?> group : ((TokenList)pattern).getContents()) {
            TokenPattern<?> rawStatement = ((TokenStructure)group.find("STATEMENT")).getContents();
            String key = rawStatement.getName().toLowerCase();

            CompileStatement parser = ParserManager.getParser(CompileStatement.class, key);
            if(parser != null) {
                parser.execute(rawStatement, this);
            }
        }
    }


}

package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.say.SayCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "say")
public class SayParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        StringBuilder sb = new StringBuilder();
        for(TokenPattern<?> part : ((TokenList) pattern.find("SAY_MESSAGE")).getContents()) {
            if(part.getName().equals("SAY_PART")) {
                TokenPattern<?> partInner = (TokenPattern<?>) part.getContents();
                if(partInner.getName().equals("SELECTOR")) {
                    sb.append(EntityParser.parseSelector(partInner, ctx));
                } else {
                    sb.append(partInner.flatten(false));
                }
            }
        }
        return new SayCommand(sb.toString());
    }
}
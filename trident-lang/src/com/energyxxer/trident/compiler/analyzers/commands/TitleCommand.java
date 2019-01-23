package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.title.TitleClearCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleResetCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleShowCommand;
import com.energyxxer.commodore.functionlogic.commands.title.TitleTimesCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "title")
public class TitleCommand implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file);
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "SHOW": {
                TextComponent text = TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), file);
                TitleShowCommand.Display display = TitleShowCommand.Display.valueOf(inner.find("DISPLAY").flatten(false).toUpperCase());

                return new TitleShowCommand(entity, display, text);
            }
            case "CLEAR_RESET": {
                return inner.find("LITERAL_CLEAR") != null ? new TitleClearCommand(entity) : new TitleResetCommand(entity);
            }
            case "TIMES": {
                int fadeIn = CommonParsers.parseInt(inner.find("FADEIN"), file);
                int stay = CommonParsers.parseInt(inner.find("STAY"), file);
                int fadeOut = CommonParsers.parseInt(inner.find("FADEOUT"), file);
                return new TitleTimesCommand(entity, fadeIn, stay, fadeOut);
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }
}

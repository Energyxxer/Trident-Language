package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

@ParserMember(key = "register")
public class RegisterInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "REGISTER_OBJECTIVE":
                registerObjective(inner, file);
                break;
            case "REGISTER_ENTITY":
                registerEntity(inner, file);
                break;
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
            }
        }
    }

    private void registerObjective(TokenPattern<?> pattern, TridentFile file) {
        String objectiveName = pattern.find("OBJECTIVE_NAME").flatten(false);
        String criteria = "dummy";
        TextComponent displayName = null;

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            criteria = sub.find("CRITERIA").flatten(false);
            TokenPattern<?> rawDisplayName = sub.find(".TEXT_COMPONENT");
            if(rawDisplayName != null) {
                displayName = TextParser.parseTextComponent(rawDisplayName, file.getCompiler());
            }
        }

        if(file.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) {
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "An objective with the name '" + objectiveName + "' has already been registered", pattern));
        } else {
            file.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
        }
    }

    private void registerEntity(TokenPattern<?> pattern, TridentFile file) {
        String entityName = pattern.find("ENTITY_NAME").flatten(false);
        Type defaultType = CommonParsers.parseEntityType(pattern.find("ENTITY_ID"), file.getCompiler());

        CustomEntity entityDecl = new CustomEntity(entityName, defaultType);

        var bodyEntries = (TokenList) pattern.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");

        if(bodyEntries != null) {
            for(var rawEntry : bodyEntries.getContents()) {
                var entry = ((TokenStructure) rawEntry).getContents();
                switch(entry.getName()) {
                    case "DEFAULT_NBT": {
                        entityDecl.setDefaultNBT(NBTParser.parseCompound(entry.find("NBT_COMPOUND")));
                        break;
                    }
                    default:
                        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + entry.getName() + "'", entry));
                }
            }
        }

        SymbolTable table = file.getCompiler().getStack().getGlobal();

        table.put(new Symbol(entityName, Symbol.SymbolAccess.GLOBAL, entityDecl));
    }
}

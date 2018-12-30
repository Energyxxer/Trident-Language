package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagCompoundTraverser;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

@ParserMember(key = "tdndebug")
public class DebugInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        TagCompound compound = NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), file.getCompiler());

        //PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, file.getCompiler().getModule().minecraft.types.entity.get("player"));

        //DataTypeQueryResponse response = file.getCompiler().getTypeMap().collectTypeInformation(compound, context);
        //Debug.log(response.getPossibleTypes());


        TagCompoundTraverser traverser = new TagCompoundTraverser(compound);
        TagCompoundTraverser.PathContents next = null;
        while((next = (traverser.next())) != null) {
            Debug.log(next);
        }
    }
}

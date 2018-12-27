package com.energyxxer.trident.compiler.commands.parsers.instructions;

import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.nbtmapper.tags.DataTypeQueryResponse;
import com.energyxxer.nbtmapper.tags.PathProtocol;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

@ParserMember(key = "tdndebug")
public class DebugInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        NBTPath path = NBTParser.parsePath(pattern.find("NBT_PATH"), file.getCompiler());

        PathContext context = new PathContext().setIsSetting(true).setProtocol(PathProtocol.ENTITY, file.getCompiler().getModule().minecraft.types.entity.get("player"));

        DataTypeQueryResponse response = file.getCompiler().getTypeMap().collectTypeInformation(path, context);
        Debug.log(response.getPossibleTypes());
    }
}

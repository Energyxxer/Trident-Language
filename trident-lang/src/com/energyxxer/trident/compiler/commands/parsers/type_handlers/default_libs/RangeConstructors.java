package com.energyxxer.trident.compiler.commands.parsers.type_handlers.default_libs;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

@ParserMember(key = "new.range")
public class RangeConstructors implements DefaultLibraryPopulator {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject newObj = ((DictionaryObject) stack.getGlobal().get("new").getValue());

        if(newObj == null) {
            throw new IllegalStateException("'new' object is null, somehow. " +
                    "Either Energy accidentally removed the new object initializer or you're" +
                    " seeing this from the source code in which case I really just couldn't stand " +
                    "the condescending 'newObj' may be null message from IntelliJ");
        }
    }
}

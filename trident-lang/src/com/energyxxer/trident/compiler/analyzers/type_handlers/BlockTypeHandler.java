package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.block.Blockstate;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Map;

@AnalyzerMember(key = "com.energyxxer.commodore.block.Block")
public class BlockTypeHandler implements VariableTypeHandler<Block> {
    @Override
    public Object getMember(Block object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("blockType")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockType", TridentUtil.ResourceLocation.class, () -> new TridentUtil.ResourceLocation(object.getBlockType().toString()), value -> {
                if(file.getCompiler().getModule().namespaceExists(value.namespace) && file.getCompiler().getModule().getNamespace(value.namespace).types.block.exists(value.body)) {
                    object.setBlockType(file.getCompiler().getModule().getNamespace(value.namespace).types.block.get(value.body));
                } else {
                    throw new TridentException(TridentException.Source.COMMAND_ERROR, value + " is not a valid block type", pattern, file);
                }
            });
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("blockTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("blockTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("blockState")) {
            AutoPropertySymbol<DictionaryObject> property = new AutoPropertySymbol<>("blockState", DictionaryObject.class, () -> {
                DictionaryObject dict = new DictionaryObject();
                if (object.getBlockstate() == null) return dict;

                for (Map.Entry<String, String> entry : object.getBlockstate().getProperties().entrySet()) {
                    dict.put(entry.getKey(), entry.getValue());
                }

                return dict;
            }, value -> {
                Blockstate newState = new Blockstate();
                for (Map.Entry<String, Symbol> a : value.entrySet()) {
                    newState.put(a.getKey(), InterpolationManager.cast(a.getValue(), String.class, pattern, file));
                }
                object.setBlockstate(newState);
            });
            return keepSymbol ? property : property.getValue();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Block object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(Block object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}

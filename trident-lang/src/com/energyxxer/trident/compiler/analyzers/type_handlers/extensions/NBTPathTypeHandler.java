package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.path.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.HashMap;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.path.NBTPath")
public class NBTPathTypeHandler implements VariableTypeHandler<NBTPath> {
    private static HashMap<String, MemberWrapper<NBTPath>> members = new HashMap<>();

    static {
        members.put("resolveKey", new MethodWrapper<NBTPath>("resolveKey", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathKey((String) params[0], (TagCompound) params[1]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, String.class, TagCompound.class).setNullable(1));
        members.put("resolveIndex", new MethodWrapper<>("resolveIndex", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTPathIndex((int) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, Integer.class));
        members.put("resolveListMatch", new MethodWrapper<>("resolveListMatch", (instance, params) -> {
            ArrayList<NBTPathNode> nodes = new ArrayList<>();
            for (NBTPath subPath : instance) {
                nodes.add(subPath.getNode());
            }
            nodes.add(new NBTListMatch((TagCompound) params[0]));
            return new NBTPath(nodes.toArray(new NBTPathNode[0]));
        }, TagCompound.class));
    }

    @Override
    public Object getMember(NBTPath path, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        MemberWrapper<NBTPath> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(path);
    }

    @Override
    public Object getIndexer(NBTPath object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(NBTPath object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}

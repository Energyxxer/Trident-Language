package com.energyxxer.trident.compiler.semantics.custom;

import com.energyxxer.commodore.functionlogic.nbt.ComplexNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathKey;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPathNode;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;

public interface TypeAwareMerger {
    int REPLACE = 0;
    int MERGE = 1;

    ThreadLocal<Stack<NBTPathNode>> path = new ThreadLocal<>();

    default TagCompound merge(@NotNull TagCompound thiz, @NotNull TagCompound other) {
        if(path.get() == null) path.set(new Stack<>());
        TagCompound merged = thiz.clone();
        for(NBTTag otherTag : other.getAllTags()) {
            if (merged.contains(otherTag.getName())) { //Collision
                NBTTag tag = merged.get(otherTag.getName());
                path.get().push(new NBTPathKey(otherTag.getName()));
                try {
                    if (otherTag.getClass() == tag.getClass() && otherTag instanceof ComplexNBTTag && handleCollision(new NBTPath(path.get().toArray(new NBTPathNode[0]))) == MERGE) {
                        merged.remove(otherTag.getName());
                        if (otherTag instanceof TagCompound) {
                            merged.add(merge(((TagCompound) tag), ((TagCompound) otherTag)));
                        } else if (otherTag instanceof TagList) {
                            merged.add(((TagList)tag).merge((TagList)otherTag));
                        }
                    } else {
                        merged.remove(otherTag.getName());
                        merged.add(otherTag.clone());
                    }
                } finally {
                    path.get().pop();
                }
            } else { //No collision
                merged.add(otherTag.clone());
            }
        }
        return merged;
    }

    int handleCollision(NBTPath path);
}

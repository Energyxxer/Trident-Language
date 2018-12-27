package com.energyxxer.nbtmapper.tags;

import com.energyxxer.commodore.CommandUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TypeFlags {
    private ArrayList<String> flags = new ArrayList<>();
    private ArrayList<String> typeCategories = new ArrayList<>();
    private ArrayList<String> stringOptions = new ArrayList<>();

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void putFlag(String flag) {
        flags.add(flag);
    }

    public void putTypeCategory(String category) {
        typeCategories.add(category);
    }

    public Collection<String> getFlags() {
        return flags;
    }

    public Collection<String> getTypeCategories() {
        return typeCategories;
    }

    public void putStringOption(String option) {
        stringOptions.add(option);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        Iterator<String> it = flags.iterator();
        while(it.hasNext()) {
            sb.append(it.next());
            if(it.hasNext()) sb.append(", ");
            else if(!typeCategories.isEmpty() || !stringOptions.isEmpty()) sb.append(' ');
        }
        if(!typeCategories.isEmpty()) {
            sb.append("type(");
            it = typeCategories.iterator();
            while(it.hasNext()) {
                sb.append(it.next());
                if(it.hasNext()) sb.append(", ");
            }
            sb.append(')');
        }
        if(!stringOptions.isEmpty()) {
            sb.append("one_of(");
            it = typeCategories.iterator();
            while(it.hasNext()) {
                sb.append(CommandUtils.escape(it.next()));
                if(it.hasNext()) sb.append(", ");
            }
            sb.append(')');
        }

        sb.append(")");
        return sb.toString();
    }
}
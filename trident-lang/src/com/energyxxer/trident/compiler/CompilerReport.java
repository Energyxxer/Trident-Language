package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.report.Notice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CompilerReport {
    private final ArrayList<Notice> errors = new ArrayList<>();
    private final ArrayList<Notice> warnings = new ArrayList<>();
    private final ArrayList<Notice> info = new ArrayList<>();
    private final ArrayList<Notice> debug = new ArrayList<>();

    public CompilerReport() {
    }

    public void addNotices(Collection<? extends Notice> notices) {
        notices.forEach(this::addNotice);
    }

    public void addNotice(Notice n) {
        switch(n.getType()) {
            case ERROR: {
                errors.add(n);
                break;
            }
            case WARNING: {
                warnings.add(n);
                break;
            }
            case INFO: {
                info.add(n);
                break;
            }
            case DEBUG: {
                debug.add(n);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported notice type " + n);
            }
        }
    }

    public ArrayList<Notice> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public ArrayList<Notice> getWarnings() {
        return warnings;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public ArrayList<Notice> getInfo() {
        return info;
    }

    public boolean hasInfo() {
        return !info.isEmpty();
    }

    public ArrayList<Notice> getDebug() {
        return debug;
    }

    public boolean hasDebug() {
        return !debug.isEmpty();
    }

    public HashMap<String, ArrayList<Notice>> groupByLabel() {
        HashMap<String, ArrayList<Notice>> map = new HashMap<>();

        for(Notice n : getInfo()) {
            if(!map.containsKey(n.getLabel())) map.put(n.getLabel(), new ArrayList<>());
            map.get(n.getLabel()).add(n);
        }
        for(Notice n : getWarnings()) {
            if(!map.containsKey(n.getLabel())) map.put(n.getLabel(), new ArrayList<>());
            map.get(n.getLabel()).add(n);
        }
        for(Notice n : getErrors()) {
            if(!map.containsKey(n.getLabel())) map.put(n.getLabel(), new ArrayList<>());
            map.get(n.getLabel()).add(n);
        }
        for(Notice n : getDebug()) {
            if(!map.containsKey(n.getLabel())) map.put(n.getLabel(), new ArrayList<>());
            map.get(n.getLabel()).add(n);
        }

        return map;
    }

    public String getTotalsString() {
        int errorCount = errors.size();
        int warningsCount = warnings.size();

        return "" + ((errorCount == 0) ? "no" : errorCount) + " error" + ((errorCount == 1) ? "" : "s") + " and " + ((warningsCount == 0) ? "no" : warningsCount) + " warning" + ((warningsCount == 1) ? "" : "s");
    }

    public int getTotal() {
        return info.size() + warnings.size() + errors.size() + debug.size();
    }

    public List<Notice> getAllNotices() {
        ArrayList<Notice> list = new ArrayList<>();
        list.addAll(errors);
        list.addAll(warnings);
        list.addAll(info);
        list.addAll(debug);
        return list;
    }

    public void clearNotices() {
        errors.clear();
        warnings.clear();
        info.clear();
        debug.clear();
    }
}

package com.energyxxer.trident.global;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.dialogs.ConfirmDialog;
import com.energyxxer.trident.util.ProjectUtil;
import com.energyxxer.util.FileUtil;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by User on 2/10/2017.
 */
public class FileManager {

    public static void delete(List<String> files) {
        if(files.size() <= 0) return;
        StringBuilder subject = new StringBuilder("");

        if(files.size() > 1) {

            HashMap<String, Integer> contents = new HashMap<>();
            contents.put("unit", 0);
            contents.put("path", 0);
            contents.put("model", 0);
            contents.put("sound index file", 0);
            contents.put("meta file", 0);
            contents.put("language file", 0);
            contents.put("texture", 0);
            contents.put("sound", 0);
            contents.put("file", 0);

            for (String path : files) {
                if (path.endsWith(".tdn")) {
                    contents.put("unit", contents.get("unit") + 1);
                } else if (path.endsWith(".json")) {
                    if (path.endsWith(File.separator + "sounds.json")) {
                        contents.put("sound index file", contents.get("sound index file") + 1);
                    } else {
                        contents.put("model", contents.get("model") + 1);
                    }
                } else if (path.endsWith(".mcmeta")) {
                    contents.put("meta file", contents.get("meta file") + 1);
                } else if (path.endsWith(".lang")) {
                    contents.put("language file", contents.get("language file") + 1);
                } else if (path.endsWith(".png")) {
                    contents.put("texture", contents.get("texture") + 1);
                } else if (path.endsWith(".ogg")) {
                    contents.put("sound", contents.get("sound") + 1);
                } else if (new File(path).isDirectory()) {
                    contents.put("folder", contents.get("folder") + 1);
                } else {
                    contents.put("file", contents.get("file") + 1);
                }
            }

            for (String key : contents.keySet()) {
                int count = contents.get(key);
                if (count == 0) continue;
                StringBuilder enumeration = new StringBuilder("");
                enumeration.append(count);
                enumeration.append(' ');
                enumeration.append(key);
                if (count != 1) enumeration.append('s');
                enumeration.append(", ");
                subject.append(enumeration.toString());
            }
            subject.setLength(subject.length() - 2);

            int andIndex = subject.lastIndexOf(",");
            if(andIndex >= 0) subject.replace(andIndex, andIndex + 1, " and");
        } else {
            File file = new File(files.get(0));
            if(file.isDirectory()) {
                subject.append('\'');
                String _package = ProjectUtil.getPackageInclusive(file);
                if(_package.equals("")) _package = file.getName();
                subject.append(_package);
            } else {
                if(file.getName().endsWith(".trident")) {
                    subject.append("unit ");
                    subject.append('\'');
                    subject.append(FileUtil.stripExtension(file.getName()));
                } else {
                    subject.append("file ");
                    subject.append('\'');
                    subject.append(file.getName());
                }
            }
            subject.append('\'');
        }

        String query = "Delete " + subject + "?";

        boolean confirmation = new ConfirmDialog("Delete", query).result;

        if(!confirmation) return;

        for(String path : files) {
            File file = new File(path);
            if(!file.exists()) continue;
            if(file.isDirectory()) {
                FileUtil.deleteFolder(file);
            } else {
                boolean success = file.delete();
                if(!success) Debug.log("Couldn't delete file '" + file.getName() + "'", Debug.MessageType.ERROR);
            }
        }
        TridentWindow.projectExplorer.refresh();
    }
}

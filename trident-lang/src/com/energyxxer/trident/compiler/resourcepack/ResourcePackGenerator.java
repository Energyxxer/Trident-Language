package com.energyxxer.trident.compiler.resourcepack;

import com.energyxxer.commodore.module.Exportable;
import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.energyxxer.commodore.module.ModulePackGenerator.OutputType.FOLDER;
import static com.energyxxer.commodore.module.ModulePackGenerator.OutputType.ZIP;

public class ResourcePackGenerator {
    @NotNull
    private final TridentCompiler compiler;

    @NotNull
    private final String rootPath;
    @NotNull
    private final File rootFile;

    private final Gson gson;

    private String description;

    @NotNull
    public ArrayList<Exportable> exportables;

    @NotNull
    private final ModulePackGenerator.OutputType outputType;

    private ZipOutputStream zipStream;

    public ResourcePackGenerator(@NotNull TridentCompiler compiler, @NotNull File outFile) {
        this(compiler, outFile, outFile.isFile() && outFile.getName().endsWith(".zip") ? ZIP : FOLDER);
    }

    private float progressDelta = 1;

    public ResourcePackGenerator(@NotNull TridentCompiler compiler, @NotNull File outFile, @NotNull ModulePackGenerator.OutputType outputType) {
        this.compiler = compiler;
        this.outputType = outputType;

        if(outputType == FOLDER && !outFile.exists()) {
            outFile.mkdirs();
        } else if(outputType == ZIP && !outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        this.rootPath = outFile.getAbsolutePath();
        this.rootFile = outFile;
        this.exportables = new ArrayList<>();
        this.description = "Resource Pack created with Trident";
        if(rootFile.isDirectory() && !rootFile.exists()) rootFile.mkdirs();
    }

    public void generate() throws IOException {
        if(outputType == ZIP) {
            zipStream = new ZipOutputStream(new FileOutputStream(rootFile));
        }

        progressDelta = exportables.isEmpty() ? 1 : 1f/(exportables.size());

        //createPackMcmeta();

        for(Exportable exportable : exportables) {
            if(exportable.shouldExport()) {
                createFile(exportable.getExportPath(), exportable.getContents());
            }
        }

        if(zipStream != null) zipStream.close();
    }

    private void createPackMcmeta() throws IOException {
        JsonObject root = new JsonObject();
        JsonObject inner = new JsonObject();
        root.add("pack", inner);
        inner.addProperty("pack_format", 1);

        inner.addProperty("description", description);

        createFile("pack.mcmeta", gson.toJson(root).getBytes());
    }

    private void createFile(@Nullable String path, @Nullable byte[] contents) throws IOException {
        if(path == null || contents == null) return;
        compiler.progress += progressDelta;
        compiler.setProgress("Generating resource pack: " + path);
        if(outputType == ZIP) {
            ZipEntry e = new ZipEntry(path);
            zipStream.putNextEntry(e);

            byte[] data = contents;
            zipStream.write(data, 0, data.length);
            zipStream.closeEntry();
        } else {
            File file = new File(rootPath + File.separator + path.replace("/", File.separator));
            file.getParentFile().mkdirs();
            file.createNewFile();

            try(FileOutputStream writer = new FileOutputStream(file)) {
                writer.write(contents);
                writer.flush();
            }
        }
    }

    @NotNull
    public ModulePackGenerator.OutputType getOutputType() {
        return outputType;
    }
}

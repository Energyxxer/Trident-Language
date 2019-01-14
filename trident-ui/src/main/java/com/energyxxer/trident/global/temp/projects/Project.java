package com.energyxxer.trident.global.temp.projects;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.util.Lazy;
import com.energyxxer.util.StringUtil;
import com.energyxxer.util.logger.Debug;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class Project {

	private File rootDirectory;

	private File datapackRoot;
	private File resourceRoot;
	private String name;
	
	public HashMap<String, String> icons = new HashMap<>();

	public final Lazy<CommandModule> module = new Lazy<>(() -> {
		try {
			return TridentCompiler.createModuleForProject(getName(), rootDirectory, StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT);
		} catch(IOException x) {
			Debug.log("Exception while creating module: " + x.toString(), Debug.MessageType.ERROR);
		}
		return Commons.getDefaultModule();
	});

	private final Lazy<TridentProductions> productions = new Lazy<>(() -> new TridentProductions(module.getValue()), TridentProductions::resetNestedStructures);

	//region a

	private JsonObject config;
	//endregion
	public Project(String name) {
		Path rootPath = Paths.get(ProjectManager.getWorkspaceDir()).resolve(name);
		this.rootDirectory = rootPath.toFile();

		datapackRoot = rootPath.resolve("datapack").toFile();
		resourceRoot = rootPath.resolve("resources").toFile();

		this.name = name;
		//this.prefix = StringUtil.getInitials(name).toLowerCase();

        Path outFolder = Paths.get(System.getProperty("user.home"), "Trident", "out");

        config = new JsonObject();
        config.addProperty("default-namespace", StringUtil.getInitials(name).toLowerCase());
        config.addProperty("language-level", 1);
        config.addProperty("datapack-output", outFolder.resolve(name).toString());
        config.addProperty("resources-output", outFolder.resolve(name + "-resources.zip").toString());
        config.addProperty("export-comments", true);
        config.addProperty("strict-text-components", false);
	}

	public Project(File rootDirectory) {
		this.rootDirectory = rootDirectory;
		File config = new File(rootDirectory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
		this.name = rootDirectory.getName();
		if(config.exists() && config.isFile() && config.getName().equals(TridentCompiler.PROJECT_FILE_NAME)) {
            try {
                this.config = new Gson().fromJson(new FileReader(config), JsonObject.class);
                return;
            } catch (FileNotFoundException x) {
                //I literally *just* checked if the file exists beforehand. Damn Java and its trust issues
                x.printStackTrace();
            }
		}
		this.rootDirectory = null;
		throw new RuntimeException("Invalid configuration file.");
	}

	public LazyTokenPatternMatch getFileStructure() {
		return productions.getValue().FILE;
	}

	public void rename(String name) throws IOException {
		File newFile = new File(ProjectManager.getWorkspaceDir() + File.separator + name);
		if(newFile.exists()) {
			throw new IOException("A project by that name already exists!");
		}
		this.name = name;
		updateConfig();
	}

	public boolean canFlatten(File file) {
		if(getRelativePath(file) == null) return true;
		if(!file.getParentFile().equals(this.getRootDirectory())) return true;
		return !Arrays.asList("src","resources","data").contains(file.getName());
	}
	
	public void updateConfig() {
		File config = new File(rootDirectory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
		PrintWriter writer;
		try {
			writer = new PrintWriter(config, "UTF-8");
			writer.print(new GsonBuilder().setPrettyPrinting().create().toJson(this.config));
			writer.close();
		} catch (IOException x) {
		    Debug.log(x.getMessage());
		}
	}
	
	private boolean exists() {
		return rootDirectory != null && rootDirectory.exists();
	}
	
	public void createNew() {
		if(!exists()) {
			this.datapackRoot.mkdirs();
			File config = new File(rootDirectory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
			try {
				config.createNewFile();
				updateConfig();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    public String getRelativePath(File file) {
		if(!file.getAbsolutePath().startsWith((rootDirectory.getAbsolutePath()+File.separator))) return null;
		return file.getAbsolutePath().substring((rootDirectory.getAbsolutePath()+File.separator).length());
	}
	
	public void setIconFor(File file, String value) {
		String path = getRelativePath(file);
		if(path != null) {
			icons.put(path.intern(), value);
			updateConfig();
		}
	}
	
	public String getIconFor(File file) {
		String path = getRelativePath(file);
		if(path != null) {
			return icons.get(path);
		}
		return null;
	}

    public File getRootDirectory() {
		return rootDirectory;
	}

    public File getDataPackRoot() {
        return datapackRoot;
    }

	public String getName() {
		return name;
	}

    @Deprecated
	public String getPrefix() {
		return "";
	}

    @Deprecated
	public String getWorld() {
		return "";
	}

	@Override
	public String toString() {
		return "Project [" + name + "]";
	}

	public void setName(String name) {
		this.name = name;
	}

    @Deprecated
	public void setPrefix(String prefix) {

	}

	@Deprecated
	public void setWorld(String world) {

	}
}

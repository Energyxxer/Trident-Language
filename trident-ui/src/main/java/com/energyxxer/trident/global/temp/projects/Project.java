package com.energyxxer.trident.global.temp.projects;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.util.StringUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class Project {

	private File directory;
	private File source;
	private String name = null;

	private String prefix = null;
	private String world = null;
	
	public HashMap<String, String> icons = new HashMap<>();
	
	public Project(String name) {
		this.directory = new File(ProjectManager.getWorkspaceDir() + File.separator + name);
		this.source = new File(ProjectManager.getWorkspaceDir() + File.separator + name + File.separator + "src");
		this.name = name;
		this.prefix = StringUtil.getInitials(name).toLowerCase();
		this.icons.put("src","src");
	}
	
	public Project(File directory) {
		this.directory = directory;
		this.source = new File(directory.getAbsolutePath() + File.separator + "src");
		File config = new File(directory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
		this.name = directory.getName();
		if(config.exists() && config.isFile() && config.getName().equals(TridentCompiler.PROJECT_FILE_NAME)) {

			return;
		}
		this.directory = null;
		this.source = null;
		throw new RuntimeException("Invalid configuration file.");
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
		if(!file.getParentFile().equals(this.getDirectory())) return true;
		return !Arrays.asList("src","resources","data").contains(file.getName());
	}
	
	public void updateConfig() {
		File config = new File(directory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
		PrintWriter writer;
		try {
			writer = new PrintWriter(config, "UTF-8");
			
			writer.print(getRawConfig());
			
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private boolean exists() {
		return directory != null && directory.exists();
	}
	
	public void createNew() {
		if(!exists()) {
			this.source.mkdirs();
			File config = new File(directory.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME);
			try {
				config.createNewFile();
				updateConfig();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createFromName(String name) {
		this.name = name;
		this.prefix = StringUtil.getInitials(name).toLowerCase();
	}
	
	private void fixIfCorrupted() {
		if((this.name == null || this.prefix == null) && this.directory != null) {
			createFromName(directory.getName());
			updateConfig();
		}
	}
	
	/*public void promptOutput() {
		String path = FileSelector.create("Select world", "<html>Specify the world directory to output to.<br>Structures will be saved in the structures folder.<br>The resource pack (if present) will be saved to this location, too.</html>", MinecraftConstants.getMinecraftDir() + File.separator + "saves" + File.separator, FileSelector.OPEN_DIRECTORY);
		if(path != null) {
			world = path;
			updateConfig();
		}
	}*/
	
	private String getRawConfig() {
		String s = "";
		s += "name=" + name + "\n";
		s += "prefix=" + prefix + "\n";
		if(world != null) 
			s += "out=" + world + "\n";
		{
			purgeIcons();
			String entry = "";
			Iterator<String> it = icons.keySet().iterator();
			while(it.hasNext()) {
				String key = it.next();
				String value = icons.get(key);
				String file = key + "?" + value;
				entry += file;
				if(it.hasNext()) entry += "|";
			}
			s += "icons=" + entry;
		}
		return s;
	}
	
	public String getRelativePath(File file) {
		if(!file.getAbsolutePath().startsWith((directory.getAbsolutePath()+File.separator))) return null;
		return file.getAbsolutePath().substring((directory.getAbsolutePath()+File.separator).length());
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

	public void purgeIcons() {
		ArrayList<String> pathsToRemove = new ArrayList<>();
		for(String path : icons.keySet()) {
			String absPath = directory.getPath() + File.separator + path;
			if(!new File(absPath).exists()) {
				pathsToRemove.add(path);
			}
		}

		for(String path : pathsToRemove) {
			icons.remove(path);
		}
	}

	public File getDirectory() {
		return directory;
	}

	public File getSource() {
		return source;
	}
	
	public String getName() {
		return name;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getWorld() {
		return world;
	}
	
	@Override
	public String toString() {
		return String.format("Project [name=%s, prefix=%s, world=%s]", name, prefix, world);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setWorld(String world) {
		this.world = world;
	}
}

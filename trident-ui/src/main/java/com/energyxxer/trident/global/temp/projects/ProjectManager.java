package com.energyxxer.trident.global.temp.projects;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.util.FileUtil;

import java.io.File;
import java.util.ArrayList;

public class ProjectManager {
	private static ArrayList<Project> loadedProjects = new ArrayList<>();
	private static String workspaceDir = null;

	public static void loadWorkspace() {
		if(workspaceDir == null) throw new IllegalStateException("Workspace directory not specified.");
		loadedProjects.clear();
		
		File workspace = new File(workspaceDir);

		File[] fileList = workspace.listFiles();
		if (fileList == null) {
			return;
		}

		//ArrayList<File> files = new ArrayList<>();
		for(File file : fileList) {
			if (file.isDirectory() && new File(file.getAbsolutePath() + File.separator + TridentCompiler.PROJECT_FILE_NAME).exists()) {
				//files.add(file);
				loadedProjects.add(new Project(new File(file.getAbsolutePath())));
			}
		}
	}
	
	public static Project getAssociatedProject(File file) {
		for(Project project : loadedProjects) {
			if((file.getPath() + File.separator).startsWith((project.getDirectory().getPath() + File.separator))) {
				return project;
			}
		}
		return null;
	}
	
	public static void setIconFor(File file, String value) {
		for(Project project : loadedProjects) {
			project.setIconFor(file, value);
		}
	}
	
	public static String getIconFor(File file) {
		Project project = getAssociatedProject(file);
		if(project != null) {
			String icon = project.getIconFor(file);
			if(icon != null) return icon;
		}
		String filename = file.getName();
		if(file.isFile()) {
			if(filename.endsWith(".json")) {
				if(filename.equals("sounds.json")) {
					return "sound_config";
				} else if(file.getParentFile().getName().equals("blockstates")) {
					return "blockstate";
				} else return "model";
			} else if(filename.endsWith(".lang")) {
				return "lang";
			} else if(filename.endsWith(".mcmeta") || filename.endsWith(TridentCompiler.PROJECT_FILE_NAME)) {
				return "meta";
			} else if(filename.endsWith(".ogg")) {
				return "audio";
			} else if(filename.endsWith(".nbt")) {
				return "structure";
			} else if(filename.endsWith(".mcfunction")) {
				return "function";
			} else if(filename.endsWith(".tdn")) {
				return "trident_file";
			}
			//TODO: Make this extension-to-icon mapping data-driven by the selected UI theme.

            /*
sounds.json = sound_config
blockstates/*.json = blockstate
*.json = model
*.lang = lang
*.mcmeta = meta
*.ogg = audio
*.nbt = structure
            */
		} else {
			//Check for file roots
			if(project != null) {
				if(file.getParentFile().equals(project.getDirectory())) {
					switch(filename) {
						case "src":
						case "resources":
						case "data":
							return filename;
					}
				}
			}
		}
		return null;
	}
	
	public static void create(String name) {
		Project p = new Project(name);
		p.createNew();
		loadedProjects.add(p);
	}
	
	public static boolean renameFile(File file, String newName) {
		String path = file.getAbsolutePath();
		String name = file.getName();
		String rawName = FileUtil.stripExtension(name);
		String extension = name.replaceAll(rawName, "");
		String pathToParent = path.substring(0, path.lastIndexOf(name));
		
		File newFile = new File(pathToParent + newName + extension);
		
		boolean renamed = file.renameTo(newFile);
		
		
		if(renamed) {
			for(Project project : loadedProjects) {

				String oldRPath = project.getRelativePath(new File(path));
				String newRPath = project.getRelativePath(newFile);
				
				if(oldRPath != null && project.icons.containsKey(oldRPath.intern())) {
					project.icons.put(newRPath.intern(), project.icons.get(oldRPath.intern()));
					project.icons.remove(oldRPath.intern());
					project.updateConfig();
				}
			}
		}
		
		return renamed;
	}

	public static String getWorkspaceDir() {
		return workspaceDir;
	}

	public static void setWorkspaceDir(String workspaceDir) {
		ProjectManager.workspaceDir = workspaceDir;
	}
}

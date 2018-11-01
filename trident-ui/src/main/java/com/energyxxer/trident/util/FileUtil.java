package com.energyxxer.trident.util;

import com.energyxxer.trident.global.temp.projects.Project;
import com.energyxxer.trident.global.temp.projects.ProjectManager;

import java.io.File;
import java.util.regex.Matcher;

/**
 * Provides utility managers for dealing with files.
 */
public class FileUtil {
	/**
	 * Deletes a folder and all its contents.
	 */
	public static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/**
	 * Returns whether or not the given string is a valid file name.
	 */
	public static boolean validateFilename(String str) {
		return str.indexOf("\\") + str.indexOf("/") + str.indexOf(":") + str.indexOf("*") + str.indexOf("?")
				+ str.indexOf("\"") + str.indexOf("<") + str.indexOf(">") + str.indexOf("|") == -9;
	}

	public static String getRelativePath(File file, File root) {
		String result = (file.getAbsolutePath() + File.separator).replaceFirst(Matcher.quoteReplacement(root.getAbsolutePath() + File.separator),"");
		if(result.endsWith(File.separator)) result = result.substring(0, result.length()-1);
		return result;
	}

	public static String stripExtension(String str) {

		if (str == null)
			return null;

		int pos = str.lastIndexOf(".");

		if (pos == -1)
			return str;

		return str.substring(0, pos);
	}

	public static String getPackage(File file) {
		return getPackageInclusive(file.getParentFile());
	}

	public static String getPackageInclusive(File file) {
		Project associatedProject = ProjectManager.getAssociatedProject(file);
		return (
				(associatedProject != null) ?
						stripExtension(
								getRelativePath(
										file,
										associatedProject.getSource()
								)
						) : "src"
		).replace(File.separator,".");
	}

	private FileUtil() {}
}

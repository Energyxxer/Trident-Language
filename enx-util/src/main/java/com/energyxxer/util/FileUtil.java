package com.energyxxer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

	public static Collection<File> listFilesOrdered(File directory) {
		if(!directory.isDirectory()) throw new IllegalArgumentException("Received non-directory file as parameter for FileUtil.listFilesOrdered(File)");
		ArrayList<File> subFilesOrdered = new ArrayList<>();
		int firstFileIndex = 0;
		File[] subFiles = directory.listFiles();
		if(subFiles != null) {
			for(File file : subFiles) {
				if(file.isDirectory()) {
					subFilesOrdered.add(firstFileIndex, file);
					firstFileIndex++;
				} else {
					subFilesOrdered.add(file);
				}
			}
		}
		return subFilesOrdered;
	}

	private FileUtil() {}
}

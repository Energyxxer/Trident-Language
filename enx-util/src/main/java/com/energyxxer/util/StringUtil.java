package com.energyxxer.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Things for strings.
 */
public class StringUtil {

	private static final char[] randomCharacters = new char[] {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','_','-'};

	public static final String TRUE = "true";
	public static final String FALSE = "false";
	
	public static String ellipsis(String str, int max) {
		if (str.length() > max) {
			return (str.substring(0, max - 3) + "...").intern();
		} else {
			return str;
		}
	}

	public static String substring(String str, int i1, int i2) {
		if (i1 < 0)
			i1 = 0;
		if (i2 < 0)
			i2 = 0;
		if (i1 > str.length())
			i1 = str.length();
		if (i2 > str.length())
			i2 = str.length();
		return str.substring(i1, i2);
	}

	public static String escapeHTML(String str) {
		return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	
	public static String escape(String str) {
		return str.replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t").replaceAll("\f", "\\\\f").replaceAll("\r", "\\\\r");
	}
	
	public static String addSlashes(String str) {
		return str.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	public static String stringFromBoolMap(HashMap<String, Object> m) {
		String s = "";
		Set<String> set = m.keySet();
		Iterator<String> setI = set.iterator();
		while(setI.hasNext()) {
			String key = setI.next();
			if(m.get(key) == Boolean.valueOf(true)) {
				s += key;
			}
		}
		return s.trim();
	}

	public static String repeat(String str, int amount) {
		StringBuilder o = new StringBuilder();
		for(int i = 0; i < amount; i++) {
			o.append(str);
		}
		return o.toString();
	}

	public static String getInitials(String s) {
		StringBuilder initials = new StringBuilder();

		char lastChar = 0;
		for(int i = 0; i < s.length(); i++) {
			if(i == 0) {
				initials.append(s.charAt(i));
			} else if(Character.isUpperCase(s.charAt(i))) {
				initials.append(s.charAt(i));
			} else if((lastChar == '_' || lastChar == '-') && Character.isAlphabetic(s.charAt(i))) {
				initials.append(s.charAt(i));
			}
			lastChar = s.charAt(i);
		}
		return initials.toString().toUpperCase();
	}

	public static char getRandomChar() {
		Random rand = new Random();

		return randomCharacters[rand.nextInt(randomCharacters.length)];
	}

	public static String getRandomString(int len) {
		String s = "";
		for (int i = 0; i < len; i++) {
			s += getRandomChar();
		}
		return s;
	}

	public static int getSequenceCount(String str, String pattern) {
		return getSequenceCount(str, pattern, 0);
	}

	public static int getSequenceCount(String str, String pattern, int start) {
		if(pattern.length() == 0) throw new IllegalArgumentException("Pattern length to count must not be empty");
		int count = 0;

		for(int i = start; i < str.length();) {
			if(str.substring(i).startsWith(pattern)) {
				count++;
				i += pattern.length();
			} else break;
		}

		return count;
	}

	public static String stripDecimals(double n) {
		if(n % 1 == 0) {
			return Integer.toString((int) n);
		} else {
			return Double.toString(n);
		}
	}
	
	private StringUtil() {}
}

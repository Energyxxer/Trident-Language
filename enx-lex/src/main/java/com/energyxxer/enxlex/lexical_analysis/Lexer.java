package com.energyxxer.enxlex.lexical_analysis;

import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.util.StringLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * For tokenizing any file by rules given by Lexer Profiles
 */
public class Lexer {
	
	private TokenStream stream;

	private ArrayList<Notice> notices = new ArrayList<>();
	
	/*public Lexer(File directory, TokenStream stream) {
		this.stream = stream;
		parse(directory);
	}*/

	public Lexer(TokenStream stream) {
		this.stream = stream;
	}

	public Lexer(File file, String str, TokenStream stream, ScannerProfile profile) {
		this.stream = stream;
		if(profile != null) tokenize(file, str, profile);
	}
	
	/*private void parse(File directory) {
		if (!directory.exists())
			return;
		File[] files = directory.listFiles();
		if(files == null) return;
		for (File file : files) {
			String name = file.getName();
			if (file.isDirectory()) {
				if(!file.getName().equals("resources") || !file.getParentFile().getParent().equals(ProjectManager.getWorkspaceDir())) {
					//This is not the resource pack directory.
					parse(file);
				}
			} else {
				Lang fileLang = Lang.getLangForFile(name);
				if(fileLang == null) continue;

				try {
					String str = new String(Files.readAllBytes(Paths.get(file.getPath())));
					tokenize(file, str, fileLang.createProfile());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}*/

	private File file;

	private StringBuilder token = new StringBuilder();
	private int line = 0;
	private int column = 0;
	private int index = 0;

	private TokenType tokenType = null;

	private int tokenLine = 0;
	private int tokenColumn = 0;
	private int tokenIndex = 0;

	private HashMap<TokenSection, String> subSections = null;

	public void tokenize(File file, String str, ScannerProfile profile) {
		this.file = file;
		stream.setProfile(profile);
		profile.setStream(stream);
		line = column = index = tokenLine = tokenColumn = tokenIndex = 0;
		token.setLength(0);

		{
			Token header = new Token("", TokenType.FILE_HEADER, file, new StringLocation(0, 0, 0));
			profile.putHeaderInfo(header);
			flush(header);
		}

		mainLoop: for(int i = 0; i <= str.length(); i++) {
			this.index = i;
			String c = "";

			boolean isClosingIteration = true;

			if(i < str.length()) {
				c = Character.toString(str.charAt(i));
				isClosingIteration = false;
			}

			String sub = str.substring(i);

			if (c.equals("\n")) {
				line++;
				column = -1;
			} else {
				column++;
			}

			for(ScannerContext ctx : profile.contexts) {
				if(ctx.getCondition() == ScannerContext.ContextCondition.LEADING_WHITESPACE && token.length() > 0) continue;
				if(ctx.getCondition() == ScannerContext.ContextCondition.LINE_START && column != 1) continue;
				ScannerContextResponse response = ctx.analyze(sub);
				if(response.errorMessage != null) {
					notices.add(new Notice(NoticeType.ERROR, response.errorMessage, "\b" + file.getAbsolutePath() + "\b" + (i + response.errorIndex) + "\b" + response.errorLength));
				}
				if(response.success) {
					flush();
					updateTokenPos();
					line += response.endLocation.line;
					column += response.endLocation.column;
					i += response.value.length()-1;
					token.append(response.value);
					tokenType = response.tokenType;
					subSections = response.subSections;
					flush();
					continue mainLoop;
				}
			}

			if(isClosingIteration) {
				flush();
				break;
			}

			if(Character.isWhitespace(c.charAt(0))) {
				//Is whitespace.
				flush();
				continue;
			} else if(token.length() == 0) {
				//Is start of a new token.
				updateTokenPos();
				tokenType = null;
			}

			char lastChar = '\u0000';

			if(i > 0) lastChar = str.charAt(i-1);

			if(lastChar != '\u0000' && !profile.canMerge(lastChar,c.charAt(0))) {
				flush();
				updateTokenPos();
			}
			token.append(c);
		}
		flush();

		updateTokenPos();
		token.setLength(0);
		tokenType = TokenType.END_OF_FILE;
		flush();
	}

	private void updateTokenPos() {
		tokenLine = line;
		tokenColumn = column;
		tokenIndex = index;
	}

	private void flush() {
		if(token.length() > 0 || (tokenType == TokenType.FILE_HEADER || tokenType == TokenType.END_OF_FILE))
			flush(new Token(token.toString(), tokenType, file, new StringLocation(tokenIndex, tokenLine, tokenColumn), subSections));

		token.setLength(0);
		tokenType = null;
		subSections = null;
	}
	
	private void flush(Token token) {
		stream.write(token);
	}

	public ArrayList<Notice> getNotices() {
		return notices;
	}

	public TokenStream getStream() {
		return stream;
	}
}

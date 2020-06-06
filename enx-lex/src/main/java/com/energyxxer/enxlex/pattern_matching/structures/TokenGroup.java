package com.energyxxer.enxlex.pattern_matching.structures;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TokenGroup extends TokenPattern<TokenPattern<?>[]> {
	private ArrayList<TokenPattern<?>> patterns = new ArrayList<TokenPattern<?>>();
	
	public TokenGroup() {}
	
	public TokenGroup(ArrayList<TokenPattern<?>> patterns) {
		this.patterns = patterns;
	}
	
	public void add(TokenPattern<?> pattern) {
		patterns.add(pattern);
	}

	@Override
	public TokenPattern<?>[] getContents() {
		return patterns.toArray(new TokenPattern<?>[0]);
	}
	
	@Override
	public TokenGroup setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String toString() {
		String o = ((name != null && name.length() > 0) ? name + ": " : "") + "{ ";
		
		for(TokenPattern<?> p : patterns) {
			o += p.toString();
		}
		o += " }";
		return o;
	}

	@Override
	public List<Token> search(TokenType type) {
		ArrayList<Token> list = new ArrayList<>();
		for(TokenPattern<?> p : patterns) {
			if(p.getContents() instanceof Token) {
				if(((Token) p.getContents()).type == type) list.add((Token) p.getContents());
			}
		}
		return list;
	}

	@Override
	public List<Token> deepSearch(TokenType type) {
		ArrayList<Token> list = new ArrayList<>();
		for(TokenPattern<?> p : patterns) {
			list.addAll(p.deepSearch(type));
		}
		return list;
	}

	@Override
	public List<TokenPattern<?>> searchByName(String name) {
		ArrayList<TokenPattern<?>> list = new ArrayList<>();
		for(TokenPattern<?> p : patterns) {
			if(p.name.equals(name)) list.add(p);
		}
		return list;
	}

	@Override
	public List<TokenPattern<?>> deepSearchByName(String name) {
		ArrayList<TokenPattern<?>> list = new ArrayList<>();
		for(TokenPattern<?> p : patterns) {
			if(p.name.equals(name)) list.add(p);
			list.addAll(p.deepSearchByName(name));
		}
		return list;
	}

	@Override
	public TokenPattern<?> find(String path) {
		String[] subPath = path.split("\\.",2);

		List<TokenPattern<?>> next = searchByName(subPath[0]);
		if(next.size() <= 0) return null;
		if(subPath.length == 1) return next.get(0);
		return next.get(0).find(subPath[1]);
	}

	@Override
	public String flatten(boolean separate) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < patterns.size(); i++) {
			String str = patterns.get(i).flatten(separate);
			sb.append(str);
			if(!str.isEmpty() && i < patterns.size()-1 && separate) sb.append(" ");
		}
		return sb.toString();
	}

	@Override
	public File getFile() {
		if(patterns == null) return null;
		for(TokenPattern pattern : patterns) {
			File file = pattern.getFile();
			if(file != null) return file;
		}
		return null;
	}

	@Override
	public StringLocation getStringLocation() {
		if (patterns == null || patterns.size() <= 0) return null;
		StringLocation l = null;
		for (TokenPattern<?> pattern : patterns) {
			StringLocation loc = pattern.getStringLocation();
			if(l == null) {
				l = loc;
				continue;
			}
			if(loc != null && loc.index < l.index) {
				l = loc;
			}
		}
		return l;
	}

	@Override
	public StringBounds getStringBounds() {
		if (patterns == null || patterns.size() <= 0) return null;
		StringLocation start = null;
		StringLocation end = null;

		//Find start
		for (TokenPattern<?> pattern : patterns) {
			StringLocation loc = pattern.getStringLocation();
			if(loc != null) {
				if (start == null) {
					start = loc;
					continue;
				}
				if (loc.index < start.index) {
					start = loc;
				}
			}
		}
		if(start == null) return null;

		//Find end
		for (TokenPattern<?> pattern : patterns) {
			StringBounds bounds = pattern.getStringBounds();
			if(bounds != null) {
				if (end == null) {
					end = bounds.end;
					continue;
				}
				if (bounds.end.index > end.index) {
					end = bounds.end;
				}
			}
		}

		if(end == null) return null;
		return new StringBounds(start, end);
	}

	@Override
	public ArrayList<Token> flattenTokens() {
		ArrayList<Token> list = new ArrayList<>();
		for(TokenPattern<?> pattern : patterns) {
		    list.addAll(pattern.flattenTokens());
        }
        return list;
	}

	@Override
	public String getType() {
		return "GROUP";
	}

	@Override
	public TokenGroup addTags(List<String> newTags) {
		super.addTags(newTags);
		return this;
	}

	@Override
	public void validate() {
		if(this.name != null && this.name.length() > 0) this.tags.add(name);
		patterns.forEach(p -> {
			for(String tag : this.tags) {
				if(!tag.startsWith("__")) p.addTag(tag);
			}
			p.validate();
		});
	}
}

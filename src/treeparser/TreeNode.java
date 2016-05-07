package treeparser;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import treeparser.io.IOSource;
import treeparser.query.QueryNode;
import treeparser.query.ResultNode;

public class TreeNode implements Serializable {

	public TreeNode parent = null;
	public IOSource source = null;
	public int line = -1;
	public int exitLine = -1;
	public int start = -1;
	public int enter = -1;
	public int exit = -1;
	public int end = -1;
	public ArrayList<TreeNode> children = new ArrayList<TreeNode>();

	public TreeNode() {
		// TODO Auto-generated constructor stub
	}
	
	public TreeNode(String source) {
		TreeNode node = TreeParser.parse(source);
		if (node != null) {
			add(node.children);
		}
	}

	public TreeNode(IOSource source, int start) {
		this.source = source;
		this.start = start;
	}

	public TreeNode(IOSource source, int start, int end, int line) {
		this.source = source;
		this.start = start;
		this.end = end;
		this.line = line;
	}

	public TreeNode(IOSource source, int start, int enter, int exit, int end, int line, int exitLine) {
		this.source = source;
		this.start = start;
		this.enter = enter;
		this.exit = exit;
		this.end = end;
		this.line = line;
		this.exitLine = exitLine;
	}
	
	public void set(TreeNode other) {
		this.source = other.source;
		this.start = other.start;
		this.enter = other.enter;
		this.exit = other.exit;
		this.end = other.end;
		this.line = other.line;
		this.exitLine = other.exitLine;
	}
	
	public void addOffset(int insertPoint, int lines) {
		start += insertPoint;
		
		if (enter != -1) {
			enter += insertPoint;
		}
		
		if (exit != -1) {
			exit += insertPoint;
		}
		
		end += insertPoint;
		line += lines;
		
		if (exitLine != -1) {
			exitLine += lines;
		}
	}
	
	public String getLeadingWhitespace() {
		
		int i = start - 1;
		for (; i >= 0 ; i --) {
			byte c = source.buffer.get(i);
			if (!(c == ' '
					|| c == '\r'
					|| c == '\t'
					)) {
				i ++;
				break;
			}
		}
		if (i >= 0) {
			
			int length = start - i;
			byte[] data = new byte[length];
			source.buffer.position(i);
			source.buffer.get(data, 0, length);
			
			return new String(data);
		}
		
		return "";
	}
	
	public String getFollowingWhitespace() {
		
		int i = end + 1;
		int limit = source.buffer.limit();
		for (; i < limit ; i ++) {
			byte c = source.buffer.get(i);
			if (!(c == ' '
					|| c == '\r'
					|| c == '\t'
					)) {
				i --;
				break;
			}
		}
		if (i < limit) {
			
			int length = i - end;
			byte[] data = new byte[length];
			source.buffer.position(i);
			source.buffer.get(data, 0, length);
			
			return new String(data);
		}
		
		return "";
	}
	
	public String getLineIndent() {
		
		TreeNode last = this;
		for (TreeNode node = last ; node != null && node.line == last.line ; node = node.getPreviousSibling()) {
			last = node;
		}
		
		return last.getLeadingWhitespace();
	}
	
	public String getIndentOfParent() {
		
		if (!hasParent()) {
			return "";
		}
		
		return parent.getLineIndent();
	}
	
	public String getLabel() {
		int length = (end - start) + 1;
		if (start + length > source.buffer.limit()) {
			return null;
		}
		byte[] data = new byte[length];
		source.buffer.position(start);
		source.buffer.get(data, 0, length);
		return new String(data);
	}
	
	public String getEnterLabel() {
		int length = (enter - start) + 1;
		if (start + length > source.buffer.limit()) {
			return null;
		}
		byte[] data = new byte[length];
		source.buffer.position(start);
		source.buffer.get(data, 0, length);
		return new String(data);
	}
	
	public String getExitLabel() {
		int length = (end - exit) + 1;
		if (exit + length > source.buffer.limit()) {
			return null;
		}
		byte[] data = new byte[length];
		source.buffer.position(exit);
		source.buffer.get(data, 0, length);
		return new String(data);
	}
	
	public boolean hasParent() {
		return parent != null;
	}
	
	public boolean isEmpty() {
		return start == -1;
	}
	
	public boolean isEnclosing() {
		return enter != -1;
	}
	
	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	public void add(ArrayList<TreeNode> childNodes) {
		for (TreeNode childNode : childNodes) {
			children.add(childNode);
			childNode.parent = this;
		}
	}
	
	public void add(TreeNode childNode) {
		children.add(childNode);
		childNode.parent = this;
	}
	
	public void addAll(ArrayList<ResultNode> children) {
		for (ResultNode child : children) {
			add(child);
		}
	}

	public TreeNode cloneWithoutLinks() {
		return new TreeNode(source, start, enter, exit, end, line, exitLine);
	}
	
	public int getIndex() {
		
		if (!hasParent() || !parent.hasChildren()) {
			return -1;
		}
		
		return parent.children.indexOf(this);
	}
	
	public TreeNode getSibling(int index) {
		
		if (index < 0) {
			return null;
		}
		
		if (!hasParent() || !parent.hasChildren()) {
			return null;
		}
		
		if (index >= parent.children.size()) {
			return null;
		}
		
		return parent.children.get(index);
	}
	
	public TreeNode getPreviousSibling() {
		
		return getSibling(getIndex() - 1);
	}
	
	public TreeNode getNextSibling() {
		
		return getSibling(getIndex() + 1);
	}
	
	public TreeNode getFirstChild() {
		
		if (!hasChildren()) {
			
			return null;
		}
		
		return children.get(0);
	}
	
	public TreeNode getLastChild() {
		
		if (!hasChildren()) {
			
			return null;
		}
		
		return children.get(children.size() - 1);
	}
	
	public ResultNode query(QueryNode query) {
		return query.queryNode(this);
	}
	
	public Collection<TreeNode> getPostModifiersQueryMatch(QueryNode query) {
		
		if (query.isEmpty() == this.isEmpty()) {
			
			if (query.axesString.equals(".*")) {
				
				return Arrays.asList(this);
			}
			
			if (query.isEnclosing() == this.isEnclosing()) {
				
				if (this.isEnclosing()) {
					
					String enter = query.getEnterLabel();
					String exit = query.getExitLabel();
					
					int index = enter.lastIndexOf("::");
					
					if (index != -1) {
						enter = enter.substring(index + "::".length());
					}
					
					Matcher enterMatcher = Pattern.compile(
							enter.replaceAll("([\\{\\(\\[])", "\\\\$1").replaceAll("[\\\\]([\\\\]+)", "$1").replaceAll("[\\\\]{2}", "\\\\")
							).matcher(this.getEnterLabel());
					
					Matcher exitMatcher = Pattern.compile(
							exit.replaceAll("([\\}\\)\\]])", "\\\\$1").replaceAll("[\\\\]([\\\\]+)", "$1").replaceAll("[\\\\]{2}", "\\\\")
							).matcher(this.getExitLabel());
					
					if (enterMatcher.find() && exitMatcher.find()) {
						
						// TODO: finish this
						if (query.hasSteps()) {
							
							return query.query(this.children);
							
						} else {
							
							return Arrays.asList(this);
						}
					}
					
				} else {
					
					String pattern = query.getLabel();
					
					int index = pattern.lastIndexOf("::");
					
					if (index != -1) {
						pattern = pattern.substring(index + "::".length());
					}
					
					Matcher matcher = Pattern.compile(
							pattern.replaceAll("[\\\\]([\\\\]*)", "$1").replaceAll("[\\\\][\\\\]", "\\\\")
							).matcher(this.getLabel());
					
					if (matcher.find()) {
						
						return Arrays.asList(this);
					}
				}
			}
		}
		
		return null;
	}
	
	public Collection<TreeNode> getMatch(QueryNode query) {
		
		if (query.isEmpty() == this.isEmpty()) {
			
			if (this.isEnclosing() == this.isEnclosing()) {
				
				if (query.hasSteps() && this.hasChildren()) {
					
					Collection<TreeNode> result = query.query(this.children);
					
					return result;
					
				} else if (!this.hasChildren()) {
					
					String pattern = null;
					int index = query.axesString.lastIndexOf("::");
					
					if (index != -1) {
						pattern = query.axesString.substring(index + "::".length());
					} else {
						pattern = query.axesString;
					}
					
					String text = this.getLabel();
					Matcher matcher = Pattern.compile(pattern).matcher(text);
					
					if (matcher.find()) {
						
						return children;
					}
				}
			}
		}
		
		return null;
	}
	
	public boolean matchesPattern(String pattern) {
		String text = null;
		if (this.start == -1) {
			System.err.printf("[ERROR] null node in comparison at line: %d\n", this.line);
			return false;
		} else if (this.enter == -1) {
			text = this.getLabel();
		} else {
			text = this.getEnterLabel() + this.getExitLabel();
		}
		Matcher matcher = Pattern.compile(pattern).matcher(text);
		return matcher.find();
	}

	public boolean matches(QueryNode query) {
		String pattern = null;
		if (query.start == -1) {
			return true;
		} else if (query.enter == -1) {
			pattern = query.getLabel();
		} else {
			pattern = query.getEnterLabel();
			pattern = pattern.replaceAll("\\\\(.)", "$1");
			pattern = pattern.replaceAll("(?<!\\\\)([\\(\\)\\{\\}\\[\\]])", "\\\\$1");
			if (this.start == -1) {
				System.err.printf("[ERROR] null node in comparison at line: %d\n", this.line);
				return false;
			} else if (this.enter == -1) {
				return false;
			} else {
				String rootStartString = this.getLabel();
				Matcher matcher = Pattern.compile(pattern).matcher(rootStartString);
				if (matcher.find()) {
					pattern = query.getExitLabel();
					pattern = pattern.replaceAll("\\\\(.)", "$1");
					pattern = pattern.replaceAll("(?<!\\\\)([\\(\\)\\{\\}\\[\\]])", "\\\\$1");
					String rootEndString = this.getExitLabel();
					matcher = Pattern.compile(pattern).matcher(rootEndString);
					if (matcher.find()) {
						return true;
					}
				}
			}
		}
		return this.matchesPattern(pattern);
	}

	public ArrayList<ResultNode> getMatchingChildren(QueryNode query) {
		ArrayList<ResultNode> matches = new ArrayList<ResultNode>();
		if (this.hasChildren()) {
			for (TreeNode child : children) {
				ResultNode result = child.query(query);
				if (result != null) {
					matches.add(result);
					matches.addAll(result.siblings);
				}
			}
		}
		return matches;
	}

	public Collection<TreeNode> getChildrenMatchingPattern(String pattern) {
		ArrayList<TreeNode> matches = new ArrayList<TreeNode>();
		if (this.hasChildren()) {
			for (TreeNode child : children) {
//				System.out.println(child.toString() + " = " + pattern);
//				if (child.getLabel().contains("if")) {
//					System.out.print("");
//				}
				if (child.matchesPattern(pattern)) {
					matches.add(child);
				}
			}
		}
		return matches;
	}

	public Collection<TreeNode> getParentMatchingPattern(String pattern) {
		ArrayList<TreeNode> matches = new ArrayList<TreeNode>();
		if (this.hasParent()) {
			matches.add(parent);
		}
		return matches;
	}

	@Override
	public String toString() {
		return treeNodeToString(new AtomicInteger(-1), new AtomicReference<IOSource>(null));
	}
	
	public String treeNodeToString(AtomicInteger line, AtomicReference<IOSource> currentSource) {
		
		StringBuilder stringBuilder = new StringBuilder();
		
		if (start != -1) {
			
			if (this.source != currentSource.get()) {
				
				stringBuilder.append("\n");
				currentSource.set(this.source);
				line.set(this.line);
			}
			
			if (this.line != line.get()) {
				if (line.get() != -1) {
					if (line.get() > -1) {
						
						TreeNode sibling = this.getPreviousSibling();
						if (sibling != null
								&& !( sibling.line == line.get()
								|| sibling.exitLine == line.get() )) {
							
							stringBuilder.append("\n");
							
							if (sibling.isEnclosing()) {
								
								line.set(sibling.exitLine);
								
							} else {
								
								line.set(this.line);
							}
						}
						
						while (line.get() < this.line) {
							
							line.getAndIncrement();
							stringBuilder.append("\n");
						}
					}
				}
				line.set(this.line);
			}
			
			stringBuilder.append(this.getLeadingWhitespace());
			
			if (this.isEnclosing()) {
				stringBuilder.append(getEnterLabel().replaceAll("\n", ""));
			} else if (!this.isEmpty()) {
				String label = getLabel();
				if (label == null) {
					System.out.println(label);
					for (int i = start ; i < end ; i ++) {
						source.buffer.position(i);
						byte b = source.buffer.get();
						System.out.printf("%c", (char) b);
					}
					label = getLabel();
				}
				stringBuilder.append(label.replaceAll("\n", ""));
			}
		}
		
		if (line.get() == -1) {
			line.set(this.line);
		}
		
		if (children.size() > 0) {
			for (TreeNode child : children) {
				stringBuilder.append(child.treeNodeToString(line, currentSource));
			}
		}
		
		if (exit != -1) {
			if (this.exitLine != -1 && this.exitLine != line.get()) {
				if (line.get() > 0) {
					while (line.get() < this.exitLine) {
						line.getAndIncrement();
						stringBuilder.append("\n");
					}
				}
				line.set(this.exitLine);
			}
			stringBuilder.append(this.getFollowingWhitespace());
			stringBuilder.append(getExitLabel().replaceAll("\n", ""));
		}
		
		return stringBuilder.toString();
	}
}

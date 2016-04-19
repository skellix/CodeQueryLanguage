package treeparser.query;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.TreeNode;

public class ResultNode extends TreeNode {
	public TreeNode thisNode = null;
	public ArrayList<ResultNode> children = new ArrayList<ResultNode>();
	public ArrayList<ResultNode> siblings = new ArrayList<ResultNode>();
	public ArrayList<ResultNode> returnSet = new ArrayList<ResultNode>();
	
	public ResultNode(TreeNode thisNode) {
		set(thisNode);
	}
	
	public ResultNode(TreeNode thisNode, ArrayList<ResultNode> children) {
		this(thisNode);
		this.children = children;
	}
	
	public void set(TreeNode thisNode) {
		this.thisNode = thisNode;
		if (thisNode != null) {
			this.source = thisNode.source;
			this.start = thisNode.start;
			this.enter = thisNode.enter;
			this.exit = thisNode.exit;
			this.end = thisNode.end;
			this.line = thisNode.line;
		}
	}
	
	public ResultNode getSibling(int index) {
		
		if (index < 0) {
			return null;
		}
		
		if (!hasParent() || !parent.hasChildren()) {
			return null;
		}
		
		if (index >= parent.children.size()) {
			return null;
		}
		
		return (ResultNode) parent.children.get(index);
	}
	
	public ResultNode getPreviousSibling() {
		
		return getSibling(getIndex() - 1);
	}
	
	public ResultNode getNextSibling() {
		
		return getSibling(getIndex() + 1);
	}
	
	public ResultNode getFirstChild() {
		
		if (!hasChildren()) {
			
			return null;
		}
		
		return children.get(0);
	}
	
	public ResultNode getLastChild() {
		
		if (!hasChildren()) {
			
			return null;
		}
		
		return children.get(children.size() - 1);
	}
	
	public TreeNode toTreeNode() {
		
		if (children.size() > 1) {
			
			TreeNode out = new TreeNode(this.source, this.start, this.enter, this.exit, this.end, this.line, this.exitLine);
			
			
			for (ResultNode child : this.children) {
				out.add(child.toTreeNode());
			}
			
			return out;
			
		} else if (thisNode == null) {
			
			TreeNode out = new TreeNode(this.source, this.start, this.enter, this.exit, this.end, this.line, this.exitLine);
			
			
			for (ResultNode child : this.children) {
				out.add(child.toTreeNode());
			}
			
			return out;
			
		} else {
			
			return thisNode;
		}
	}
	
	@Override
	public String toString() {
		return toString(new AtomicInteger(0));
	}
	
	protected String toString(AtomicInteger line) {
		if (children.size() > 1) {
			StringBuilder stringBuilder = new StringBuilder();
			if (start != -1) {
				if (thisNode.line != line.get()) {
					if (line.get() > 0) {
						while (line.get() < thisNode.line) {
							line.getAndIncrement();
							stringBuilder.append("\n");
						}
					}
					line.set(thisNode.line);
				}
				int i = start - 1;
				for (; i >= 0 ; i --) {
					if (!(source.buffer.get(i) == ' '
							|| source.buffer.get(i) == '\r'
							//|| source.get(i) == '\n'
							|| source.buffer.get(i) == '\t'
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
					stringBuilder.append(new String(data).replaceAll("\n", ""));
				}
				if (enter != -1) {
					stringBuilder.append(getEnterLabel().replaceAll("\n", ""));
				} else if (end != -1) {
					stringBuilder.append(getLabel().replaceAll("\n", ""));
				}
			}
			if (children.size() > 0) {
				for (ResultNode child : children) {
					stringBuilder.append(child.toString(line));
				}
			}
			if (exit != -1) {
				if (thisNode.exitLine != -1 && thisNode.exitLine != line.get()) {
					if (line.get() > 0) {
						while (line.get() < thisNode.exitLine) {
							line.getAndIncrement();
							stringBuilder.append("\n");
						}
					}
					line.set(thisNode.exitLine);
				}
				int i = exit - 1;
				for (; i >= 0 ; i --) {
					if (!(source.buffer.get(i) == ' '
							|| source.buffer.get(i) == '\r'
							//|| source.get(i) == '\n'
							|| source.buffer.get(i) == '\t'
							)) {
						i ++;
						break;
					}
				}
				int length = exit - i;
				byte[] data = new byte[length];
				source.buffer.position(i);
				source.buffer.get(data, 0, length);
				stringBuilder.append(new String(data).replaceAll("\n", ""));
				stringBuilder.append(getExitLabel().replaceAll("\n", ""));
			}
			return stringBuilder.toString();
		} else if (thisNode == null) {
			StringBuilder stringBuilder = new StringBuilder();
			if (children.size() > 0) {
				for (ResultNode child : children) {
					stringBuilder.append(child.toString(line));
				}
			}
			return stringBuilder.toString();
		} else {
			return thisNode.treeNodeToString(line);
		}
	}
}

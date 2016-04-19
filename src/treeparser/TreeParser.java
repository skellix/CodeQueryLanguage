package treeparser;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.io.IOSource;

public class TreeParser {

	public static TreeNode parse(String input) {
		
		TreeNode root = new TreeNode();
		IOSource source = new IOSource(MappedByteBuffer.wrap(input.getBytes()));
		doParse(root, source, new AtomicInteger(0), new AtomicInteger(0));
		
		return root;
	}
	
	public static TreeNode parse(File input) {
		
		TreeNode root = new TreeNode();
		IOSource source = new IOSource(input);
		doParse(root, source, new AtomicInteger(0), new AtomicInteger(0));
		
		return root;
	}
	
	public static TreeNode parse(IOSource source) {
		
		TreeNode root = new TreeNode();
		doParse(root, source, new AtomicInteger(0), new AtomicInteger(0));
		
		return root;
	}

	private static void doParse(TreeNode root, IOSource source,
			AtomicInteger i, AtomicInteger line) {
		TreeNode node = null;
		char c = 0;
		boolean whitespace = true;
		boolean delimit = false;
		for (; i.get() < source.buffer.limit() ; i.getAndIncrement()) {
			c = (char) source.buffer.get(i.get());
			//System.out.printf("[%d]", (int) c);
			switch (c) {
				case '\\' : {
					delimit = true;
					break;}
				case '\n' : line.getAndIncrement(); //$FALL-THROUGH$
				case '\t' :
				case '\r' :
				case ' ' : {
					if (delimit) delimit = false;
					if (!whitespace) {
						whitespace = true;
						if (node != null) {
							node.end = i.get() - 1;
							root.add(node);
							node = null;
						}
					}
					break;}
				case '"' : {
					if (delimit) {
						delimit = false;
						if (whitespace) {
							whitespace = false;
							node = new TreeNode(source, i.get());
							node.line = line.get();
							node.start = i.get();
						}
					} else {
						whitespace = true;
						TreeNode childNode = new TreeNode(source, i.get());
						childNode.line = line.get();
						childNode.start = i.get();
						
						for (i.getAndIncrement() ; i.get() < source.buffer.limit() ; i.getAndIncrement()) {
							c = (char) source.buffer.get(i.get());
							if (!delimit && (c == '"')) {
								break;
							} else if (c == '\\') {
								delimit = true;
							} else {
								delimit = false;
							}
						}
						delimit = false;
						childNode.end = i.get();
						root.add(childNode);
					}
					break;
				}
				case '.' :
				case ',' :
				case ';' : {
					if (!whitespace) {
						whitespace = true;
						if (node != null) {
							node.end = i.get() - 1;
							root.add(node);
							node = null;
						}
					}
					node = new TreeNode(source, i.get());
					node.line = line.get();
					node.start = i.get();
					node.end = i.get();
					root.add(node);
					node = null;
					break;}
				case '{' :
				case '(' :
				case '[' : {
					if (delimit) {
						delimit = false;
						if (whitespace) {
							whitespace = false;
							node = new TreeNode(source, i.get());
							node.line = line.get();
							node.start = i.get();
						}
					} else {
						whitespace = true;
						if (node != null) {
							node.end = i.get() - 1;
							root.add(node);
							node = null;
						}
						TreeNode childNode = new TreeNode(source, i.get());
						childNode.line = line.get();
						childNode.start = i.get();
						childNode.enter = i.get();
						i.getAndIncrement();
						doParse(childNode, source, i, line);
						childNode.exitLine = line.get();
						childNode.exit = i.get() - 1;
						childNode.end = i.get() - 1;
						root.add(childNode);
						i.getAndDecrement();
					}
					break;
				}
				case '}' :
				case ')' :
				case ']' : {
					if (delimit) delimit = false;
					if (node != null) {
						node.end = i.get() - 1;
						root.add(node);
						node = null;
					}
					i.getAndIncrement();
					return;
				}
				default : {
					if (delimit) delimit = false;
					if (whitespace) {
						whitespace = false;
						node = new TreeNode(source, i.get());
						node.line = line.get();
						node.start = i.get();
					}
					break;
				}
			}
		}
		if ((node != null) && (node.end == -1)) {
			node.end = i.get() - 1;//input.length - 1;
			root.add(node);
		}
	}
}

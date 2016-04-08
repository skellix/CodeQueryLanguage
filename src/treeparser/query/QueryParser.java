package treeparser.query;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryParser {
	
	public static QueryNode parse(String input) {
		QueryNode root = new QueryNode();
		ByteBuffer bytes = MappedByteBuffer.wrap(input.getBytes());
		if (!bytes.hasRemaining()) {
			return root;
		}
		doParse(root, bytes, new AtomicInteger(0), new AtomicInteger(0));
		return root;
	}

	private static void doParse(QueryNode root, ByteBuffer input, AtomicInteger i, AtomicInteger line) {
		QueryNode node = null;
		char c = 0;
		boolean whitespace = true;
		boolean delimit = false;
		for (; i.get() < input.limit() ; i.getAndIncrement()) {
			c = (char) input.get(i.get());
			switch (c) {
				case '\\' : {
					if (delimit) {
						delimit = false;
						if (whitespace) {
							whitespace = false;
							node = new QueryNode(input, i.get());
							node.line = line.get();
							node.start = i.get();
						}
					} else {
						delimit = true;
					}
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
							node = new QueryNode(input, i.get());
							node.line = line.get();
							node.start = i.get();
						}
					} else {
						whitespace = true;
						QueryNode childNode = new QueryNode(input, i.get());
						childNode.line = line.get();
						childNode.start = i.get();
						
						for (i.getAndIncrement() ; i.get() < input.limit() ; i.getAndIncrement()) {
							c = (char) input.get(i.get());
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
				case '/' : {
					if (!whitespace) {
						whitespace = true;
					}
					if (node != null) {
						node.end = i.get() - 1;
						i.getAndIncrement();
						doParse(node, input, i, line);
						root.addStep(node);
						node = null;
					}
					return;}
				case '{' :
				case '(' :
				case '[' : {
					if (delimit) {
						delimit = false;
						if (whitespace) {
							whitespace = false;
							node = new QueryNode(input, i.get());
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
						QueryNode childNode = new QueryNode(input, i.get());
						childNode.line = line.get();
						childNode.start = i.get();
						childNode.enter = i.get();
						i.getAndIncrement();
						doParse(childNode, input, i, line);
						if (childNode.hasSteps()) {
							childNode.children.add(childNode.steps.remove(0));
							QueryNode step = (QueryNode) childNode.children.get(childNode.children.size() - 1);
							while (step.hasSteps()) {
								step = step.nextStep();
							}
							if (!step.isEnclosing() && step.hasChildren()) {
								step.steps.add((QueryNode) step.children.remove(0));
							}
						}
						childNode.exitLine = line.get();
						childNode.exit = i.get() - 1;
						childNode.end = i.get() - 1;
						root.addStep(childNode);
						if (i.get() < input.limit() && input.get(i.get()) == '/') {
							i.getAndIncrement();
						}
						if (input.hasRemaining()) {
							doParse(childNode, input, i, line);
						}
						return;
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
						node = new QueryNode(input, i.get());
						node.line = line.get();
						node.start = i.get();
					}
					break;
				}
			}
		}
		if ((node != null) && (node.end == -1)) {
			node.end = i.get() - 1;
			root.addStep(node);
		}
	}

}

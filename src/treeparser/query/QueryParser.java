package treeparser.query;

import java.nio.MappedByteBuffer;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.io.IOSource;

public class QueryParser {
	
	public static QueryNode parse(String input) {
		QueryNode root = new QueryNode();
		IOSource source = new IOSource(MappedByteBuffer.wrap(input.getBytes()));
		doParse(root, source, new AtomicInteger(0), new AtomicInteger(0));
		return root;
	}
	
	private static void doParse(QueryNode root, IOSource source, AtomicInteger i, AtomicInteger line) {
		
		Stack<LinkedList<QueryNode>> stepStack = new Stack<LinkedList<QueryNode>>();
		LinkedList<QueryNode> steps = new LinkedList<QueryNode>();
		Stack<QueryNode> stack = new Stack<QueryNode>();
		
		QueryNode currentNode = new QueryNode(source, 0);
		boolean delimit = false;
		
		char c = 0;
		for (; i.get() < source.buffer.limit() ; i.getAndIncrement()) {
			c = (char) source.buffer.get(i.get());
			
			if (delimit) {
				
				delimit = false;
				
			} else {
			
				switch (c) {
				case '\\' :
					
					delimit = true;
					break;
					
				case '{' :
				case '(' :
				case '[' :
					
					currentNode.enter = i.get();
					stack.push(currentNode);
					stepStack.push(steps);
					steps = new LinkedList<QueryNode>();
					currentNode = new QueryNode(source, i.get() + 1);
					break;
					
				case '/' :
					
					{
						currentNode.end = i.get() - 1;
						byte[] data = new byte[(currentNode.end - currentNode.start) + 1];
						source.buffer.position(currentNode.start);
						source.buffer.get(data);
						currentNode.axesString = new String(data).replaceAll("[\\\\]([\\\\]*)", "$1").replaceAll("[\\\\][\\\\]", "\\\\");
					}
					steps.addLast(currentNode);
					currentNode = new QueryNode(source, i.get() + 1);
					
					break;
					
				case '}' :
				case ')' :
				case ']' :
					
					if (i.get() - currentNode.start > 1) {
						
						currentNode.end = i.get() - 1;
						byte[] data = new byte[(currentNode.end - currentNode.start) + 1];
						source.buffer.position(currentNode.start);
						source.buffer.get(data);
						currentNode.axesString = new String(data).replaceAll("[\\\\]([\\\\]*)", "$1").replaceAll("[\\\\][\\\\]", "\\\\");
						steps.addLast(currentNode);
					}
					
					currentNode = stack.pop();
					currentNode.exit = i.get();
					
					while (!steps.isEmpty()) {
						currentNode.steps.add(steps.remove(0));
					}
					
					steps = stepStack.pop();
					
					break;
				}
			}
		}
		
		if (i.get() - currentNode.start > 0) {
			
			if (currentNode.enter != -1) {
				currentNode.exit = i.get() - 1;
			}
			
			currentNode.end = i.get() - 1;
			byte[] data = new byte[(currentNode.end - currentNode.start) + 1];
			source.buffer.position(currentNode.start);
			source.buffer.get(data);
			currentNode.axesString = new String(data).replaceAll("[\\\\]([\\\\]*)", "$1").replaceAll("[\\\\][\\\\]", "\\\\");
			steps.addLast(currentNode);
		}
		
		while (!steps.isEmpty()) {
			root.steps.add(steps.remove(0));
		}
	}

}

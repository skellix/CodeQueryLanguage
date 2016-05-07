package treeparser;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.query.QueryNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class Main {
	
	public static boolean includeSiblings = false;

	public static void main(String[] args) {
		
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/static/void/.*/.*/.*", "from", "select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "descendent-or-self::if/.*/.*", "from", "select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "Test.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "{([descendent-or-self::.*])}", "from", "--file", "test.src"});
		
//		ResultNode result = queryArgs(new String[]{"select", "first::following-sibling::declare", "from", "--file", "test.xqy"});
//		TreeNode result = queryArgs(new String[]{"select", "declare/function/.*/(.*)/first::following::{.*}/child::doc/(.*)/.*/[.*]", "from", "--file", "test.xqy"});
		//TreeNode result = queryArgs(new String[]{"select", "{\\(?<!\\\\\\(\\).*}", "from", "{children (String source )}"});
		//TreeNode result = queryArgs(new String[]{"select", "class/.*", "from", "--file", "-r", "src/"});
		//TreeNode result = queryArgs(new String[]{"select", "class/.*/{.*}", "from", "--file", "-r", "src/"});
		//TreeNode result = queryArgs(new String[]{"-e", "open", "src/"});
		//TreeNode result = queryArgs(new String[]{"-e", "return", ".*"});
		//TreeNode result = queryArgs(new String[]{"-e", "--id", "10036", "open", "src/"});
		TreeNode result = queryArgs(args);
		
		if (result.hasChildren()) {
			System.out.println(result.toString());
		}
		
//		String searchOn = result.getFirstChild().toString();
//		
//		System.out.println(queryArgs(new String[]{"select", searchOn+"/.*", "from", "--file", "test.xqy"}));
	}
	
	private static TreeNode queryArgs(String[] args) {
		TreeNode result = parseArgs(Arrays.asList(args), new AtomicInteger(0));
		if (result == null) {
			return new TreeNode();
		}
//		if (!(result instanceof ResultNode)) {
//			System.err.println("[ERROR] No query was performed");
//			System.exit(-1);
//		}
		return result;
	}

	private static TreeNode parseArgs(List<String> args, AtomicInteger index) {
		
		TreeNode result = null;
		QueryNode query = null;
		TreeNode rootNode = null;
		
		while (index.get() < args.size()) {
			
			String arg = args.get(index.get());
			
			if (arg.startsWith("--")) {
				
				if (arg.equalsIgnoreCase("--include-siblings")) {
					
					includeSiblings = true;
					
				} else if (arg.equalsIgnoreCase("--file")) {
					
					if (args.size() > index.get() + 1) {
						
						index.getAndIncrement();
						String fileArg = args.get(index.get());
						boolean recursive = false;
						if (fileArg.equals("-r")) {
							recursive = true;
							index.getAndIncrement();
							fileArg = args.get(index.get());
						}
						File file = new File(fileArg);
						
						if (file.isDirectory()) {
							
							TreeNode root = fileLookup(file, recursive);
							
							return root;
							
						} else {
						
							return TreeParser.parse(file);
						}
						
					} else {
						System.err.println("[ERROR] filename expected after --file");
						System.exit(-1);
					}
				}
			} else if (arg.startsWith("-")) {
				
				if (arg.equals("-e")) {
					
					ExtendedArgs.parseArgs(args);
					index.set(args.size());
					return new TreeNode();
				}
				
			} else if (arg.equalsIgnoreCase("select")) {
				
				if (args.size() > index.get() + 1) {
					
					index.getAndIncrement();
					String queryString = args.get(index.get());
					query = QueryParser.parse(queryString);
					query.setResultInheritsSiblings(includeSiblings);
					
				} else {
					System.err.println("[ERROR] query expected after select");
					System.exit(-1);
				}
			} else if (arg.equalsIgnoreCase("from")) {
				
				if (args.size() > index.get() + 1) {
					
					index.getAndIncrement();
					String next = args.get(index.get());
					
					if (next.equals("--file")) {
						
						rootNode = parseArgs(args, index);
						
						rootNode.toString();
						
					} else if (next.equals("--")) {
						
						//TODO: add stdin
						
					} else {
						
						rootNode = TreeParser.parse(next);
					}
					
					
					if (rootNode instanceof ResultNode) {
						
						rootNode = ((ResultNode) rootNode).toTreeNode();
					}
					
					if (rootNode != null && query != null) {
						
						Collection<TreeNode> output = query.query(rootNode.children);
						result = new TreeNode();
						result.children.addAll(output);
						//result = rootNode.query(query);
						
					}
				} else {
					System.err.println("[ERROR] args expected after for");
					System.exit(-1);
				}
			}
			
			index.getAndIncrement();
		}
		return result;
	}

	private static TreeNode fileLookup(File dir, boolean recursive) {
		
		TreeNode root = new TreeNode();
		
		for (File child : dir.listFiles()) {
			
			if (child.isFile()) {
				
				root.add(TreeParser.parse(child).children);
				
			} else if (recursive && child.isDirectory()) {
				
				for (TreeNode node : fileLookup(child, recursive).children) {
					
					root.add(node);
				}
			}
		}
		
		return root;
	}

}

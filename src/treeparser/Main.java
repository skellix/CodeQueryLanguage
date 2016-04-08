package treeparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import treeparser.query.QueryNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class Main {

	public static void main(String[] args) {
		
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/static/void/.*/.*/.*", "from", "select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
		ResultNode result = queryArgs(new String[] {
				"select", "descendent-or-self::if/.*/.*", "from", "select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "src/test02/Main.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "public/class/.*/{descendent-or-self::.*}", "from", "--file", "Test.java"});
//		ResultNode result = queryArgs(new String[] {
//				"select", "{([descendent-or-self::.*])}", "from", "--file", "test.src"});
		
		if (result.hasChildren()) {
			System.out.println(result.toString());
		}
	}
	
	private static ResultNode queryArgs(String[] args) {
		TreeNode result = parseArgs(Arrays.asList(args).iterator());
		if (result == null) {
			return new ResultNode(null);
		}
		if (!(result instanceof ResultNode)) {
			System.err.println("[ERROR] No query was performed");
			System.exit(-1);
		}
		return (ResultNode) result;
	}

	private static TreeNode parseArgs(Iterator<String> args) {
		
		TreeNode result = null;
		
		QueryNode query = null;
		TreeNode rootNode = null;
		
		while (args.hasNext()) {
			String arg = args.next();
			
			if (arg.startsWith("--")) {
				if (arg.equalsIgnoreCase("--file")) {
					if (args.hasNext()) {
						String fileArg = args.next();
						try {
							RandomAccessFile file = new RandomAccessFile(new File(fileArg), "rw");
							MappedByteBuffer fileMap = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
							file.close();
							return TreeParser.parse(fileMap);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("[ERROR] filename expected after --file");
						System.exit(-1);
					}
				}
			} else if (arg.startsWith("-")) {
				//
			} else if (arg.equalsIgnoreCase("select")) {
				if (args.hasNext()) {
					String queryString = args.next();
					query = QueryParser.parse(queryString);
					query.toString();
				} else {
					System.err.println("[ERROR] query expected after select");
					System.exit(-1);
				}
			} else if (arg.equalsIgnoreCase("from")) {
				if (args.hasNext()) {
					rootNode = parseArgs(args);
					if (rootNode instanceof ResultNode) {
						rootNode = ((ResultNode) rootNode).toTreeNode();
					}
					result = rootNode.query(query);
					System.out.print("");
				} else {
					System.err.println("[ERROR] args expected after for");
					System.exit(-1);
				}
			}
		}
		return result;
	}

}

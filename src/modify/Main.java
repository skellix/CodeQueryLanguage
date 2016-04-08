package modify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;

import treeparser.TreeNode;
import treeparser.TreeParser;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class Main {

	public static void main(String[] args) {
		
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile("src/insert/bufferTest.js", "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		try {
			file.setLength(0L);
			file.write((""
					 + "function main() {\n"
					 + "	return 0;\n"
					 + "}\n"
					 + "").getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		
		MappedByteBuffer map = null;
		try {
			map = file.getChannel().map(MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		TreeNode root = TreeParser.parse(map);
		
		/* insert data in middle */
		
		String insertData = "\n"
				 + "function bar() {\n"
				 + "\n"
				 + "	return 0;\n"
				 + "}\n";
		
		ResultNode node = root.query(QueryParser.parse("function/main/.*/.*"));
		
		TreeNode dataStart = node.getFirstChild().thisNode.getPreviousSibling().getPreviousSibling().getPreviousSibling();
		
		TreeModifier.insertDataBefore(insertData, dataStart, file);
		
		/* insert data at end */
		
//		String insertData = "\n"
//				 + "function foo() {\n"
//				 + "\n"
//				 + "	return 0;\n"
//				 + "}";
//		
//		TreeNode node = root.getLastChild();
//		
//		insertDataAfter(insertData, node, file);
		
		try {
			map = file.getChannel().map(MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		node = TreeParser.parse(map).query(QueryParser.parse("function/main/.*/.*"));
		
		dataStart = node.getFirstChild().thisNode.getPreviousSibling().getPreviousSibling().getPreviousSibling();
		
		TreeModifier.deleteData(dataStart.start, node.getFirstChild().thisNode.end - dataStart.start, file);
		
		System.out.print("");
		
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

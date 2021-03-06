package modify.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import org.junit.Test;

import modify.TreeModifier;
import modify.TreeOperation;
import treeparser.TreeNode;
import treeparser.TreeParser;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TestInsert {

	@Test
	public void testInsert() {
		
		/* reset the data */
		
		{
			File testFile = new File("src/modify/test/bufferTest.js");
			
			if (!testFile.exists()) {
				try {
					testFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			RandomAccessFile file = null;
			try {
				file = new RandomAccessFile(testFile, "rw");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			try {
				file.setLength(0L);
				file.write((""
						 + "function main() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n"
						 + "").getBytes());
				file.close();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}
		}
		
		/* insert data before function main */
		
		TreeOperation operation = new TreeOperation("src/modify/test/bufferTest.js")
				.before("function/main/.*/.*/previous::.*/previous::.*/previous::.*")
				.insert("\n"
						 + "function before() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n")
				.after("function/main/.*/.*")
				.insert("\n"
						 + "function after() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n");
	}
}

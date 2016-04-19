package modify.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

import modify.TreeOperation;
import treeparser.io.IOSource;
import treeparser.query.ResultNode;

public class TestWhitespace {

	@Test
	public void testWhitespace() {
		
		File testFile = new File("src/modify/test/bufferTest.js");
		
		/* reset the data */
		
		{
			if (!testFile.exists()) {
				try {
					testFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try (FileOutputStream out = new FileOutputStream(testFile)) {
				
				out.write((""
						 + "function before() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n"
						 + "function middle() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n"
						 + "function after() {\n"
						 + "\n"
						 + "	return 0;\n"
						 + "}\n"
						 + "").getBytes());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		TreeOperation op = new TreeOperation(testFile);
		ResultNode node = op.get("function/middle/.*/{return}");
		
		String leadingWhitespace = node.getFirstChild().getLeadingWhitespace();
		
		String followingWhitespace = node.getFirstChild().getFollowingWhitespace();
		
		System.out.println("before: '" + leadingWhitespace + "'");
		System.out.println("after: '" + followingWhitespace + "'");
	}
}

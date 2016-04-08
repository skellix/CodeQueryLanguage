package modify.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

import modify.TreeOperation;

public class TestCopy {

	@Test
	public void testCopy() {
		
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
				file.close();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}
		}
		
		String data = new TreeOperation("src/modify/test/bufferTest.js")
				.includeSiblings()
				.get("function/middle/.*/.*")
				.toString();
		
		TreeOperation operation = new TreeOperation("src/modify/test/bufferTest.js")
				.includeSiblings()
				.after("function/after/.*/.*")
				.insert(data);
	}
}

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

public class TestAutoindent {

	@Test
	public void testAutoindent() {
		
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
			
			try (FileOutputStream out = new FileOutputStream(testFile)) {
				
				out.write((""
						 + "function main() {\n"
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
		
		/* insert data before function main */
		
		TreeOperation operation = new TreeOperation("src/modify/test/bufferTest.js");
		ResultNode context = operation.get("function/main/.*/.*");
		String parentIndent = context.getFirstChild().getLineIndent();
		String insertString = "\n"
				 + "if (true) {\n"
				 + "\n"
				 + "	return 0;\n"
				 + "}\n";
		
		StringBuilder stringResult = new StringBuilder();
		
		int pos = 0;
		while (pos != insertString.length()) {
			
			int next = insertString.indexOf('\n', pos);
			
			stringResult.append(parentIndent);
			stringResult.append('\t');
			stringResult.append(insertString.substring(pos, next + 1));
			pos = next + 1;
		}
		
		insertString = stringResult.toString();
		
		operation.into("function/main/.*/.*")
				.insert(insertString);
	}
}

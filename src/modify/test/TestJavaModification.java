package modify.test;

import org.junit.Test;

import modify.TreeOperation;

public class TestJavaModification {

	@Test
	public void testJavaModification() {
		
		TreeOperation data = new TreeOperation("src/modify/test/Main.java");
		
		for (int i = 0 ; i < 4 ; i ++) {
			
			System.out.println(i);
			
			if (i == 3) {
				
				System.out.print("");
			}
			
			data.into("public/class/.*/{public/static/void/.*/.*/.*}")
				.insert("System.out.println(\"Hello, world!\");\n");
		}
		
		System.out.print("");
	}
}

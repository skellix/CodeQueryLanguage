package restructure;

import treeparser.TreeNode;
import treeparser.TreeParser;

public class Main {

	public static void main(String[] args) {
		
		TreeNode parsedTree = TreeParser.parse(""
				 + "var x = 5 * 7 + 4");
		
		TreeRestructureRules rules = new TreeRestructureRules();
		rules.addRule("\\\\d+", "<node0 class=\"restructure.test.Number\"/>");
		rules.addRule(".*/\\\\*/.*", "<node1 class=\"restructure.test.Multiply\"><node0/><node2/></node1>");
		rules.addRule(".*/\\\\+/.*", "<node1 class=\"restructure.test.Add\"><node0/><node2/></node1>");
		rules.addRule("var/.*/=/.*", "<node0 class=\"restructure.test.SetVar\" name=\"node1\"><node3/></node0>");
		
		TreeNode restructured = rules.apply(parsedTree);
	}

}

package restructure;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import treeparser.TreeNode;
import treeparser.query.QueryNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TreeRestructureRules {
	
	public LinkedHashMap<QueryNode, Element> rules = new LinkedHashMap<QueryNode, Element>();
	
	public void addRule(String query, String transform) {
		
		DocumentBuilder db = null;
		
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(transform));
		
		Document doc = null;
		try {
			doc = db.parse(is);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		Element document = doc.getDocumentElement();
		
		rules.put(QueryParser.parse(query), document);
	}
	
	class ResultMatch {
		
		public QueryNode rule;
		public ArrayList<ResultNode> children;
		
		public ResultMatch(QueryNode rule, ArrayList<ResultNode> children) {
			
			this.rule = rule;
			this.children = children;
		}
	}

	public TreeNode apply(TreeNode root) {
		
		TreeNode out = root.cloneWithoutLinks();
		
		Iterator<QueryNode> ruleIterator = null;
		
		ArrayList<ResultMatch> matches = null;
		
		do {
			
			ruleIterator = rules.keySet().iterator();
			matches = new ArrayList<ResultMatch>();
			
			while (ruleIterator.hasNext()) {
				
				QueryNode rule = ruleIterator.next();
				ArrayList<ResultNode> children = root.getMatchingChildren(rule.steps.get(0));
				
				if (!children.isEmpty()) {
					
					ResultMatch match = new ResultMatch(rule, children);
					matches.add(match);
				}
			}
			
			for (int i = 0 ; i < matches.size() ; i ++) {
				
				ResultMatch match1 = matches.get(i);
				
				inner:
				for (int j = i + 1 ; j < matches.size() ; j ++) {
					
					ResultMatch match2 = matches.get(j);
					
					for (ResultNode child1 : match1.children) {
						
						for (ResultNode child2 : match2.children) {
							
							if (child1.thisNode == child2.thisNode) {
								matches.remove(j);
								j --;
								continue inner;
							}
						}
					}
				}
			}
			
			if (!matches.isEmpty()) {
				
				ResultMatch match = matches.get(0);
				QueryNode key = match.rule;
				
				if (key.nextStep() == null || key.nextStep().nextStep() == null) {
					while (match.children.size() > 1) {
						match.children.remove(match.children.size() - 1);
					}
//					if (match.children.get(0).getClass() != ResultNode.class) {
//						continue;
//					}
				}
				
				ArrayList<ResultNode> children = match.children;
				Node rule = rules.get(key);
				ResultNode transformedNodes = transform(children, rule);
				
				TreeNode parent = children.get(0).thisNode.parent;
				int index = parent.children.indexOf(children.get(0).thisNode);
				//parent.children.remove(index);
				parent.children.add(index, transformedNodes);
				transformedNodes.parent = parent;
				
				for (int i = 0 ; i < children.size() ; i ++) {
					
					TreeNode child = children.get(i).thisNode;
					child.parent.children.remove(child);
				}
				System.out.print("");
			}
			
		} while (matches.size() > 0);
		
		return null;
	}

	private ResultNode transform(ArrayList<ResultNode> children, Node rule) {
		
		ResultNode out = null;
		
		int index = 0;
		
		if (rule.getNodeName().startsWith("node")) {
			String numPart = rule.getNodeName().substring("node".length());
			index = Integer.parseInt(numPart);
		}
		
		NamedNodeMap attrs = rule.getAttributes();
		
		for (int i = 0 ; i < attrs.getLength() ; i ++) {
			
			Node attr = attrs.item(i);
			String name = attr.getNodeName();
			String value = attr.getNodeValue();
			
			if (name.equals("class")) {
				try {
					
					ResultNode thisNode = children.get(index);
					out = (ResultNode) Class.forName(value)
							.getConstructor(TreeNode.class)
							.newInstance(thisNode.thisNode);
					//out.set(thisNode);
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | SecurityException | InvocationTargetException | NoSuchMethodException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} else {
				try {
					out.getClass().getDeclaredField(name).set(out, value);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					try {
						out.getClass().getField(name).set(out, value);
					} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
							| SecurityException e1) {
						e1.printStackTrace();
						System.exit(-1);
					}
				}
			}
			System.out.print("");
		}
		
		NodeList childNodes = rule.getChildNodes();
		
		for (int i = 0 ; i < childNodes.getLength() ; i ++) {
			
			Node child = childNodes.item(i);
			String name = child.getNodeName();
			String value = child.getNodeValue();
			short type = child.getNodeType();
			out.add(transform(children, child));
			System.out.print("");
		}
		
		return out;
	}
	
}

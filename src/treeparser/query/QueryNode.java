package treeparser.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.TreeNode;
import treeparser.io.IOSource;

public class QueryNode extends TreeNode {
	
	public ArrayList<QueryNode> steps = new ArrayList<QueryNode>();
	public QueryNode parent = null;

	public QueryNode() {
		super();
	}

	public QueryNode(IOSource source, int i) {
		super(source, i);
	}
	
	@Override
	public void add(TreeNode childNode) {
		super.add(childNode);
		((QueryNode) childNode).parent = this;
	}
	
	public void addStep(QueryNode nextStep) {
		steps.add(nextStep);
		nextStep.parent = this;
	}
	
	public boolean hasSteps() {
		return steps != null && !steps.isEmpty();
	}
	
	public QueryNode nextStep() {
		if (hasSteps()) {
			return steps.get(0);
		}
		return null;
	}
	
	public String axesString = null;
	
	private boolean hasAxesChange() {
		if (axesString != null) {
			return axesString.contains("::");
		}
		if (start == -1) {
			return false;
		} else if (enter == -1) {
			axesString = getLabel();
		} else {
			axesString = getEnterLabel();
		}
		if (axesString.contains("::")) {
			return true;
		}
		return false;
	}
	
	public Collection<TreeNode> query(Collection<TreeNode> input) {
		
		return query(input, false);
	}
	
	public Collection<TreeNode> query(Collection<TreeNode> input, boolean isContinued) {
		
		ArrayList<TreeNode> output = new ArrayList<TreeNode>();
		
		for (int i = 0 ; i < steps.size() ; i ++) {
			
			QueryNode step = steps.get(i);
			
			Iterator<TreeNode> inputIterator = input.iterator();
			
			while (inputIterator.hasNext()) {
				
				TreeNode node = inputIterator.next();
				
				Collection<TreeNode> children = null;
				
				if (step.hasAxesChange()) {
					
					children = changeAxes(step.axesString, step, node);
					
					if (children != null) {
						output.addAll(children);
					}
					
				} else {
					
					if (i != 0 || isContinued) {
						node = node.getNextSibling();
					}
					children = node.getPostModifiersQueryMatch(step);
					
					if (children != null) {
						if (i + 1 < steps.size()) {
							
							output.add(node);
							
						} else {
							
							output.addAll(children);
						}
					}
				}
			}
			
			input = output;
			output = new ArrayList<TreeNode>();
		}
		
		return input;
	}

	private Collection<TreeNode> changeAxes(String axesString, QueryNode step, TreeNode node) {
		
		String axesModifier = null;
		
		if (axesString.contains("::")) {
			
			int index = axesString.indexOf("::");
			
			if (index != -1) {
				axesModifier = axesString.substring(0, index);
				axesString = axesString.substring(index + "::".length());
			}
		}
		
		if (axesModifier != null) {
			
			if (axesModifier.equals("first")) {
				
				Collection<TreeNode> children = changeAxes(axesString, step, node);
				
				if (children != null) {
					
					Iterator<TreeNode> iterator = children.iterator();
					if (iterator.hasNext()) {
						
						return Arrays.asList(iterator.next());
					}
				}
				
				return null;
				
			} else if (axesModifier.equals("last")) {
				
				Collection<TreeNode> children = changeAxes(axesString, step, node);
				
				if (children != null) {
					
					TreeNode last = null;
					Iterator<TreeNode> iterator = children.iterator();
					
					if (iterator.hasNext()) {
						last = iterator.next();
					}
					
					if (last != null) {
						return Arrays.asList(last);
					}
				}
				
				return null;
				
			} else if (axesModifier.equals("child")) {
				
				return queryChildAxes(axesString, step, node);
				
			} else if (axesModifier.equals("child-or-self")) {
				
				LinkedList<TreeNode> result = new LinkedList<TreeNode>();
				Collection<TreeNode> childAxes = queryChildAxes(axesString, step, node);
				Collection<TreeNode> thisAxes = queryThisAxes(axesString, step, node);
				
				if (thisAxes != null) {
					result.addAll(thisAxes);
				}
				if (childAxes != null) {
					result.addAll(childAxes);
				}
				
				return result;
				
			} else if (axesModifier.equals("parent")) {
				
				return queryParentAxes(axesString, step, node);
				
			} else if (axesModifier.equals("descendent")) {
				
				return queryDescendentAxes(axesString, step, node);
				
			} else if (axesModifier.equals("descendent-or-self")) {
				
				LinkedList<TreeNode> result = new LinkedList<TreeNode>();
				Collection<TreeNode> descendentAxes = queryDescendentAxes(axesString, step, node);
				Collection<TreeNode> thisAxes = queryThisAxes(axesString, step, node);
				
				if (thisAxes != null) {
					result.addAll(thisAxes);
				}
				if (descendentAxes != null) {
					result.addAll(descendentAxes);
				}
				
				return result;
				
			} else if (axesModifier.equals("ancestor")) {
				
				return queryAncestorAxes(axesString, step, node);
				
			} else if (axesModifier.equals("ancestor-or-self")) {
				
				LinkedList<TreeNode> result = new LinkedList<TreeNode>();
				Collection<TreeNode> ancestorAxes = queryAncestorAxes(axesString, step, node);
				Collection<TreeNode> thisAxes = queryThisAxes(axesString, step, node);
				
				if (ancestorAxes != null) {
					result.addAll(ancestorAxes);
				}
				if (thisAxes != null) {
					result.addAll(thisAxes);
				}
				
				return result;
				
			} else if (axesModifier.equals("following")) {
				
				return queryFollowingAxes(axesString, step, node);
				
			} else if (axesModifier.equals("following-sibling")) {
				
				return queryFollowingSiblingAxes(axesString, step, node);
				
			} else if (axesModifier.equals("preceding")) {
				
				return queryPrecedingAxes(axesString, step, node);
				
			} else if (axesModifier.equals("preceding-sibling")) {
				
				return queryPrecedingSiblingAxes(axesString, step, node);
			}
		}
		String message = String.format("Unknown axis modifier '%s' in query at index %d", axesModifier, step.start);
		new Exception(message).printStackTrace();
		System.exit(-1);
		return null;
	}

	private Collection<TreeNode> queryPrecedingSiblingAxes(String axesString, QueryNode step, TreeNode node) {
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		node = node.getPreviousSibling();
		
		while (node != null) {
			
			Collection<TreeNode> matches = queryThisAxes(axesString, step, node);
			
			if (matches != null) {
				
				out.addAll(matches);
			}
			
			node = node.getPreviousSibling();
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	private Collection<TreeNode> queryPrecedingAxes(String axesString, QueryNode step, TreeNode node) {
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		node = node.getPreviousSibling();
		
		while (node != null) {
			
			Collection<TreeNode> matches = queryThisAxes(axesString, step, node);
			
			if (matches != null) {
				
				out.addAll(matches);
			}
			
			Collection<TreeNode> descendentMatches = queryDescendentAxes(axesString, step, node);
			
			if (descendentMatches != null) {
				
				out.addAll(descendentMatches);
			}
			
			if (node.getPreviousSibling() == null) {
				
				if (node.hasParent()) {
					node = node.parent;
				}
			}
			
			node = node.getPreviousSibling();
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	private Collection<TreeNode> queryFollowingSiblingAxes(String axesString, QueryNode step, TreeNode node) {
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		node = node.getNextSibling();
		
		while (node != null) {
			
			Collection<TreeNode> matches = queryThisAxes(axesString, step, node);
			
			if (matches != null) {
				
				out.addAll(matches);
			}
			
			node = node.getNextSibling();
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	private Collection<TreeNode> queryFollowingAxes(String axesString, QueryNode step, TreeNode node) {
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		node = node.getNextSibling();
		
		while (node != null) {
			
			Collection<TreeNode> matches = queryThisAxes(axesString, step, node);
			
			if (matches != null) {
				
				out.addAll(matches);
			}
			
			Collection<TreeNode> descendentMatches = queryDescendentAxes(axesString, step, node);
			
			if (descendentMatches != null) {
				
				out.addAll(descendentMatches);
			}
			
			if (node.getNextSibling() == null) {
				
				if (node.hasParent()) {
					node = node.parent;
				}
			}
			
			node = node.getNextSibling();
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	private Collection<TreeNode> queryAncestorAxes(String axesString, QueryNode step, TreeNode node) {
		
		if (!node.hasParent()) {
			return null;
		}
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		
		Collection<TreeNode> matches = queryParentAxes(axesString, step, node.parent);
		
		if (matches != null) {
			
			out.addAll(matches);
		}
		
		Collection<TreeNode> parentMatches = queryAncestorAxes(axesString, step, node.parent);
		
		if (parentMatches != null) {
			
			out.addAll(parentMatches);
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	private Collection<TreeNode> queryDescendentAxes(String axesString, QueryNode step, TreeNode node) {
		
		if (!node.hasChildren()) {
			return null;
		}
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		
		Collection<TreeNode> matches = queryChildAxes(axesString, step, node);
		
		if (matches != null) {
			
			out.addAll(matches);
		}
		
		Iterator<TreeNode> iterator = node.children.iterator();
		
		while (iterator.hasNext()) {
			
			TreeNode child = iterator.next();
			
			Collection<TreeNode> descendentMatches = queryDescendentAxes(axesString, step, child);
			
			if (descendentMatches != null) {
				
				out.addAll(descendentMatches);
			}
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return null;
	}

	private Collection<TreeNode> queryParentAxes(String axesString, QueryNode step, TreeNode node) {
		
		if (!node.hasParent()) {
			return null;
		}
		
		return queryThisAxes(axesString, step, node.parent);
	}

	private Collection<TreeNode> queryThisAxes(String axesString, QueryNode step, TreeNode node) {
		
		Collection<TreeNode> matches = node.getPostModifiersQueryMatch(step);
		
		if (matches != null) {
			
			 return Arrays.asList(node);
		}
		
		return null;
	}

	private Collection<TreeNode> queryChildAxes(String axesString, QueryNode step, TreeNode node) {
		
		if (!node.hasChildren()) {
			return null;
		}
		
		LinkedList<TreeNode> out = new LinkedList<TreeNode>();
		Iterator<TreeNode> iterator = node.children.iterator();
		
		while (iterator.hasNext()) {
			
			TreeNode child = iterator.next();
			
			Collection<TreeNode> matches = queryThisAxes(axesString, step, child);
			
			if (matches != null) {
				
				out.addAll(matches);
			}
		}
		
		if (out.isEmpty()) {
			return null;
		}
		
		return out;
	}

	public ResultNode queryNode(TreeNode root) {
		
		ResultNode out = new ResultNode(root);
		
		if (queryNode(this, root, out)) {
			if (!this.hasSteps()) {
				for (ResultNode child : out.children) {
					out.returnSet.add(child);
					child.parent = out;
				}
			}
			for (int i = 0 ; i < out.returnSet.size() ; i ++) {
				ResultNode child1 = out.returnSet.get(i);
				for (int j = 0 ; j < out.returnSet.size() ; j ++) {
					if (i != j) {
						ResultNode child2 = out.returnSet.get(j);
						if (child1.source == child2.source) {
							if (child1.start <= child2.start) {
								if (child1.end >= child2.end) {
									out.returnSet.remove(j);
									if (j <= i) {
										i --;
									}
									j --;
								}
							}
						}
					}
				}
			}
			return out;
		} else {
			return null;
		}
	}
	
	protected static boolean queryNode(QueryNode query, TreeNode root, ResultNode out) {
		if (query.hasAxesChange()) {
			return changeAxes(query, root, out);
		}
		if (root.matches(query)) {
			if (query.hasChildren()) {
				for (TreeNode child : query.children) {
					out.children.addAll(root.getMatchingChildren((QueryNode) child));
					((TreeNode) out).addAll(out.children);
				}
				int count = 0;
				for (ResultNode child : out.children) {
					count += child.returnSet.size();
				}
				if (count == 0) {
					return false;
				}
			}
			if (query.stepsMatch(query, root, out)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean resultInheritsSiblings = false;
	
	public void setResultInheritsSiblings(boolean value) {
		resultInheritsSiblings = value;
	}
	
	private boolean stepsMatch(QueryNode query, TreeNode root, ResultNode out) { // , ArrayList<ResultNode> returnSet
		if (query.hasSteps()) {
			if (root.start == -1) {
				ArrayList<ResultNode> result = root.getMatchingChildren(query.nextStep());
				if (result != null && !result.isEmpty()) {
					out.set(null);
					for (ResultNode child : result) {
						
						if (resultInheritsSiblings) {
							
							out.children.add(child);
							
						} else if (child.returnSet.size() > 0) {
							
							out.children.addAll(child.returnSet);
						}
					}
					((TreeNode) out).children.addAll(out.children);
				} else {
					return false;
				}
			} else {
				
				ArrayList<TreeNode> rootChildren = root.parent.children;
				int rootIndex = rootChildren.indexOf(root);
				
				if (query.hasSteps() && query.nextStep().getLabel().startsWith("previous::")) {
					
					ResultNode result = root.query(query.nextStep());
					
					if (result != null) {
						
						out.siblings.addAll(result.siblings);
						out.returnSet.addAll(result.returnSet);
						result.returnSet.clear();
					}
				} else if (rootIndex + 1 < rootChildren.size()) {
					
					ResultNode result = rootChildren.get(rootIndex + 1).query(query.nextStep());
					
					if (result != null) {
						
						out.siblings.add(result);
						out.siblings.addAll(result.siblings);
						out.returnSet.addAll(result.returnSet);
						result.returnSet.clear();
						
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		} else {
			if (out.hasChildren()) {
				for (ResultNode child : out.children) {
					
					out.returnSet.add(child);
					child.returnSet.clear();
					child.parent = out;
				}
			} else {
				out.returnSet.add(out);
			}
			if (out.thisNode instanceof ResultNode) {
				
				ResultNode node = (ResultNode) out.thisNode;
				
				if (node.hasChildren()) {
					for (ResultNode child : node.children) {
						
						out.returnSet.add(child);
						child.returnSet.clear();
						child.parent = out;
					}
				} else {
					
					out.returnSet.add(node);
				}
			}
		}
		return true;
	}

	private static boolean changeAxes(QueryNode query, TreeNode root, ResultNode out) {
		
		boolean value = doSubAxesModification(query.axesString, query, root, out);
		
		return value;
	}

	private static boolean doSubAxesModification(String axesString, QueryNode query, TreeNode root, ResultNode out) {
		
		String axesModifier = null;
		
		if (axesString.contains("::")) {
			
			int index = axesString.indexOf("::");
			
			if (index != -1) {
				axesModifier = axesString.substring(0, index);
				axesString = axesString.substring(index + "::".length());
			}
		}
		
		if (axesModifier != null) {
			
			if (axesModifier.equals("first")) {
				
				ResultNode result = new ResultNode(new TreeNode());
				
				boolean value = doSubAxesModification(axesString, query, root, result);
				
				if (value) {
					
					((ResultNode) out).children.add(result.getFirstChild());
					out.add(result.getFirstChild());
				}
				
				return value;
				
			} else if (axesModifier.equals("last")) {
				
				ResultNode result = new ResultNode(new TreeNode());
				
				boolean value = doSubAxesModification(axesString, query, root, result);
				
				if (value) {
					
					((ResultNode) out).children.add(result.getLastChild());
					out.add(result.getLastChild());
				}
				
				return value;
				
			} else if (axesModifier.equals("child")) {
				
				return queryChildAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("child-or-self")) {
				
				boolean childAxes = queryChildAxes(query, root, out, axesString);
				out.children.clear();
				boolean thisAxes = queryThisAxes(query, root, out, axesString);
				return childAxes || thisAxes;
				
			} else if (axesModifier.equals("parent")) {
				
				return queryParentAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("descendent")) {
				
				return queryDescendentAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("descendent-or-self")) {
				
				boolean descendentAxes = queryDescendentAxes(query, root, out, axesString);
				out.children.clear();
				boolean thisAxes = queryThisAxes(query, root, out, axesString);
				return descendentAxes || thisAxes;
				
			} else if (axesModifier.equals("ancestor")) {
				
				return queryAncestorAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("ancestor-or-self")) {
				
				boolean ancestorAxes = queryAncestorAxes(query, root, out, axesString);
				out.children.clear();
				boolean thisAxes = queryThisAxes(query, root, out, axesString);
				return ancestorAxes || thisAxes;
				
			} else if (axesModifier.equals("following")) {
				
				return queryFollowingAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("following-sibling")) {
				
				return queryFollowingSiblingAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("preceding")) {
				
				return queryPrecedingAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("preceding-sibling")) {
				
				return queryPrecedingSiblingAxes(query, root, out, axesString);
				
			} else if (axesModifier.equals("previous")) {
				
				return queryPreviousAxes(query, root, out, axesString);
			}
		}
		
		return false;
	}

	private static boolean queryThisAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		if (root.matchesPattern(pattern)) {
			if (query.hasChildren()) {
				for (TreeNode child : query.children) {
					out.children.addAll(root.getMatchingChildren((QueryNode) child));
					((TreeNode) out).children.addAll(out.children);
				}
			}
			if (query.stepsMatch(query, root, out)) {
				out.children.add(new ResultNode(root));
				((TreeNode) out).children.addAll(out.children);
				return true;
			}
		}
		return false;
	}
	
	private static boolean queryAxesChildren(QueryNode query, TreeNode root, ResultNode out, Collection<TreeNode> axesChildren) {
		if (axesChildren != null && !axesChildren.isEmpty()) {
			if (query.hasSteps()) {
				for (TreeNode child : axesChildren) {
					ArrayList<ResultNode> matchingChildren = child.getMatchingChildren(query.nextStep());
					if (matchingChildren != null && !matchingChildren.isEmpty()) {
						out.children.add(new ResultNode(child, matchingChildren));
					}
				}
			} else {
				for (TreeNode child : axesChildren) {
					out.children.add(new ResultNode(child));
				}
			}
			((TreeNode) out).children.addAll(out.children);
			if (out.hasChildren()) {
				return true;
			}
		}
		return false;
	}

	private static boolean queryChildAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		Collection<TreeNode> axesChildren = root.getChildrenMatchingPattern(pattern);
		return queryAxesChildren(query, root, out, axesChildren);
	}

	private static boolean queryParentAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		Collection<TreeNode> axesChildren = root.getParentMatchingPattern(pattern);
		return queryAxesChildren(query, root, out, axesChildren);
	}
	
	private static boolean queryDescendentAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		Collection<TreeNode> axesChildren = root.getChildrenMatchingPattern(pattern);
		for(TreeNode axesChild : axesChildren) {
			ResultNode temp2 = new ResultNode(axesChild);
			if (queryThisAxes(query, axesChild, temp2, pattern)) {
				out.children.add(temp2);
				out.children.addAll(temp2.siblings);
			} else {
				ResultNode temp = new ResultNode(root);
				if (queryDescendentAxes(query, axesChild, temp, pattern)) {
					out.children.addAll(temp.children);
				}
			}
		}
		((TreeNode) out).children.addAll(out.children);
		return queryAxesChildren(query, root, out, axesChildren);
	}
	
	private static boolean queryAncestorAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		Collection<TreeNode> axesChildren = root.getParentMatchingPattern(pattern);
		for(TreeNode axesChild : axesChildren) {
			ResultNode temp = new ResultNode(root);
			if (queryAncestorAxes(query, axesChild, temp, pattern)) {
				out.children.addAll(temp.children);
			}
		}
		((TreeNode) out).children.addAll(out.children);
		return queryAxesChildren(query, root, out, axesChildren);
	}
	
	private static boolean queryFollowingAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		if (root.hasParent()) {
			ArrayList<TreeNode> children = root.parent.children;
			int thisIndex = children.indexOf(root);
			for (int i = thisIndex + 1 ; i < children.size() ; i ++) {
				TreeNode child = children.get(i);
				ResultNode temp = new ResultNode(root.parent);
				boolean descendentAxes = queryDescendentAxes(query, child, temp, pattern);
				boolean thisAxes = queryThisAxes(query, child, temp, pattern);
				if (descendentAxes || thisAxes) {
					out.children.addAll(temp.children);
				}
			}
			if (root.parent.hasParent()) {
				ResultNode temp = new ResultNode(root.parent.parent);
				if (queryFollowingAxes(query, root.parent, temp, pattern)) {
					out.children.addAll(temp.children);
				}
			}
			((TreeNode) out).children.addAll(out.children);
			return out.hasChildren();
		}
		return false;
	}
	
	private static boolean queryFollowingSiblingAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		if (root.hasParent()) {
			ArrayList<TreeNode> children = root.parent.children;
			int thisIndex = children.indexOf(root);
			for (int i = thisIndex + 1 ; i < children.size() ; i ++) {
				TreeNode child = children.get(i);
				ResultNode temp = new ResultNode(root.parent);
				if (queryThisAxes(query, child, temp, pattern)) {
					out.children.addAll(temp.children);
				}
			}
			((TreeNode) out).children.addAll(out.children);
			return out.hasChildren();
		}
		return false;
	}
	
	private static boolean queryPrecedingAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		if (root.hasParent()) {
			ArrayList<TreeNode> children = root.parent.children;
			int thisIndex = children.indexOf(root);
			for (int i = 0 ; i < thisIndex ; i ++) {
				TreeNode child = children.get(i);
				ResultNode temp = new ResultNode(root.parent);
				boolean descendentAxes = queryDescendentAxes(query, child, temp, pattern);
				boolean thisAxes = queryThisAxes(query, child, temp, pattern);
				if (descendentAxes || thisAxes) {
					out.children.addAll(temp.children);
				}
			}
			if (root.parent.hasParent()) {
				ResultNode temp = new ResultNode(root.parent.parent);
				if (queryPrecedingAxes(query, root.parent, temp, pattern)) {
					out.children.addAll(temp.children);
				}
			}
			((TreeNode) out).children.addAll(out.children);
			return out.hasChildren();
		}
		return false;
	}
	
	private static boolean queryPrecedingSiblingAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		if (root.hasParent()) {
			ArrayList<TreeNode> children = root.parent.children;
			int thisIndex = children.indexOf(root);
			for (int i = 0 ; i < thisIndex ; i ++) {
				TreeNode child = children.get(i);
				ResultNode temp = new ResultNode(root.parent);
				if (queryThisAxes(query, child, temp, pattern)) {
					out.children.addAll(temp.children);
				}
			}
			((TreeNode) out).children.addAll(out.children);
			return out.hasChildren();
		}
		return false;
	}
	
	private static boolean queryPreviousAxes(QueryNode query, TreeNode root, ResultNode out, String pattern) {
		
		TreeNode previous = root.getPreviousSibling();
		
		if (previous != null && previous.matchesPattern(pattern)) {
			if (query.hasSteps()) {
				
				ResultNode result = previous.query(query.nextStep());
				
				if (result != null) {
					
					out.returnSet.addAll(result.returnSet);
					return true;
				}
			} else {
				
				out.returnSet.add(new ResultNode(previous));
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String toString() {
		return queryNodeToString(new AtomicInteger(0));
	}
	
	protected String queryNodeToString(AtomicInteger indent) {
		StringBuilder stringBuilder = new StringBuilder();
		if (start != -1) {
			if (enter != -1) {
				stringBuilder.append(getEnterLabel().replaceAll("\n", ""));
			} else if (end != -1) {
				stringBuilder.append(getLabel().replaceAll("\n", ""));
			}
		}
		if (children.size() > 0) {
			indent.getAndIncrement();
			for (TreeNode child : children) {
				stringBuilder.append("\n");
				for (int i = indent.get() ; i > 0 ; i --) {
					stringBuilder.append("  ");
				}
				stringBuilder.append(((QueryNode) child).queryNodeToString(indent));
			}
			indent.getAndDecrement();
		}
		if (exit != -1) {
			stringBuilder.append('\n');
			for (int i = indent.get() ; i > 0 ; i --) {
				stringBuilder.append("  ");
			}
			stringBuilder.append(getExitLabel().replaceAll("\n", ""));
		}
		if (steps.size() > 0) {
			for (QueryNode step : steps) {
				stringBuilder.append("\n");
				for (int i = indent.get() ; i > 0 ; i --) {
					stringBuilder.append("  ");
				}
				stringBuilder.append("/ ");
				stringBuilder.append(step.queryNodeToString(indent));
			}
		}
		return stringBuilder.toString();
	}

}

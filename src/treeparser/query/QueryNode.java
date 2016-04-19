package treeparser.query;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import treeparser.TreeNode;
import treeparser.io.IOSource;

public class QueryNode extends TreeNode {
	
	public ArrayList<QueryNode> steps = new ArrayList<QueryNode>();
	public QueryNode parent = null;

	public QueryNode() {
		super();
	}
	
	/*public QueryNode(String queryString) {
		TreeNode root = parseAsQuery(TreeParser.parse(queryString));
		this.source = root.source;
		this.children = root.children;

		this.start = root.start;
		this.enter = root.enter;
		this.exit = root.exit;
		this.end = root.end;

		this.line = root.line;
		this.exitLine = root.exitLine;
	}//*/

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
		if (start == -1) {
			return false;
		} else if (enter == -1) {
			axesString = getLabel();
		} else {
			axesString = getEnterLabel();
		}
		if (axesString.startsWith("child::")
				|| axesString.startsWith("child-or-self::")
				|| axesString.startsWith("parent::")
				|| axesString.startsWith("descendent::")
				|| axesString.startsWith("descendent-or-self::")
				|| axesString.startsWith("ancestor::")
				|| axesString.startsWith("ancestor-or-self::")
				|| axesString.startsWith("following::")
				|| axesString.startsWith("following-sibling::")
				|| axesString.startsWith("preceding::")
				|| axesString.startsWith("preceding-sibling::")
				|| axesString.startsWith("previous::")) {
			return true;
		}
		return false;
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
		//return queryNode(this, root, 0, new ResultNode(null));
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
						
						//out.siblings.add(result);
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
//				if (query.hasChildren()) {
//					for (ResultNode child : out.children) {
//						if (child.returnSet.size() > 0) {
//							
//							out.returnSet.addAll(child.returnSet);
//							child.returnSet.clear();
//							
//							if (resultInheritsSiblings) {
//								
//								out.siblings.addAll(child.siblings);
//								child.siblings.clear();
//							}
//						}
//						
//						child.parent = out;
//					}
//					
//				} else {
					for (ResultNode child : out.children) {
						
						out.returnSet.add(child);
						child.returnSet.clear();
						child.parent = out;
					}
//				}
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
		String pattern = null;
		if (query.axesString.contains("::")) {
			pattern = query.axesString.substring(query.axesString.indexOf("::") + 2);
		}
		if (query.axesString.startsWith("child::")) {
			return queryChildAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("child-or-self::")) {
			boolean childAxes = queryChildAxes(query, root, out, pattern);
			out.children.clear();
			boolean thisAxes = queryThisAxes(query, root, out, pattern);
			return childAxes || thisAxes;
		} else if (query.axesString.startsWith("parent::")) {
			return queryParentAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("descendent::")) {
			return queryDescendentAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("descendent-or-self::")) {
			boolean descendentAxes = queryDescendentAxes(query, root, out, pattern);
			out.children.clear();
			boolean thisAxes = queryThisAxes(query, root, out, pattern);
			return descendentAxes || thisAxes;
		} else if (query.axesString.startsWith("ancestor::")) {
			return queryAncestorAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("ancestor-or-self::")) {
			boolean ancestorAxes = queryAncestorAxes(query, root, out, pattern);
			out.children.clear();
			boolean thisAxes = queryThisAxes(query, root, out, pattern);
			return ancestorAxes || thisAxes;
		} else if (query.axesString.startsWith("following::")) {
			return queryFollowingAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("following-sibling::")) {
			return queryFollowingSiblingAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("preceding::")) {
			return queryPrecedingAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("preceding-sibling::")) {
			return queryPrecedingSiblingAxes(query, root, out, pattern);
		} else if (query.axesString.startsWith("previous::")) {
			return queryPreviousAxes(query, root, out, pattern);
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
			//String format = String.format("$1%%%ds", indent.get() * 2);
			//String replacement = String.format(format, "");
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

	/*protected static ResultNode queryNode2(QueryNode query, TreeNode root, int index, ResultNode out) {
		if ((query.start == -1) && (root.start == -1)) {
			ResultNode child = new ResultNode(root);
			//ArrayList<ResultNode> matchingNodes = new ArrayList<ResultNode>();
			for (int i = 0 ; i < query.steps.size() ; i ++) {
				for (int j = 0 ; j < root.children.size() ; j ++) {
					ResultNode result = queryNode(query.steps.get(i), root.children.get(j), j, child);
					if (result != null) {
						child.children.add(result);
						//matchingNodes.add(result);
					}
				}
			}
			if (!child.children.isEmpty()) {
				out.children.add(child);
				//out = new ResultNode(root, matchingNodes);
			}
		} else if (query.enter == -1 && root.enter == -1) {
			String queryString = new String(query.source, query.start, (query.end - query.start) + 1);
			String rootString = new String(root.source, root.start, (root.end - root.start) + 1);
			Matcher matcher = Pattern.compile(queryString).matcher(rootString);
			if (matcher.find()) {
				ResultNode child = new ResultNode(root);
				//ArrayList<ResultNode> matchingNodes = new ArrayList<ResultNode>();
				//matchingNodes.add(new ResultNode(root));
				int next = index + 1;
				if (next < root.parent.children.size()) {
					for (int i = 0 ; i < query.steps.size() ; i ++) {
						ResultNode result = queryNode(query.steps.get(i), root.parent.children.get(next), next, child);
						if (result != null) {
							child.children.add(result);
							//matchingNodes.add(result);
						}
					}
				}
				if (!child.children.isEmpty()) {
					out.children.add(child);
					//out = new ResultNode(root, matchingNodes);
				}
			}
		} else if (query.enter != -1 && root.enter != -1 && query.exit != -1 && root.exit != -1) {
			String queryStartString = new String(query.source, query.start, (query.enter - query.start) + 1);
			// delimit regex specific parts
			queryStartString = queryStartString.replaceAll("\\\\(.)", "$1");
			queryStartString = queryStartString.replaceAll("(?<!\\\\)([\\(\\)\\{\\}\\[\\]])", "\\\\$1");
			String rootStartString = new String(root.source, root.start, (root.enter - root.start) + 1);
			Matcher matcher = Pattern.compile(queryStartString).matcher(rootStartString);
			if (matcher.find()) {
				String queryEndString = new String(query.source, query.exit, (query.end - query.exit) + 1);
				// delimit regex specific parts
				queryEndString = queryEndString.replaceAll("\\\\(.)", "$1");
				queryEndString = queryEndString.replaceAll("(?<!\\\\)([\\(\\)\\{\\}\\[\\]])", "\\\\$1");
				String rootEndString = new String(root.source, root.exit, (root.end - root.exit) + 1);
				matcher = Pattern.compile(queryEndString).matcher(rootEndString);
				if (matcher.find()) {
					ResultNode child = new ResultNode(root);
					//ArrayList<ResultNode> matchingNodes = new ArrayList<ResultNode>();
					for (int i = 0 ; i < query.children.size() ; i ++) {
						for (int j = 0 ; j < root.children.size() ; j ++) {
							ResultNode result = queryTreeNode((QueryNode) query.children.get(i), root.children.get(j), j, child);
							if (result != null) {
								child.children.add(result);
								//matchingNodes.add(result);
							}
						}
					}
					if (!child.children.isEmpty()) {
						ArrayList<ResultNode> stepNodes = new ArrayList<ResultNode>();
						stepNodes.add(child);
						for (int i = 0 ; i < query.steps.size() ; i ++) {
							ResultNode nextChild = new ResultNode(root);
							int next = index + 1;
							if (next < root.parent.children.size()) {
								ResultNode result = queryNode(query.steps.get(i), root.parent.children.get(next), next, nextChild);
								if (result != null) {
									stepNodes.add(result);
								} else {
									return null;
								}
							} else {
								return null;
							}
							
						}
						out.children.addAll(stepNodes);
						//out = new ResultNode(new ResultNode(root, stepNodes), matchingNodes);
					}
				}
			}
		} else if (query.enter == -1 && root.enter != -1) {
			String queryString = new String(query.source, query.start, (query.end - query.start) + 1);
			String rootStartString = new String(root.source, root.start, (root.enter - root.start) + 1);
			Matcher matcher = Pattern.compile(queryString).matcher(rootStartString);
			if (matcher.find()) {
				//ResultNode child = new ResultNode(root);
				ArrayList<ResultNode> matchingNodes = new ArrayList<ResultNode>();
				matchingNodes.add(new ResultNode(root));
				//matchingNodes.add(new ResultNode(root));
				int next = index + 1;
				if (next < root.parent.children.size()) {
					for (int i = 0 ; i < query.steps.size() ; i ++) {
						ResultNode result = queryNode(query.steps.get(i), root.parent.children.get(next), next, new ResultNode(null));
						if (result != null) {
							//child.children.add(result);
							matchingNodes.add(result.children.get(0));
						}
					}
				}
				if (matchingNodes.size() > 0) {
					out.children.addAll(matchingNodes);
					//out = new ResultNode(root, matchingNodes);
				}
			}
		} else {
			System.err.println("Found case without children!");
		}
		return out;
	}
	
	private static ResultNode queryTreeNode(QueryNode query, TreeNode root, int index, ResultNode out) {
		ResultNode result = queryNode(query, root, index, out);
		return result;
	}//*/
	
	/*protected static  TreeNode queryNode(TreeNode query, TreeNode root) {
		TreeNode out = null;
		if ((query.start == -1) && (root.start == -1)) {
			if ((root.children.size() > 0) && (query.children.size() > 0)) {
				out = root.cloneWithoutLinks();
				for (int i = 0 ; i < query.children.size() ; i ++) {
					for (int j = 0 ; j < root.children.size() ; j ++) {
						ArrayList<TreeNode> result = queryLinks(query.children.get(i), root, j);
						if (result.size() > 0) {
							for (TreeNode node : result) {
								out.add(node);
							}
						}
					}
				}
			}
		} else if (query.enter == -1) {
			if ((root.children.size() > 0) && (query.children.size() > 0)) {
				out = root.cloneWithoutLinks();
				for (int i = 0 ; i < query.children.size() ; i ++) {
					for (int j = 0 ; j < root.children.size() ; j ++) {
						TreeNode result = queryNode(query.children.get(i), root.children.get(j));
						if (result != null) {
							out.add(result);
						}
					}
				}
			}
		} else {
			System.err.println("Found case without children!");
		}
		return out;
	}//*/

	/*private static ArrayList<TreeNode> queryLinks(TreeNode query, TreeNode root, int start) {
		ArrayList<TreeNode> out = new ArrayList<TreeNode>();
		for (int i = start ; (i < root.children.size()) && (query != null) ; i ++) {
			TreeNode child = root.children.get(i);
			if ((query.children.size() > 0) && (query.enter != -1) && (query.exit != -1)) {
				if (child.children.size() > 0 && (child.enter != -1) && (child.exit != -1)) {
					if (query.enter - query.start != child.enter - child.start) {
						return new ArrayList<TreeNode>();
					}
					if (query.end - query.exit != child.end - child.exit) {
						return new ArrayList<TreeNode>();
					}
					for (int j = 0 ; query.start + j <= query.enter && child.start + j <= child.enter; j ++) {
						if (query.source[query.start + j] != child.source[child.start + j]) {
							return new ArrayList<TreeNode>();
						}
					}
					for (int j = 0 ; query.exit + j <= query.end && child.exit + j <= child.end; j ++) {
						if (query.source[query.exit + j] != child.source[child.exit + j]) {
							return new ArrayList<TreeNode>();
						}
					}
					TreeNode clone = child.cloneWithoutLinks();
					for (int j = 1 ; j < query.children.size() ; j ++) {
						for (int k = 0 ; k < child.children.size() ;  k ++) {
							ArrayList<TreeNode> result = queryLinks(query.children.get(j), child, k);
							if (result.size() > 0) {
								for (TreeNode node : result) {
									clone.add(node);
								}
							} else {
								return new ArrayList<TreeNode>();
							}
						}
					}
					out.add(clone);
				} else {
					return new ArrayList<TreeNode>();
				}
			} else {
				if (query.source == null) {
					return out;
				}
				// TODO add fix for "()" matching everything
				Pattern pattern = Pattern.compile(new String(query.source, query.start, (query.end - query.start) + 1));
				Matcher matcher = pattern.matcher(new String(child.source, child.start, (child.end - child.start) + 1));
				if (matcher.find()) {
					out.add(child.cloneWithoutLinks());
				} else {
					return new ArrayList<TreeNode>();
				}
			}
			// post loop
			if (query.children.size() == 0) {
				break;
			}
			query = query.children.get(0);
			if (query.source == null) {
				return out;
			}
		}
		return out;
	}//*/
	
	/*private static TreeNode parseAsQuery(TreeNode root) {
		if (root.start != -1) {
			if (root.enter != -1) {
				if (root.exit != -1) {
					//
				} else {
					System.err.printf("[ERROR] End of block not found for block that starts on line %d\n", root.line);
				}
			} else if (root.exit != -1) {
				//
			} else {
				ArrayList<TreeNode> newChildren = new ArrayList<TreeNode>();
				{
					int startIndex = root.start;
					int endIndex = startIndex;
					int i = startIndex;
					for (; i <= root.end ; i ++) {
						if (root.source[i] == '/') {
							if (i == 0) {
								//newChildren.add(new TreeNode(root.source, -1, -1, root.line));
							} else {
								endIndex = i - 1;
								newChildren.add(new TreeNode(root.source, startIndex, endIndex, root.line));
							}
							startIndex = i + 1;
						}
					}
					if (startIndex > endIndex) {
						endIndex = i - 1;
						TreeNode parent = root.parent;
						if (endIndex < startIndex) {
							outer: for (int j = 0 ; j < parent.children.size() ; j ++) {
								if (parent.children.get(j) == root) {
									if (j + 1 < parent.children.size()) {
										while (++ j < parent.children.size()) {
											TreeNode child = parent.children.remove(j);
											if (child.children.size() > 0) {
												TreeNode emptyNode = new TreeNode();
												emptyNode.parent = child;
												child.children.add(0, emptyNode);
											}
											newChildren.add(child);
											if (j + 1 < parent.children.size()) {
												TreeNode next = parent.children.get(j + 1);
												if (next.start != -1 && next.source[next.start] == '/') {
													startIndex = next.start;
													endIndex = startIndex;
													i = startIndex;
													for (; i <= next.end ; i ++) {
														if (next.source[i] == '/') {
															if (i == 0) {
																//newChildren.add(new TreeNode(root.source, -1, -1, root.line));
															} else {
																endIndex = i - 1;
																newChildren.add(new TreeNode(next.source, startIndex, endIndex, next.line));
															}
															startIndex = i + 1;
														}
													}
													if (startIndex <= endIndex) {
														break outer;
													}
												}
											}
										}
									} else {
										System.err.printf("[ERROR] Expecting node after '/' in path on line %d\n", root.line);
									}
									break;
								}
							}
						} else {
							newChildren.add(new TreeNode(root.source, startIndex, endIndex, root.line));
						}
					}
				}
				for (int i = 1 ; i < newChildren.size() ; i ++) {
					newChildren.get(i - 1).add(newChildren.get(i));
				}
				if (newChildren.size() > 0) {
					root.start = newChildren.get(0).start;
					root.end = newChildren.get(0).end;
					root.children = newChildren.get(0).children;
				}
			}
		} else {
			//System.err.printf("[ERROR] No start found for block reported on line %d\n", root.line);
		}
		for (int i = 0 ; i < root.children.size() ; i ++) {
			System.out.print("");
			parseAsQuery(root.children.get(i));
		}
		return root;
	}//*/
}

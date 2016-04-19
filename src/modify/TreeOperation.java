package modify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel.MapMode;

import treeparser.TreeNode;
import treeparser.TreeParser;
import treeparser.io.IOSource;
import treeparser.query.QueryNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TreeOperation {
	
	String fileName = null;
	QueryNode before = null;
	QueryNode after = null;
	QueryNode into = null;
	boolean includeSiblings = false;
	boolean autoindent = false;

	public TreeOperation(String fileName) {
		this.fileName = fileName;
	}
	
	public TreeOperation(File file) {
		this.fileName = file.getPath();
	}
	
	public TreeOperation before(String path) {
		before = QueryParser.parse(path);
		before.setResultInheritsSiblings(includeSiblings);
		return this;
	}

	public TreeOperation after(String path) {
		after = QueryParser.parse(path);
		after.setResultInheritsSiblings(includeSiblings);
		return this;
	}
	
	public TreeOperation into(String path) {
		into = QueryParser.parse(path);
		into.setResultInheritsSiblings(includeSiblings);
		return this;
	}
	
	public TreeOperation autoindent() {
		autoindent = true;
		return this;
	}
	
	public TreeOperation includeSiblings() {
		includeSiblings = true;
		return this;
	}
	
	private ResultNode unsafeGet(String path) {
		
		IOSource source = new IOSource(fileName);
		
		TreeNode root = TreeParser.parse(source);
		
		QueryNode query = QueryParser.parse(path);
		query.setResultInheritsSiblings(includeSiblings);
		
		ResultNode node = root.query(query);
		
		return node;
	}
	
	public ResultNode get(String path) {
		
		ResultNode node = unsafeGet(path);
		
		includeSiblings = false;
		autoindent = false;
		before = null;
		after = null;
		into = null;
		
		return node;
	}

	public TreeOperation insert(String insertData) {
		
		IOSource source = new IOSource(fileName);
		
		int insertLength = insertData.getBytes().length;
		
		int insertPoint = -1;
		
		{
			int mapSize = source.buffer.limit();
			
			TreeNode root = TreeParser.parse(source);
			
			if (before != null) {
				
				ResultNode node = root.query(before);
				
				if (node == null) {
					throw new NullPointerException();
				}
				
				insertPoint = node.getFirstChild().thisNode.start - 1;
				
			} else if (after != null) {
				
				ResultNode node = root.query(after);
				
				if (node == null) {
					throw new NullPointerException();
				}
				
				insertPoint = node.getLastChild().thisNode.end;
				
			} else if (into != null) {
				
				ResultNode node = root.query(into);
				
				if (node == null) {
					throw new NullPointerException();
				}
				
				insertPoint = node.getLastChild().enter + 1;
			}
			
			TreeModifier.insertDataInMap(insertData, insertPoint, source, mapSize);
		}
		
		
		includeSiblings = false;
		autoindent = false;
		before = null;
		after = null;
		into = null;
		
		return this;
	}

	public TreeOperation delete(String path) {
		
		IOSource source = new IOSource(fileName);
		
		long startPoint = -1;
		long dataLength = -1;
		{
			TreeNode root = TreeParser.parse(source);
			
			QueryNode query = QueryParser.parse(path);
			
			query.setResultInheritsSiblings(includeSiblings);
			
			ResultNode node = root.query(query);
			
			TreeNode dataStart = node.getFirstChild().thisNode;
			
			TreeNode dataEnd = node.getLastChild().thisNode;
			
			startPoint = dataStart.start;
			dataLength = dataEnd.end - dataStart.start;
		}
		
		TreeModifier.deleteData(startPoint, dataLength, source);
		
		includeSiblings = false;
		autoindent = false;
		before = null;
		after = null;
		into = null;
		
		return this;
	}

}

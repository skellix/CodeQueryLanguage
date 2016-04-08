package modify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import treeparser.TreeNode;
import treeparser.TreeParser;
import treeparser.query.QueryNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TreeOperation {
	
	String fileName = null;
	QueryNode before = null;
	QueryNode after = null;
	QueryNode into = null;
	boolean includeSiblings = false;

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
	
	public TreeOperation includeSiblings() {
		includeSiblings = true;
		return this;
	}
	
	public ResultNode get(String path) {
		
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(fileName, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MappedByteBuffer map = null;
		try {
			map = file.getChannel().map(MapMode.READ_ONLY, 0, file.length());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		TreeNode root = TreeParser.parse(map);
		
		QueryNode query = QueryParser.parse(path);
		query.setResultInheritsSiblings(includeSiblings);
		
		ResultNode node = root.query(query);
		
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		includeSiblings = false;
		before = null;
		after = null;
		into = null;
		
		return node;
	}

	public TreeOperation insert(String insertData) {
		
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MappedByteBuffer map = null;
		try {
			map = file.getChannel().map(MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		TreeNode root = TreeParser.parse(map);
		
		ResultNode node = null;
		
		if (before != null) {
			
			node = root.query(before);
			
		} else if (after != null) {
			
			node = root.query(after);
			
		} else if (into != null) {
			
			node = root.query(into);
		}
		
		map = null;
		
		if (before != null) {
			
			TreeNode dataStart = node.getFirstChild().thisNode;
			
			node = null;
			
			TreeModifier.insertDataBefore(insertData, dataStart, file);
			
		} else if (after != null) {
			
			TreeNode dataStart = node.getLastChild().thisNode;
			
			node = null;
			
			TreeModifier.insertDataAfter(insertData, dataStart, file);
			
		} else if (into != null) {
			
			TreeNode dataStart = node.getLastChild().thisNode;
			
			node = null;
			
			TreeModifier.insertDataInto(insertData, dataStart, file);
		}
		
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		includeSiblings = false;
		before = null;
		after = null;
		into = null;
		
		return this;
	}

	public TreeOperation delete(String path) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(fileName, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MappedByteBuffer map = null;
		try {
			map = file.getChannel().map(MapMode.READ_WRITE, 0, file.length());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		TreeNode root = TreeParser.parse(map);
		
		QueryNode query = QueryParser.parse(path);
		
		query.setResultInheritsSiblings(includeSiblings);
		
		ResultNode node = root.query(query);
		
		TreeNode dataStart = node.getFirstChild().thisNode;
		
		TreeNode dataEnd = node.getLastChild().thisNode;
		
		TreeModifier.deleteData(dataStart.start, dataEnd.end - dataStart.start, file);
		
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		includeSiblings = false;
		before = null;
		after = null;
		into = null;
		
		return this;
	}

}

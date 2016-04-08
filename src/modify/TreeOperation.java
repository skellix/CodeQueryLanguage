package modify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
		
		int insertPoint = -1;
		
		{
			MappedByteBuffer map = null;
			try {
				map = file.getChannel().map(MapMode.READ_WRITE, 0, file.length());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			TreeNode root = TreeParser.parse(map);
			
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
			
			try {
				
				Field cleanerField = map.getClass().getDeclaredField("cleaner");
				boolean accessible = cleanerField.isAccessible();
				cleanerField.setAccessible(true);
				Object cleaner = cleanerField.get(map);
				Method method = cleaner.getClass().getDeclaredMethod("clean");
				method.invoke(cleaner);
				cleanerField.setAccessible(accessible);
				
			} catch (NoSuchFieldException e1) {
				e1.printStackTrace();
				System.exit(-1);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (SecurityException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		TreeModifier.insertData(insertData, insertPoint, file);
		
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
		
		long startPoint = -1;
		long dataLength = -1;
		{
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
			
			startPoint = dataStart.start;
			dataLength = dataEnd.end - dataStart.start;
			
			try {
				
				Field cleanerField = map.getClass().getDeclaredField("cleaner");
				boolean accessible = cleanerField.isAccessible();
				cleanerField.setAccessible(true);
				Object cleaner = cleanerField.get(map);
				Method method = cleaner.getClass().getDeclaredMethod("clean");
				method.invoke(cleaner);
				cleanerField.setAccessible(accessible);
				
			} catch (NoSuchFieldException e1) {
				e1.printStackTrace();
				System.exit(-1);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (SecurityException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		TreeModifier.deleteData(startPoint, dataLength, file);
		
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

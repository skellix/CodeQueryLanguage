package modify;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import treeparser.TreeNode;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TreeModifier {
	
	public static void insertDataInto(String insertData, TreeNode node, RandomAccessFile file) {
		
		int insertPoint = node.enter + 1;
		
		insertData(insertData, insertPoint, file);
		
		//updateOffsets(node, insertPoint, insertData);
	}
	
	private static void updateOffsets(TreeNode node, int insertPoint, String insertData) {
		
		int lines = 0;
		for (int i = 0 ; i < insertData.length() ; i ++) {
			if (insertData.charAt(i) == '\n') {
				lines ++;
			}
		}
		
		if (node.start > insertPoint) {
			node.addOffset(insertData.getBytes().length, lines);
		}
		
		ResultNode nodes = node.query(QueryParser.parse("following::.*"));
		
		System.out.print("");
	}

	public static void insertDataBefore(String insertData, TreeNode node, RandomAccessFile file) {
		
		int insertPoint = node.start - 1;
		
		insertData(insertData, insertPoint, file);
		
		//updateOffsets(node, insertPoint, insertData);
	}
	
	public static void insertDataAfter(String insertData, TreeNode node, RandomAccessFile file) {
		
		int insertPoint = node.end;
		
		insertData(insertData, insertPoint, file);
		
		//updateOffsets(node, insertPoint, insertData);
	}
	
	public static void insertData(String insertData, int insertPoint, RandomAccessFile file) {
		
		FileLock lock = null;
		try {
			lock = file.getChannel().lock(insertPoint + 1, file.length() - (insertPoint + 1), false);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(-1);
		}
		
		int insertLength = insertData.getBytes().length;
		
		long insertOffset = (insertPoint + 1) + insertLength;
		
		long oldLength = 0;
		try {
			oldLength = file.length();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		long newLength = oldLength + insertLength;
		try {
			file.setLength(newLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if ((oldLength - 1) - insertPoint > 0) {
				byte[] moveBuffer = new byte[100];
				
				for (long i = oldLength - 1 ; i >= insertPoint ; i -= moveBuffer.length) {
					
					long start = Math.max(i - moveBuffer.length, insertPoint);
					int length = (int) (i - start);
					
					file.seek(start);
					file.readFully(moveBuffer, 0, length);
					
					long newOffset = insertOffset + (i - (oldLength - 1));
					file.seek(newOffset);
					file.write(moveBuffer, 0, length);
				}
//				for (long i = insertPoint + 1 ; i < oldLength ; i += moveBuffer.length) {
//					
//					int length = (int) Math.min(moveBuffer.length, oldLength - i);
//					file.seek(i);
//					file.readFully(moveBuffer, 0, length);
//					
//					long newOffset = insertOffset + (i - (insertPoint + 1));
//					file.seek(newOffset);
//					file.write(moveBuffer, 0, length);
//				}
			}
			
			file.seek(insertPoint + 1);
			file.write(insertData.getBytes());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			lock.release();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void deleteData(long startPoint, long dataLength, RandomAccessFile file) {
		
		long fileLength = 0L;
		try {
			fileLength = file.length();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		
		long offset = startPoint + dataLength;
		
		FileLock lock = null;
		try {
			lock = file.getChannel().lock(startPoint + 1, fileLength - (startPoint + 1), false);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(-1);
		}
		
		try {
			if (dataLength > 0) {
				byte[] moveBuffer = new byte[100];
				for (long i = offset + 1 ; i < fileLength ; i += moveBuffer.length) {
					
					int length = (int) Math.min(moveBuffer.length, fileLength - i);
					file.seek(i);
					file.readFully(moveBuffer, 0, length);
					
					String data = new String(moveBuffer, 0, length);
					
					long newOffset = startPoint + (i - (offset + 1));
					file.seek(newOffset);
					file.write(moveBuffer, 0, length);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			file.setLength(file.length() - (dataLength + 1));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			lock.release();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}

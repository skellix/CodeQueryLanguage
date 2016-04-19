package modify;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileLock;

import treeparser.TreeNode;
import treeparser.io.IOSource;
import treeparser.query.QueryParser;
import treeparser.query.ResultNode;

public class TreeModifier {
	
	public static void insertDataInto(String insertData, TreeNode node, RandomAccessFile file) {
		
		int insertPoint = node.enter + 1;
		
		insertData(insertData, insertPoint, file);
		
		//updateOffsets(node, insertPoint, insertData);
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
	
	public static void insertDataInMap(String insertData, int insertPoint, IOSource source, int mapSize) {
		
		int insertLength = insertData.getBytes().length;
		
		int insertOffset = insertPoint + 1 + insertLength;
		
		int oldLength = mapSize - insertLength;
		
		source.setLength(source.length() + insertLength);
		
		FileLock lock = null;
		try {
			lock = source.getLock(insertOffset, source.length(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (insertLength > 0) {
			byte[] moveBuffer = new byte[100];
			
			for (int i = 0 ; i < mapSize ; i += 100) {
				
				int getEnd = oldLength - i;
				int getStart = Math.max(getEnd - 100, insertPoint + 1);
				int getLength = getEnd - getStart;
				
				if (getLength <= 0) {
					break;
				}
				
				int putEnd = mapSize - i;
				int putStart = Math.max(putEnd - 100, insertOffset);
				int putLength = putEnd - putStart;
				
				//System.out.printf("start: %d\n", getStart);
				source.seek(getStart);
				source.buffer.get(moveBuffer, 0, getLength);
				
				String dataString = new String(moveBuffer, 0, getLength);
				
				/* vvv Debug only vvv */
//				byte[] replaceBuffer = new byte[putLength];
//				map.position(putStart);
//				map.get(replaceBuffer, 0, putLength);
//				String replacing = new String(replaceBuffer, 0, putLength);
//				
//				byte[] beforeBuffer = new byte[getStart];
//				map.position(0);
//				map.get(beforeBuffer, 0, getStart);
//				String before = new String(beforeBuffer);
				/* ^^^ Debug only ^^^ */
				
				source.seek(putStart);
				source.buffer.put(moveBuffer, 0, putLength);
			}
			
			/* vvv Debug only vvv */
//			byte[] replaceBuffer = new byte[insertLength];
//			map.position(insertPoint + 1);
//			map.get(replaceBuffer, 0, insertLength);
//			String replacing = new String(replaceBuffer, 0, insertLength);
//			
//			byte[] beforeBuffer = new byte[insertPoint + 1];
//			map.position(0);
//			map.get(beforeBuffer, 0, insertPoint + 1);
//			String before = new String(beforeBuffer);
//			
//			byte[] afterBuffer = new byte[mapSize - insertOffset];
//			map.position(insertOffset);
//			map.get(afterBuffer, 0, mapSize - insertOffset);
//			String after = new String(afterBuffer);
			/* ^^^ Debug only ^^^ */
			
			source.seek(insertPoint + 1);
			source.buffer.put(insertData.getBytes());
			
//			for (int i = oldLength - 1 ; i >= insertPoint ; i -= moveBuffer.length) {
//				
//				int length = Math.min(Math.max(Math.max(i - moveBuffer.length, 0), i - insertPoint + 1), 100);
//				int start = i - length;
//				//int start = Math.max(i - moveBuffer.length, insertPoint);
//				//int length = (i - start) + 1;
//				System.out.printf("> %d-%d / %d\n", i, i + length, mapSize);
//				
//				map.position(start);
//				map.get(moveBuffer, 0, length);
//				
//				String dataString = new String(moveBuffer, 0, length);
//				
//				int newOffset = insertOffset + (i - (oldLength - 1));
//				
//				/* vvv Debug only vvv */
//				byte[] replaceBuffer = new byte[length];
//				map.position(newOffset);
//				map.get(replaceBuffer, 0, length);
//				String replacing = new String(replaceBuffer, 0, length);
//				
//				byte[] beforeBuffer = new byte[newOffset - 1];
//				map.position(0);
//				map.get(beforeBuffer, 0, newOffset - 1);
//				String before = new String(beforeBuffer);
//				/* ^^^ Debug only ^^^ */
//				
//				map.position(newOffset);
//				map.put(moveBuffer, 0, length);
//			}
		}
		try {
			lock.release();
			lock.channel().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
					int length = (int) (i - start) + 1;
					
					file.seek(start);
					file.readFully(moveBuffer, 0, length);
					
					String dataString = new String(moveBuffer, 0, length);
					
					long newOffset = insertOffset + (i - (oldLength - 1));
					
					/* vvv Debug only vvv */
					byte[] replaceBuffer = new byte[length];
					file.seek(newOffset);
					file.readFully(replaceBuffer, 0, length);
					String replacing = new String(replaceBuffer, 0, length);
					/* ^^^ Debug only ^^^ */
					
					file.seek(newOffset);
					file.write(moveBuffer, 0, length);
				}
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
	
	public static void deleteData(long startPoint, long dataLength, IOSource source) {
		
		long fileLength = source.length();
		
		long offset = startPoint + dataLength;
		
		FileLock lock = null;
		try {
			lock = source.getLock(startPoint + 1, fileLength - (startPoint + 1), false);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(-1);
		}
		
		if (dataLength > 0) {
			byte[] moveBuffer = new byte[100];
			for (long i = offset + 1 ; i < fileLength ; i += moveBuffer.length) {
				
				int length = (int) Math.min(moveBuffer.length, fileLength - i);
				source.seek(i);
				source.buffer.get(moveBuffer, 0, length);
				
				String data = new String(moveBuffer, 0, length);
				
				long newOffset = startPoint + (i - (offset + 1));
				source.seek(newOffset);
				source.buffer.put(moveBuffer, 0, length);
			}
		}
		
		source.unlock(lock);
		
		source.setLength(source.length() - (dataLength + 1));
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

}

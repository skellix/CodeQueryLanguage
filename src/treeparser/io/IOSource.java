package treeparser.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;

public class IOSource implements Closeable {

	public File file = null;
	public ByteBuffer buffer = null;
	private boolean deleteOnClose = false;
	private long filePosition = 0L;
	private long fileLength = 0L;
	
	public IOSource(String fileName) {
		
		file = new File(fileName);
		open();
	}
	
	public IOSource(File file) {
		
		this.file = file;
		open();
	}
	
	public IOSource(ByteBuffer buffer) {
		
		this.buffer = buffer;
	}
	
	public void open() {
		
		if (buffer != null) {
			closeBuffer();
		}
		
		try (RandomAccessFile rFile = new RandomAccessFile(file, "rw")) {
			
			fileLength = rFile.length();
			buffer = rFile.getChannel().map(MapMode.READ_WRITE, 0, Math.min(rFile.length(), Integer.MAX_VALUE));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteOnClose(boolean v) {
		
		deleteOnClose = v;
		file.deleteOnExit();
	}

	@Override
	public void close() throws IOException {

		if (deleteOnClose) {
			
			file.delete();
		}
	}
	
	public long filePosition() {
		
		return filePosition;
	}
	
	public long length() {
		
		return fileLength;
	}
	
	public void seek(long pos) {
		
		if (buffer != null) {
			closeBuffer();
		}
		
		try (RandomAccessFile rFile = new RandomAccessFile(file, "rw")) {
			
			fileLength = rFile.length();
			buffer = rFile.getChannel().map(MapMode.READ_WRITE, pos, Math.min(rFile.length(), Integer.MAX_VALUE));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		filePosition = pos;
	}

	public FileLock getLock(long position, long size, boolean shared) throws IOException {
		
		FileLock lock = null;
		if (buffer != null) {
			closeBuffer();
		}
		
		try {
			
			RandomAccessFile rFile = new RandomAccessFile(file, "rw");
			fileLength = rFile.length();
			lock = rFile.getChannel().lock(position, size, shared);
			buffer = rFile.getChannel().map(MapMode.READ_WRITE, filePosition, Math.min(rFile.length(), Integer.MAX_VALUE));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return lock;
	}

	public void setLength(long length) {
		
		if (buffer != null) {
			closeBuffer();
		}
		
		try (RandomAccessFile rFile = new RandomAccessFile(file, "rw")) {
			
			rFile.getChannel().truncate(length);
			fileLength = length;
			buffer = rFile.getChannel().map(MapMode.READ_WRITE, filePosition, Math.min(rFile.length(), Integer.MAX_VALUE));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeBuffer() {
		try {
			
			Field cleanerField = buffer.getClass().getDeclaredField("cleaner");
			boolean accessible = cleanerField.isAccessible();
			cleanerField.setAccessible(true);
			Object cleaner = cleanerField.get(buffer);
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

	public void unlock(FileLock lock) {
		
		try {
			
			lock.release();
			lock.channel().close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
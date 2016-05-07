package treeparser.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;

public class IOSource implements Closeable, Serializable {

	public File file = null;
	public ByteBuffer buffer = null;
	private boolean deleteOnClose = false;
	private long filePosition = 0L;
	private long length = 0L;
	
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
	
	private void writeObject(ObjectOutputStream out)throws IOException {
		
		boolean isFile = file != null;
		out.writeBoolean(isFile);
		out.writeLong(length);
		
		if (isFile) {
			
			out.writeBoolean(true);
			out.writeObject(file.getAbsolutePath());
			
		} else {
			
			out.writeBoolean(false);
			out.write(buffer.array());
		}
		
		out.writeBoolean(deleteOnClose);
		out.writeLong(filePosition);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		boolean isFile = in.readBoolean();
		length = in.readLong();
		
		if (isFile) {
			
			String fileName = (String) in.readObject();
			file = new File(fileName);
			open();
			
		} else {
			
			byte[] buff = new byte[(int) length];
			
			if (in.read(buff) == length) {
				
				buffer = ByteBuffer.wrap(buff);
				
			} else {
				System.err.println("Buffer length was wrong");
				System.exit(-1);
			}
		}
		
		deleteOnClose = in.readBoolean();
		filePosition = in.readLong();
	}
	
	private void readObjectNoData()throws ObjectStreamException {
		
		//
	}
	
	public void open() {
		
		if (buffer != null) {
			closeBuffer();
		}
		
		try (RandomAccessFile rFile = new RandomAccessFile(file, "rw")) {
			
			length = file.length();
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
		
		return length;
	}
	
	public void seek(long pos) {
		
		if (buffer != null) {
			closeBuffer();
		}
		
		try (RandomAccessFile rFile = new RandomAccessFile(file, "rw")) {
			
			length = rFile.length();
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
			length = rFile.length();
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
			this.length = length;
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
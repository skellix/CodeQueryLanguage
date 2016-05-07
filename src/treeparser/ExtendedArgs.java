package treeparser;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import treeparser.query.QueryNode;
import treeparser.query.QueryParser;

public class ExtendedArgs {
	
	private static File store = null;
	private static String cqlManager = String.format("cql-manager-%s.bin", System.getProperty("user.name"));
	private static HashMap<String, Long> cache = new HashMap<String, Long>();
	private static String id = null;
	private static String osName = System.getProperty("os.name").toLowerCase();

	public static void parseArgs(List<String> args) {
		
		System.out.println(args.toString());
		
		if (args.contains("--id")) {
			
			id = args.get(args.indexOf("--id") + 1);
		}
		
		getOrCreateStore();
		
		TreeNode root = getNodesFromCache();
		
		for (int i = 0 ; i < args.size() ; i ++) {
			
			String arg = args.get(i);
			System.out.println("arg: " + arg);
			if (arg.startsWith("--")) {
				
				if (arg.equals("--id")) {
					
					id = args.get(++ i);
				}
				
			} else if (arg.startsWith("-")) {
				//
			} else if (arg.equals("select")) {
				
				String queryString = args.get(++ i);
				QueryNode query = QueryParser.parse(queryString);
				Collection<TreeNode> children = query.query(root.children, false);
				
				root.children.clear();
				Iterator<TreeNode> iterator = children.iterator();
				
				while (iterator.hasNext()) {
					root.children.add(iterator.next());
				}
				
			} else if (arg.equals("return")) {
				
				String queryString = args.get(++ i);
				QueryNode query = QueryParser.parse(queryString);
				Collection<TreeNode> children = query.query(root.children, false);
				
				TreeNode out = new TreeNode();
				Iterator<TreeNode> iterator = children.iterator();
				while (iterator.hasNext()) {
					out.children.add(iterator.next());
				}
				
				System.out.println(out);
				
			} else if (arg.equals("open")) {
				
				String fileName = args.get(++ i);
				File file = new File(fileName);
				
				if (!file.exists()) {
					System.err.printf("file '%s' not found", file.getAbsolutePath());
					System.exit(-1);
				}
				
				if (file.isDirectory()) {
					
					root = new TreeNode();
					recursiveOpen(root, file);
					
				} else {
					
					root = TreeParser.parse(new File(fileName));
				}
				
			} else if (arg.equals("close")) {
				
				root = new TreeNode();
				
			} else {
				
				root = TreeParser.parse(args.get(++ i));
				
			}
		}
		
		updateCache(root);
	}
	
	private static void recursiveOpen(TreeNode root, File dir) {
		for (String fileName : dir.list()) {
			
			File child = new File(dir, fileName);
			if (child.isDirectory()) {
				
				recursiveOpen(root, child);
				
			} else {
				
				TreeNode node = TreeParser.parse(child);
				for (TreeNode childNode : node.children) {
					
					root.children.add(childNode);
				}
			}
		}
	}

	private static void updateCache(TreeNode root) {
		
		if (!root.hasChildren()) {
			
			store.delete();
			
		} else {
			
			try (FileOutputStream out = new FileOutputStream(store);
					ObjectOutputStream obj = new ObjectOutputStream(out);
					) {
				
				FileLock lock = out.getChannel().lock(0L, store.length(), false);
				obj.writeObject(root);
				lock.release();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static TreeNode getNodesFromCache() {
		
		long length = store.length();
		
		if (store.length() == 0L) {
			
			return new TreeNode();
		}
		
		try (FileInputStream in = new FileInputStream(store);
				ObjectInputStream obj = new ObjectInputStream(in);
				) {
			
			FileLock lock = in.getChannel().lock(0L, store.length(), true);
			
			Object object = null;
			try {
				object = obj.readObject();
			} catch (EOFException e) {
				return new TreeNode();
			}
			
			lock.release();
			
			if (object instanceof TreeNode) {
				return (TreeNode) object;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private static void getOrCreateStore() {
		
		cleanCache();
		if (id != null) {
			
			long ppid = Long.parseLong(id);
			System.out.printf("id = %d\n", ppid);
			
			for (String fileName : cache.keySet().toArray(new String[0])) {
				
				System.out.printf("cache item: %s = %d\n", fileName, cache.get(fileName));
				
				if (cache.get(fileName) == ppid) {
					
					store = new File(fileName);
					return;
				}
			}
			
			System.out.println("Cache file not found creating new cache file");
			
			try {
				store = File.createTempFile("cql-", ".bin");
			} catch (IOException e) {
				e.printStackTrace();
			}
			addToCache(store.getAbsolutePath(), ppid);
			
		} else {
			
			if (osName.contains("nix")) {
				
				File stat = new File("/proc/self/stat");
				
				try (Scanner scanner = new Scanner(stat)) {
					
					long pid = scanner.nextLong();
					String cmd = scanner.next("\\([^\\)]*\\)");
					String state = scanner.next("[^\\s]+");
					long ppid = scanner.nextLong();
					
					for (String fileName : cache.keySet().toArray(new String[0])) {
						
						if (cache.get(fileName) == ppid) {
							
							store = new File(fileName);
							return;
						}
					}
					
					store = File.createTempFile("cql-", ".bin");
					addToCache(store.getAbsolutePath(), ppid);
					System.out.println("");
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(-1);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				
			} else if (osName.contains("win")) {
				
				System.err.println("A session id must be used on windows: use the '-id' option");
				System.exit(-1);
			}
		}
	}

	private static void addToCache(String absolutePath, long pid) {
		
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File cacheManager = new File(tempDir, cqlManager);
		
		if (cacheManager.exists()) {
			
			HashMap<String, Long> cache = null;
			try (
					FileInputStream in = new FileInputStream(cacheManager);
					ObjectInputStream obj = new ObjectInputStream(in);
					) {
				
				FileLock lock = in.getChannel().lock(0L, cacheManager.length(), true);
				
				Object object = obj.readObject();
				
				if (!(object instanceof HashMap<?, ?>)) {
					
					System.err.println("ERROR: Cache is corrupted!");
					System.exit(-1);
				}
				
				cache = (HashMap<String, Long>) object;
				lock.release();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			cache.put(absolutePath, pid);
			
			try (
					FileOutputStream out = new FileOutputStream(cacheManager);
					ObjectOutputStream obj = new ObjectOutputStream(out);
					) {
				
				FileLock lock = out.getChannel().lock(0L, 1024, false);
				obj.writeObject(cache);
				lock.release();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else {
			try {
				cacheManager.createNewFile();
				
				HashMap<String, Long> cache = new HashMap<String, Long>();
				cache.put(absolutePath, pid);
				
				try (
						FileOutputStream out = new FileOutputStream(cacheManager);
						ObjectOutputStream obj = new ObjectOutputStream(out);
						) {
					
					FileLock lock = out.getChannel().lock(0L, 1024, false);
					obj.writeObject(cache);
					lock.release();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private static void cleanCache() {
		
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File cacheManager = new File(tempDir, cqlManager);
		
		if (cacheManager.exists()) {
			
			try (
					FileInputStream in = new FileInputStream(cacheManager);
					ObjectInputStream obj = new ObjectInputStream(in);
					) {
				
				FileLock lock = in.getChannel().lock(0L, cacheManager.length(), true);
				Object object = obj.readObject();
				
				if (!(object instanceof HashMap<?, ?>)) {
					
					System.err.println("ERROR: Cache is corrupted!");
					System.exit(-1);
				}
				
				HashMap<String, Long> cache = (HashMap<String, Long>) object;
				ExtendedArgs.cache = cache;
				
				for (String cacheFile : cache.keySet().toArray(new String[0])) {
					
					final String fileName = cacheFile;
					File file = new File(fileName);
					if (!file.exists()) {
						
						cache.remove(fileName);
						
					} else {
						
						final Long pid = cache.get(cacheFile);
						if (osName.contains("win")) {
							
							// do nothing
							
						} else if (osName.contains("nix")) {
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									
									try {
										
										Process p = Runtime.getRuntime().exec(String.format("bash -c ps -aux %d", pid));
										p.waitFor();
										Scanner scanner = new Scanner(p.getInputStream());
										
										if (scanner.hasNextLine()) {
											String line = scanner.nextLine();
											if (!scanner.hasNextLine()) {
												System.out.printf("process not found %d cleaning file %s\n", pid, fileName);
												new File(fileName).delete();
											}
										}
										scanner.close();
									} catch (IOException e) {
										e.printStackTrace();
										System.exit(-1);
									} catch (InterruptedException e) {
										e.printStackTrace();
										System.exit(-1);
									}
									
								}
							}).start();
						}
					}
				}
				
				lock.release();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
	}

}

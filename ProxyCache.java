import java.util.*;
import java.util.concurrent.*; // For ConcurrentHashMap
import java.io.*;
import java.security.*; // For MD5 hexdigest

public class ProxyCache {

  /**
   * Constants
   */
  private static final String CACHE_PATH = ".proxy-cache";

  /**
   * Properties
   */
  private ConcurrentHashMap<String, CachedContent> cacheMap;
  private ConcurrentHashMap<String, String> md5Map;

  /**
   * Constructor
   */
  public ProxyCache() {
    // Check cache directory's existence
    File f = new File(CACHE_PATH);
    if (!f.exists() || !f.isDirectory()) {
      try {
        if (!f.isDirectory()) f.delete();
        f.mkdir();
      } catch (SecurityException e) {
        e.printStackTrace();
      }
    }
    // Initialize concurrent hashmap
    this.cacheMap = new ConcurrentHashMap<String, CachedContent>();
    this.md5Map = new ConcurrentHashMap<String, String>();
  }

  private String pathFromURI(String URI) {
    return String.format("%s/%s", CACHE_PATH, MD5(URI));
  }

  /**
   * Get a md5 hash from an URI
   * @param input {String} The string to get a hash of
   * @return {String} The md5 hash of the string
   */
  private String MD5(String input) {
    if (this.md5Map.containsKey(input)) {
      return this.md5Map.get(input);
    }
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(input.getBytes());
      byte[] digest = md.digest();
      StringBuffer buf = new StringBuffer();
      for (byte b : digest) {
        buf.append(String.format("%02x", b & 0xff));
      }
      String md5Hash = buf.toString();
      this.md5Map.put(input, md5Hash);
      return md5Hash;
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Check if the URI has been cached
   */
  public boolean contains(String URI) throws IOException {
    File f = new File(pathFromURI(URI));
    return f.exists();
  }

  public OutputStream getOutputStream(String URI) throws IOException {
    return getContentFromURI(URI).getOutputStream();
  }

  public InputStream getInputStream(String URI) throws IOException {
    return getContentFromURI(URI).getInputStream();
  }

  private CachedContent getContentFromURI(String URI) throws IOException {
    CachedContent c;
    if (!this.cacheMap.containsKey(URI)) {
      File f = new File(pathFromURI(URI));
      c = new CachedContent(f);
      this.cacheMap.put(URI, c);
    } else {
      c = this.cacheMap.get(URI);
    }
    return c;
  }

  private class CachedContent {
    private File cacheFile;

    public CachedContent(File f) {
      this.cacheFile = f;
    }

    public OutputStream getOutputStream() throws IOException {
      return new FileOutputStream(this.cacheFile);
    }

    public InputStream getInputStream() throws IOException {
      return new FileInputStream(this.cacheFile);
    }

  }

}

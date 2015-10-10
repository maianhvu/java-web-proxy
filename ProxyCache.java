import java.util.*;
import java.util.concurrent.*; // For ConcurrentHashMap
import java.io.*;
import java.net.*;
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
    if (this.cacheMap.containsKey(URI)) return true;
    CachedContent content = new CachedContent(URI);
    // If hash collision or file doesn't exist
    if (!content.isValid()) return false;
    // Put content to cache
    this.cacheMap.put(URI, content);
    return true;
  }

  /**
   * Get the CachedContent object from the URI
   */
  public CachedContent getFromURI(String URI) throws IOException {
    if (!contains(URI)) {
      return null;
    }
    return this.cacheMap.get(URI);
  }

  public CachedContent create(String URI) throws IOException {
    return new CachedContent(URI, new Date());
  }

  public class CachedContent {
    private String uri;
    private Date retrieved;
    private File file;

    private BufferedInputStream fromCache;
    private BufferedOutputStream toCache;

    private boolean valid;

    /**
     * Constructor.
     * Creates a cached content from scratch, begin to write to
     */
    public CachedContent(String uri, Date retrieved) throws IOException {
      // Set params that are passed in first
      this.uri = uri;
      this.retrieved = retrieved;
      // Set file
      this.file = new File(pathFromURI(uri));
      // Write metadata:
      // <first line>: URI
      // <second line>: Date (in number of millis)
      // <third line onwards>: Actual response data
      StringBuilder sb = new StringBuilder();
      sb.append(uri).append("\r\n").append(retrieved.getTime()).append("\r\n");
      byte[] b = sb.toString().getBytes();
      this.toCache = new BufferedOutputStream(new FileOutputStream(this.file));
      this.toCache.write(b, 0, b.length);
      // Content created from scratch should be valid
      this.valid = true;
    }

    public BufferedOutputStream getOutputStream() {
      return this.toCache;
    }

    /**
     * Constructor.
     * Creates content from cache
     */
    public CachedContent(String uri) throws IOException {
      this.uri = uri;
      this.file = new File(pathFromURI(uri));
      if (!file.exists() || file.isDirectory()) { this.valid = false; return; }
      BufferedInputStream queryStream = new BufferedInputStream(new FileInputStream(file));
      byte[] b = new byte[8192];
      int len = queryStream.read(b);
      if (len <= 0) { this.valid = false; return; }
      int eom; // End of metadata
      for (eom=0;eom<len && (b[eom] != '\r' || !(new String(b, eom, 2)).equals("\r\n")); eom++);
      String uriFromMeta = new String(b, 0, eom);
      if (!uriFromMeta.equals(uri)) {
        // Hash collision
        this.valid = false;
        return;
      }
      int sosl = eom + 2; // Start of second line
      for (eom=sosl;eom<len &&(b[eom] != '\r' || !(new String(b, eom, 2)).equals("\r\n")); eom++);
      this.retrieved = new Date(Long.parseLong(new String(b, sosl, eom - sosl)));
      eom += 2;
      queryStream.close();

      try {
        Request req = Request.ifModifiedSince(this.uri, this.retrieved);
        Socket remoteSocket = req.createSocket();
        BufferedOutputStream toRemote = new BufferedOutputStream(remoteSocket.getOutputStream());
        req.fire(toRemote);
        BufferedInputStream fromRemote = new BufferedInputStream(remoteSocket.getInputStream());
        b = new byte[256]; // Hopefully 256 is sufficient to read just the statusCode
        len = fromRemote.read(b);
        if (len > 0) {
          // Find response header
          int eofl; // End of header's first line
          for (eofl=0;eofl<len && (b[eofl] != '\r' || (new String(b, eofl, 2)).equals("\r\n")); eofl++);
          String statusCode = new String(b, 0, eofl).split("\\s+", 3)[1];
          // If statusCode is 304 means content is not modified
          this.valid = statusCode.equals("304");
        }
        fromRemote.close();
        toRemote.close();
        remoteSocket.close();
      } catch (IOException e) {
        System.out.println("Error checking for modifications. Assuming cached content is fresh.");
        e.printStackTrace();
      } finally {
        if (!this.valid) return;
        this.fromCache = new BufferedInputStream(new FileInputStream(file));
        this.fromCache.skip(eom); // Skip metadata

        this.valid = true;
      }
    }

    public BufferedInputStream getInputStream() {
      return this.fromCache;
    }

    /**
     * @return if the cached content is valid
     */
    public boolean isValid() { return this.valid; }
  }
}

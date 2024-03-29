import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*; // For Pattern and Matcher
import java.text.*; // For SimpleDateFormat

public class Request {

  /**
   * Types
   */
  public static enum Field {
    METHOD       ("Method"),
    URI          ("URI"),
    HTTP_VERSION ("Http-Version"),
    HOST         ("Host"),
    HOST_ADDRESS ("Host-Address"),
    PORT         ("Port"),
    IF_MODIFIED_SINCE("If-Modified-Since");

    public final String key;
    Field(String k) {
      this.key = k;
    }
  }

  /**
   * Constants
   */
  private static final int DEFAULT_PORT = 80;
  private static final String URI_PATTERN = "^(?:http(?:s)?://)?([^/]+)(:\\d+)?(?:.+)?$";

  /**
   * Properties
   */
  private boolean valid;
  private byte[] rawData;
  private int length;

  private LinkedHashMap<String, String> fieldsMap;

  /**
   * Constructor
   * @param inputStream The stream to read the request data from
   */
  public Request(BufferedInputStream inputStream) throws IOException {
    byte[] b = new byte[8192];
    int len = inputStream.read(b);

    // Reject empty requests
    if (len <= 0) { this.valid = false; return; }

    String[] lines = (new String(b, 0, len)).split("\r\n");

    // Split first line into <method> <uri> <httpVersion>
    String[] params = lines[0].split("\\s+",3);

    // Invalid params -> invalid request
    if (params.length != 3) { this.valid = false; return; }

    String method      = params[0].toUpperCase();
    String uri         = params[1];
    String httpVersion = params[2].toUpperCase();

    // Only HTTP/1.0 allowed
    if (!httpVersion.equals("HTTP/1.0")) { this.valid = false; return; }

    // Initialize fields map
    this.fieldsMap = new LinkedHashMap<>();

    // Put in parsed params
    set(Field.METHOD, method);
    set(Field.URI   , uri);
    set(Field.HTTP_VERSION, httpVersion);

    // Read the rest of the request
    for (int i = 1; i < lines.length; i++) {
      String[] parts = lines[i].split(":\\s+",2);
      if (parts.length != 2) continue;
      this.fieldsMap.put(parts[0].trim(), parts[1].trim());
    }

    // Find out the host address and the port from the Host: field
    String host;
    if ((host = get(Field.HOST)) != null) {
      String[] parts = host.split(":", 2);
      set(Field.HOST_ADDRESS, parts[0].trim());
      // If port exists inside host, set to that port
      if (parts.length >= 2) {
        set(Field.PORT, parts[1].trim());
      }
      // Else use default port
      else {
        set(Field.PORT, Integer.toString(DEFAULT_PORT));
      }
    }
    // Find out host address from URI
    else {
      Pattern uriPattern = Pattern.compile(URI_PATTERN);
      Matcher m = uriPattern.matcher(uri);
      // Check for matches
      if (m.matches()) {
        set(Field.HOST_ADDRESS, m.group(1).trim());
        // Same as above, set port if exists, else set to default port
        if (m.group(2) != null) {
          set(Field.PORT, m.group(2).trim());
        } else {
          set(Field.PORT, Integer.toString(DEFAULT_PORT));
        }
      }
      // Else mark as invalid
      else {
        this.valid = false;
        return;
      }
    }

    // Set raw data and length
    this.rawData = b;
    this.length = len;

    // Set validity
    this.valid = true;
  }

  /**
   * Constructing an If-Modified-Since request
   */
  private Request() { this.fieldsMap = new LinkedHashMap<>(); }

  public static Request ifModifiedSince(String uri, Date date) {
    // Set defaults for this constructor
    final String method = "GET";
    final String httpVersion = "HTTP/1.0";
    // Prepare date formatter into server-acceptable date formats
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    // Use regex to parse the URI
    Pattern uriPattern = Pattern.compile(URI_PATTERN);
    Matcher m = uriPattern.matcher(uri);

    // Initialize request
    Request req = new Request();

    if (m.matches()) {
      // Start building request.
      StringBuilder sb = new StringBuilder();
      sb.append(method).append(" ").append(uri).append(" ").append(httpVersion).append("\r\n");

      String host = m.group(1);
      String port = Integer.toString(DEFAULT_PORT);
      if (m.group(2) != null) {
         // Append port to Host record, if exists
         // If there is none, just omit
        port = m.group(2);
        host += ":" + port;
      }
      // Host field
      sb.append(Field.HOST.key).append(": ").append(host).append("\r\n");
      // If modified since field
      sb.append(Field.IF_MODIFIED_SINCE.key).append(": ");
      sb.append(dateFormat.format(date)).append("\r\n\r\n");

      // Set raw data
      byte[] data = sb.toString().getBytes();
      req.rawData = data;
      req.length  = data.length;

      // Set appropriate fields
      req.set(Field.METHOD, method);
      req.set(Field.URI, uri);
      req.set(Field.HTTP_VERSION, httpVersion);

      req.set(Field.HOST, host);
      req.set(Field.HOST_ADDRESS, m.group(1));
      req.set(Field.PORT, port);

      req.valid = true;
      return req;
    } else {
      req.valid = false;
      return req;
    }
  }

  /**
   * Check if the initialized request is valid
   */
  public boolean isValid() {
    return this.valid;
  }

  /**
   * Set the value for a field inside fieldsMap
   */
  public void set(Field field, String value) {
    this.fieldsMap.put(field.key, value);
  }

  /**
   * Convenient getter for fieldsMap
   */
  public String get(Field field) {
    if (!this.fieldsMap.containsKey(field.key)) return null;
    return this.fieldsMap.get(field.key);
  }

  public Socket createSocket() throws IOException {
    String hostAddr = get(Field.HOST_ADDRESS);
    if (!this.isValid() || hostAddr == null) return null;
    int port = DEFAULT_PORT;
    try {
      port = Integer.parseInt(get(Field.PORT));
    } catch (Exception e) {
      e.printStackTrace();
      port = DEFAULT_PORT;
    } finally {
      return new Socket(hostAddr, port);
    }
  }

  /**
   * Fire this request to the destination BufferedWriter
   */
  public void fire(BufferedOutputStream dest) throws IOException {
    dest.write(this.rawData, 0, this.length);
    dest.flush();
  }

  /**
   * For debugging purposes
   */
  public String getFields() {
    StringBuilder sb = new StringBuilder();
    for (String k : this.fieldsMap.keySet()) {
      if (sb.length() != 0) { sb.append("\n"); }
      sb.append(k).append(": ").append(this.fieldsMap.get(k));
    }
    return sb.toString();
  }

  public String toString() {
    return new String(this.rawData,0,this.length);
  }
}

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Request {

  /**
   * Types
   */
  public enum Field {
    METHOD       ("Method"),
    URI          ("URI"),
    HTTP_VERSION ("Http-Version"),
    HOST         ("Host"),
    HOST_ADDRESS ("Host-Address"),
    PORT         ("Port");

    private String key;
    Field(String k) {
      this.key = k;
    }
    public String getKey() { return this.key; }
  }

  /**
   * Constants
   */
  private static final int DEFAULT_PORT = 80;
  private static final String URI_PATTERN = "^(?:http(?:s)?://)([^/]+)(:\\d+)?(?:.+)?$";

  /**
   * Properties
   */
  private boolean valid;
  private byte[] rawData;
  private int length;

  private LinkedHashMap<String, String> fieldsMap;

  /**
   * Constructor
   * @param inputStream
   */
  public Request(InputStream inputStream) throws IOException {
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

    this.valid = true;
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
    this.fieldsMap.put(field.getKey(), value);
  }

  /**
   * Convenient getter for fieldsMap
   */
  public String get(Field field) {
    if (!this.fieldsMap.containsKey(field.getKey())) return null;
    return this.fieldsMap.get(field.getKey());
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String k : this.fieldsMap.keySet()) {
      if (sb.length() != 0) { sb.append("\r\n"); }
      sb.append(k).append(": ").append(this.fieldsMap.get(k));
    }
    sb.append("\r\n");
    return sb.toString();
  }
}

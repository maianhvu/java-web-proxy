import java.io.*;
import java.text.*;
import java.util.*;

public class Response {

  /**
   * Properties
   */
  private BufferedInputStream dataSource;
  private CensorEngine censorEngine;

  // Private constructor
  private Response(BufferedInputStream source) {
    this.dataSource = source;
    this.censorEngine = null;
  }

  /**
   * Static Constructor.
   * Alias for private constructor, just for better readability
   */
  public static Response read(BufferedInputStream source) {
    return new Response(source);
  }

  public static Response createBadGateway() {
    // Prepare date formatter into server-acceptable date formats
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    // Begin request data
    StringBuilder sb = new StringBuilder();
    sb.append("HTTP/1.0 502 Bad Gateway\r\n");
    sb.append("Date: ").append(dateFormat.format(new Date())).append("\r\n");
    sb.append("Content-Length: 134\r\n");
    sb.append("Connection: close\r\n\r\n");

    sb.append("<!doctype html><html><head><meta charset='UTF-8'><title>502 - Bad Gateway</title>");
    sb.append("</head><body><h1>502 - Bad Gateway</h1></body></html>");

    return new Response(new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes())));
  }

  /**
   * Supplying the censoring engine
   */
  public void setCensorEngine(CensorEngine engine) {
    this.censorEngine = engine;
  }

  /**
   * Start forwarding request from the data source initialized to
   * Accept multiple output streams
   */
  public void forward(BufferedOutputStream... dests) throws IOException {
    byte[] b = new byte[8192];
    int len;

    // Read header first. This helps to extract out header info,
    // as well as preventing the censor engine from censoring header
    if ((len = this.dataSource.read(b)) > 0) {
      // Iterate forward to find end of header
      int eoh;
      for (eoh = 0; eoh < len && (b[eoh] != '\r' || !(new String(b, eoh, 4)).equals("\r\n\r\n")); eoh++);
      String header  = new String(b, 0, eoh);

      // Only censor if type is text
      boolean censor = this.censorEngine != null && header.indexOf("Content-Type: text") != -1;
      if (censor) {
        b = this.censorEngine.process(b, eoh + 4, -1);
        len = b.length;
      }

      // Write initial data
      writeBytes(b, len, dests);

      // Forward data
      while ((len = this.dataSource.read(b)) > 0) {
        // Write to all non-null streams
        if (censor) {
          b = this.censorEngine.process(b);
          len = b.length;
        }
        writeBytes(b, len, dests);
      }

      // Flush all streams
      flushAll(dests);
    }
  }

  /**
   * Private convenient method to write data to all streams
   */
  private static void writeBytes(byte[] data, int length, BufferedOutputStream[] dests) throws IOException {
    for (BufferedOutputStream dest : dests) {
      if (dest != null) dest.write(data, 0, length);
    }
  }

  /**
   * Private convenient method to flush all treams
   */
  private static void flushAll(BufferedOutputStream[] dests) throws IOException {
    for (BufferedOutputStream dest : dests) if (dest != null) {
      dest.flush();
    }
  }
}

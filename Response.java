import java.io.*;

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

      // Split first line into <httpVersion> <statusCode> <statusName>
      String[] params = header.substring(0, header.indexOf("\r\n")).split("\\s+", 3);
      String statusCode  = params[1];
      // TODO: Process statusCode

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

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
    // Keep track if the censorship engine has already read through the header
    // We don't want to replace any characters inside the header
    boolean endOfHeader = false;
    // Start reading from the source
    while ((len = this.dataSource.read(b)) > 0) {
      if (this.censorEngine != null) {
        // Censor
        CensorEngine.Result result = this.censorEngine.process(b, endOfHeader);
        b = result.data;
        len = result.length;
        endOfHeader = result.endOfHeader;
      }
      // Write to all destinations
      for (BufferedOutputStream dest : dests) {
        if (dest != null) {
          dest.write(b, 0, len);
        }
      }
    }
    // Flush all destination streams
    for (BufferedOutputStream dest : dests) {
      if (dest != null) {
        dest.flush();
      }
    }
  }
}

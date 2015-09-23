import java.io.*;

public class Response {

  private BufferedInputStream dataSource;

  // Private constructor
  private Response(BufferedInputStream source) {
    this.dataSource = source;
  }

  /**
   * Static Constructor.
   * Alias for private constructor, just for better readability
   */
  public static Response read(BufferedInputStream source) {
    return new Response(source);
  }

  /**
   * Start forwarding request from the data source initialized to
   * Accept multiple output streams
   */
  public void forward(BufferedOutputStream... dests) throws IOException {
    byte[] b = new byte[8192];
    int len;
    // Start reading from the source
    while ((len = this.dataSource.read(b)) > 0) {
      // Write to all destinations
      for (BufferedOutputStream dest : dests) {
        dest.write(b, 0, len);
      }
    }
    // Flush all destination streams
    for (BufferedOutputStream dest : dests) {
      dest.flush();
    }
  }
}

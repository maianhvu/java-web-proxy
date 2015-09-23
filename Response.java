import java.io.*;

public class Response {

  private InputStream dataSource;

  // Private constructor
  private Response(InputStream inputStream) {
    this.dataSource = inputStream;
  }

  /**
   * Static Constructor.
   * Alias for private constructor, just for better readability
   */
  public static Response read(InputStream stream) {
    return new Response(stream);
  }

  /**
   * Start forwarding request from the data source initialized to
   * Accept multiple output streams
   */
  public void forward(OutputStream... dests) throws IOException {
    byte[] b = new byte[8192];
    int len;
    while ((len = this.dataSource.read(b)) > 0) {
      for (OutputStream dest : dests) {
        dest.write(b, 0, len);
      }
    }
  }
}

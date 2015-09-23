import java.io.*;

public class Response {

  private InputStream dataSource;

  private Response(InputStream inputStream) {
    this.dataSource = inputStream;
  }

  public static Response read(InputStream stream) {
    return new Response(stream);
  }

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

import java.io.*;

public class CachedContent {
  private File cacheFile;

  public CachedContent(File f) {
    this.cacheFile = f;
  }

  public BufferedOutputStream getOutputStream() throws IOException {
    return new BufferedOutputStream(new FileOutputStream(this.cacheFile));
  }

  public BufferedInputStream getInputStream() throws IOException {
    return new BufferedInputStream(new FileInputStream(this.cacheFile));
  }

}

import java.io.*;
import java.util.*;

public class CensorEngine {

  /**
   * Constants
   */
  private static final double GROW_FACTOR = 2.0; // Describes how fast the temp array resize

  /**
   * Properties
   */
  private HashSet<String> wordsSet;

  /**
   * Seed the engine with the words to be censored
   * @param censorFile The path to the file to be censored
   */
  public static CensorEngine seed(String censorFile) {
    CensorEngine engine = new CensorEngine();
    try {
      File f = new File(censorFile);
      // If censor file doesn't exist or is directory
      if (!f.exists() || f.isDirectory()) {
        // Return empty engine
        return engine;
      }
      // Populate engine's HashSet with the lines inside
      BufferedReader fromCensorFile = new BufferedReader(new InputStreamReader(
            new FileInputStream(f)));
      String line;
      while ((line = fromCensorFile.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;
        engine.wordsSet.add(line.toLowerCase());
      }

      fromCensorFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      return engine;
    }
  }

  /**
   * Private constructor.
   * Initializes the words set
   */
  private CensorEngine() {
    this.wordsSet = new HashSet<>();
  }

  /**
   * Process the input, output a new byte array with the censored text
   */
  public Result process(byte[] input, int bufferSize, boolean endOfHeader) {
    // Temporary array to store the processed output
    byte[] tmp = new byte[bufferSize];

    // Censor character
    byte censor = (byte)'-';
    // Which is 3 chars long (---)
    int censorLength = 3;

    // Current pointer to tmp array
    int cursor = 0;

    // Current pointer to input array
    int i = 0;

    while (i < input.length) {
      // Check for end of header
      if (!endOfHeader && i < input.length - 3 && input[i] == '\r') {
        if ((new String(input, i, 4)).equals("\r\n\r\n")) {
          endOfHeader = true;
        }
      }
      // If is blank space or not yet end of header, copy as is
      if (!endOfHeader || !isWordChar(input[i])) {
        tmp[cursor++] = input[i++];
        if (cursor >= tmp.length) tmp = resize(tmp);
        continue;
      }

      // If is word character
      int start = i;
      int cursorStart = cursor;
      // Expect word to not be censored first, copy as is
      while (isWordChar(input[i]) && i < input.length - 1) {
        tmp[cursor++] = input[i++];
        if (cursor >= tmp.length) tmp = resize(tmp);
      }

      // If word needs to be censored, go back and censor
      String word = new String(input, start, i - start).toLowerCase();
      if (this.wordsSet.contains(word)) {
        cursor = cursorStart + censorLength;
        if (cursor >= tmp.length) tmp = resize(tmp);
        for (int j = cursorStart; j < cursorStart + censorLength; j++) {
          tmp[j] = censor;
        }
      }
    }

    // Return result
    return new Result(tmp, cursor, endOfHeader);
  }

  public Result process(byte[] input, boolean endOfHeader) {
    return process(input, input.length, endOfHeader);
  }

  /**
   * Doubles the size of an array
   */
  private byte[] resize(byte[] b) {
    byte[] tmp = new byte[(int) Math.round(b.length * GROW_FACTOR)];
    System.arraycopy(b, 0, tmp, 0, b.length);
    return tmp;
  }

  public class Result {
    public final byte[] data;
    public final int length;
    public final boolean endOfHeader;

    public Result(byte[] b, int len, boolean eoh) {
      this.data = b;
      this.length = len;
      this.endOfHeader = eoh;
    }
  }

  /**
   * Private method to check if a byte represents a word character
   */
  private static boolean isWordChar(byte c) {
    return (c >= 65 && c <= 90) || (c >= 97 && c <= 122);
  }
}

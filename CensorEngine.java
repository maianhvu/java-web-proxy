import java.io.*;
import java.util.*;

public class CensorEngine {

  /**
   * Constants
   */
  private static final double GROW_FACTOR = 1.5; // Describes how fast the temp array resize

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
  public byte[] process(byte[] input, int offset, int length) {
    // If offset is beyond length, return original
    if (offset >= input.length) return input;

    // Temporary array to store the processed output
    byte[] tmp = new byte[input.length];

    // Censor character
    byte censor = (byte)'-';
    // Which is 3 chars long (---)
    int censorLength = 3;

    // Copy uncensored part at first
    System.arraycopy(input, 0, tmp, 0, offset);

    // Current pointer to tmp array
    int cursor = offset;

    // Current pointer to input array
    int i = offset;

    // Find out end position
    int end;
    if (length == -1) {
      // Censor till the end
      end = input.length;
    } else {
      end = offset + length;
      if (end > input.length) end = input.length;
    }

    while (i < end && input[i] > 0) {
      // If is blank space or not yet end of header, copy as is
      if (!isWordChar(input[i])) {
        tmp[cursor++] = input[i++];
        if (cursor >= tmp.length) tmp = extend(tmp);
        continue;
      }

      // If is word character
      int start = i;
      int cursorStart = cursor;
      // Expect word to not be censored first, copy as is
      while (isWordChar(input[i]) && i < end - 1) {
        tmp[cursor++] = input[i++];
        if (cursor >= tmp.length) tmp = extend(tmp);
      }

      // If word needs to be censored, go back and censor
      String word = new String(input, start, i - start).toLowerCase();
      if (this.wordsSet.contains(word)) {
        cursor = cursorStart + censorLength;
        if (cursor >= tmp.length) tmp = extend(tmp);
        for (int j = cursorStart; j < cursorStart + censorLength; j++) {
          tmp[j] = censor;
        }
      }
    }

    // Resize to appropriate length
    tmp = resize(tmp, cursor + (input.length - i));

    // Copy end part which is not censored
    if (end < input.length) {
      System.arraycopy(input, i, tmp, cursor, input.length - i);
    }

    // Return result
    return tmp;
  }

  /**
   * Convenient method to censor the entire array
   */
  public byte[] process(byte[] input) {
    return process(input, 0, input.length);
  }

  /**
   * Doubles the size of an array
   */
  private byte[] extend(byte[] b) {
    byte[] tmp = new byte[(int) Math.round(b.length * GROW_FACTOR)];
    System.arraycopy(b, 0, tmp, 0, b.length);
    return tmp;
  }

  private byte[] resize(byte[] b, int length) {
    byte[] tmp = new byte[length];
    if (length > b.length) length = b.length;
    System.arraycopy(b, 0, tmp, 0, length);
    return tmp;
  }

  /**
   * Private method to check if a byte represents a word character
   */
  private static boolean isWordChar(byte c) {
    return (c >= 65 && c <= 90) || (c >= 97 && c <= 122);
  }

  private static void printByteArray(byte[] b, int len) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < len; i++) {
      if (i != 0) sb.append(", ");
      sb.append(b[i]);
    }
    sb.append("]");
    System.out.println(sb);
  }
}

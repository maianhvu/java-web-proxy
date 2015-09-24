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
    // Prepare censoring material
    byte[] censor = "---".getBytes();
    // Create temporary array
    byte[] tmp = new byte[input.length];
    // Copy offset
    System.arraycopy(input, 0, tmp, 0, offset);
    // Set cursors
    int tmp_c = offset;
    int input_c = offset;
    // Set endpoint
    int end;
    if (length < 0) {
      // Reads till the end
      end = input.length - 1;
    } else {
      end = offset + length;
      if (end > input.length) end = input.length;
    }
    // Loop and censor
    while (input_c < end) {
      // If is not word char, copy exactly
      while (input_c < end && !isWordChar(input[input_c])) {
        tmp[tmp_c++] = input[input_c++];
        if (tmp_c >= tmp.length) tmp = extend(tmp);
      }
      // If encounter a word character, start keeping track
      int wordStart = tmp_c;
      // Assume that word is not censored, keep moving forward
      // and copy exactly
      while (input_c < end && isWordChar(input[input_c])) {
        tmp[tmp_c++] = input[input_c++];
        if (tmp_c >= tmp.length) tmp = extend(tmp);
      }
      if (wordStart + censor.length > tmp.length) tmp = extend(tmp);
      // Start forming word
      String word = new String(tmp, wordStart, tmp_c - wordStart).toLowerCase();
      // Censor if exists
      if (this.wordsSet.contains(word)) {
        for (int i = 0; i < censor.length; i++) {
          tmp[wordStart+i] = censor[i];
        }
        // Set cursor
        tmp_c = wordStart + censor.length;
      }
    }
    // Resize
    tmp = resize(tmp, tmp_c + input.length - input_c);
    // Copy residual bytes
    if (length > 0) {
      System.arraycopy(input, offset + length, tmp, tmp_c, input.length - input_c);
    }
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

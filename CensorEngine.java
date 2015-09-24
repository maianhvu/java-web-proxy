import java.io.*;
import java.util.*;

public class CensorEngine {

  /**
   * Constants
   */
  private static final String CENSOR = "---";

  /**
   * Properties
   */
  private String replaceRegex;

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
      String line = fromCensorFile.readLine();
      if (line == null) {
        return engine;
      }
      StringBuilder sb = new StringBuilder();
      sb.append("(?i)(?:").append(line.trim().toLowerCase());
      while ((line = fromCensorFile.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;
        sb.append("|").append(line.toLowerCase());
      }
      sb.append(")");
      engine.replaceRegex = sb.toString();
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
  private CensorEngine() { this.replaceRegex = null; }

  /**
   * Process the input, output a new byte array with the censored text
   */
  public String process(String input) {
    if (this.replaceRegex == null) return input;
    return input.replaceAll(this.replaceRegex, CENSOR);
  }
}

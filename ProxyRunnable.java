import java.io.*;
import java.net.*;

public class ProxyRunnable implements Runnable {

  /**
   * Properties
   */
  private Socket clientSocket;

  /**
   * Constructor
   */
  public ProxyRunnable(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  public void run() {
    try {
      // Read request from client
      InputStream fromClient = this.clientSocket.getInputStream();
      Request request = new Request(fromClient);

      if (!request.isValid()) {
        System.out.println("Request invalid!");
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      System.out.println(request);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        this.clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

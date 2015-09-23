import java.io.*;
import java.net.*;

public class WebProxy implements Runnable {

  /**
   * Properties
   */
  private int port;
  private boolean stopped;
  private ServerSocket welcomeSocket;
  private ProxyCache cache;

  /**
   * Constructor
   * @param port The port to listen for connections
   */
  public WebProxy(int port) {
    this.port = port;
    this.stopped = true;
    this.cache = new ProxyCache();
  }

  /**
   * Implementation of Runnable
   */
  public void run() {
    try {
      // Start listening on the port specified
      this.welcomeSocket = new ServerSocket(this.port);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Mark server as running
    this.stopped = false;
    // Keep listening for client connections
    // Spawn new proxy thread upon handshake
    while (!this.isStopped()) {
      Socket clientSocket = null;
      try {
        clientSocket = welcomeSocket.accept();
      } catch (IOException e) {
        e.printStackTrace();
      }
      (new Thread(new ProxyRunnable(clientSocket,
                                    cache
                                    ))).start();
    }
  }

  public boolean isStopped() { return this.stopped; }

  public synchronized void stop() {
    this.stopped = true;
    // Try to close the server socket if it is initialized
    if (this.welcomeSocket != null) {
      try {
        this.welcomeSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Main executable method
   */
  public static void main(String[] args) {
    int port = 8080;
    WebProxy proxy = new WebProxy(port);
    (new Thread(proxy)).start();
  }
}

import java.io.*;
import java.net.*;

public class ProxyRunnable implements Runnable {

  /**
   * Properties
   */
  private Socket clientSocket;
  private ProxyCache cache;

  /**
   * Constructor
   */
  public ProxyRunnable(Socket clientSocket, ProxyCache cache) {
    this.clientSocket = clientSocket;
    this.cache = cache;
  }

  public void run() {
    try {
      // Read request from client
      InputStream fromClient = this.clientSocket.getInputStream();
      Request request = new Request(fromClient);

      // Check request's validity
      if (!request.isValid()) {
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      String uri = null;
      OutputStream toClient = this.clientSocket.getOutputStream();
      // Checks for cached item
      if (this.cache.contains(uri = request.get(Request.Field.URI))) {
        // Reads from cache
        InputStream fromCache = this.cache.getInputStream(uri);
        Response response = Response.read(fromCache);
        response.forward(toClient);

        // Close all streams and exit
        toClient.close();
        fromCache.close();
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // If cached item doesn't exist
      // Create a socket that connects to the remote server
      Socket remoteSocket = request.createSocket();

      // If somehow the socket creation process fails, exit
      if (remoteSocket == null) {
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // Fire request to remote server
      OutputStream toRemote = remoteSocket.getOutputStream();
      request.fire(toRemote);

      // Prepare streams for forwarding of requests
      InputStream fromRemote = remoteSocket.getInputStream();
      Response response = Response.read(fromRemote);

      // Forward request to client and cache
      OutputStream toCache   = this.cache.getOutputStream(uri);
      response.forward(toClient, toCache);

      toClient.close();
      fromRemote.close();
      toRemote.close();
      fromClient.close();

      remoteSocket.close();
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

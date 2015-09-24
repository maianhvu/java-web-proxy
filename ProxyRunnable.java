import java.io.*;
import java.net.*;

public class ProxyRunnable implements Runnable {

  /**
   * Constants
   */
  private static final boolean CACHING_ENABLED = true;
  private static final boolean CENSOR_ENABLED = true;

  /**
   * Properties
   */
  private Socket clientSocket;
  private ProxyCache cache;
  private CensorEngine censorEngine;

  /**
   * Constructor
   */
  public ProxyRunnable(Socket clientSocket, ProxyCache cache, CensorEngine engine) {
    this.clientSocket = clientSocket;
    this.cache = cache;
    this.censorEngine = engine;
  }

  /**
   * Runnable implementation
   */
  public void run() {
    // Prepare the stream toClient so that we can send error response
    BufferedOutputStream toClient = null;
    // Have this one too so we can close it properly
    BufferedInputStream fromClient = null;
    try {
      // Read request from client
      fromClient = new BufferedInputStream(this.clientSocket.getInputStream());
      Request request = new Request(fromClient);

      // Check request's validity
      if (!request.isValid()) {
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      String uri = null;
      toClient = new BufferedOutputStream(this.clientSocket.getOutputStream());
      // Checks for cached item
      if (CACHING_ENABLED && this.cache.contains(uri = request.get(Request.Field.URI))) {
        // Reads from cache
        BufferedInputStream fromCache = this.cache.getFromURI(uri).getInputStream();
        Response response = Response.read(fromCache);
        response.forward(toClient);

        // Close all streams and exit
        toClient.close();
        fromCache.close();
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // If cached item doesn't exist or caching is disabled
      // Create a socket that connects to the remote server
      Socket remoteSocket = request.createSocket();

      // If somehow the socket creation process fails, exit
      if (remoteSocket == null) {
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // Fire request to remote server
      BufferedOutputStream toRemote = new BufferedOutputStream(remoteSocket.getOutputStream());
      request.fire(toRemote);

      // Prepare streams for forwarding of requests
      BufferedInputStream fromRemote = new BufferedInputStream(remoteSocket.getInputStream());
      Response response = Response.read(fromRemote);

      // Add censor engine if censoring is enabled
      if (CENSOR_ENABLED) response.setCensorEngine(this.censorEngine);

      // Forward request to client and cache
      BufferedOutputStream toCache = null;
      if (CACHING_ENABLED) toCache = this.cache.create(uri).getOutputStream();
      response.forward(toClient, toCache);

      toClient.close();
      fromRemote.close();
      toRemote.close();
      fromClient.close();

      remoteSocket.close();

      // WARNING: HARDCODE ERROR HANDLING ACTION BELOW
    } catch (IOException e) {
      if (toClient != null) {
        Response res = Response.createBadGateway();
        // Try to send this response to client
        try {
          res.forward(toClient);
        } catch (IOException e2) {
          e2.printStackTrace();
        } finally {
          try {
            toClient.close();
            fromClient.close();
          } catch (IOException e3) {
            e3.printStackTrace();
          }
        }
      } else {
        e.printStackTrace();
      }
    } finally {
      try {
        this.clientSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

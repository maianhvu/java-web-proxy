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

      // Check request's validity
      if (!request.isValid()) {
        System.out.println("Request invalid!");
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // Create a socket that connects to the remote server
      Socket remoteSocket = request.createSocket();

      // If somehow the socket creation process fails, exit
      if (remoteSocket == null) {
        System.out.println("Invalid socket!");
        fromClient.close();
        this.clientSocket.close();
        return;
      }

      // Fire request to remote server
      OutputStream toRemote = remoteSocket.getOutputStream();
      request.fire(toRemote);

      // Prepare streams for forwarding of requests
      InputStream fromRemote = remoteSocket.getInputStream();
      OutputStream toClient = this.clientSocket.getOutputStream();

      // Forward request to client
      Response response = Response.read(fromRemote);
      response.forward(toClient);

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

package demo.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BioEchoServer {

  public static void main(String[] args) throws IOException {

    var ss = new ServerSocket(6379);
    while (true) {
      var socket = ss.accept();
      System.out.println("new client accept " + socket.getRemoteSocketAddress());
      new EchoHandler(socket).start();
    }
  }

  static class EchoHandler extends Thread {

    final Socket socket;

    EchoHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      while (true) {
        try {
          // read
          var is = socket.getInputStream();
          var bis = new BufferedReader(new InputStreamReader(is));
          var line = bis.readLine();
          if (line == null) { // means client close
            socket.close();
            break;
          }
          if (line.equals("quit")) {
            socket.getOutputStream().write("good bye!\r\n".getBytes());
            socket.close();
            break;
          }
          line += "\r\n";
          socket.getOutputStream().write(line.getBytes());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}

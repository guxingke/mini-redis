package com.guxingke.redis;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jline.console.ConsoleReader;

public class Cli {

  private static final AtomicBoolean connected = new AtomicBoolean(false);
  private static String prompt = "noe connected";
  private static InetSocketAddress endpoint;
  private static volatile Socket socket;

  public static void log(Object msg) {
    System.out.println(msg);
  }

  public static void main(String[] args) throws IOException {
    // parse args
    var cfg = new Config();
    cfg.parse(args);

    endpoint = new InetSocketAddress(cfg.host, cfg.port);
    socket = new Socket();
    var np = cfg.host + ":" + cfg.port;
    try {
      socket.connect(endpoint);
      connected.set(true);
      Cli.prompt = np;
    } catch (IOException e) {
      log("Could not connect to Redis at %s:%d: Connection refused".formatted(cfg.host, cfg.port));
      // async reconnect
      new ReconnectThread(np).start();
    }

    // req
    ConsoleReader cin = new ConsoleReader();
    while (true) {
      String line = cin.readLine("%s> ".formatted(prompt));
      if (line == null) {
        System.exit(0);
        break;
      }
      if (line.isBlank()) {
        continue;
      }
      if (!socket.isConnected()) {
        log("Could not connect to Redis at %s:%d: Connection refused".formatted(cfg.host, cfg.port));
        continue;
      }

      // command
      var os = socket.getOutputStream();
      var is = new BufferedInputStream(socket.getInputStream());

      var bytes = (line + "\r\n").getBytes(StandardCharsets.US_ASCII);
      os.write(bytes);

      // block for is
      var flag = is.read();
      int i = 0;
      switch ((char) flag) {
        case '-' -> { // inline error
          log("(error) " + readLine(is));
        }
        case '+' -> { // Simple Strings
          log(readLine(is));
        }
        case '$' -> {
          var msg = readBulkString(is);
          if (msg == null) { // Null Bulk String.
            log("(nil)");
          } else {
            log(msg);
          }
        }
        case ':' -> {
          log(readInt(is));
        }
        case '*' -> {
          var len = readInt(is);
          List<Object> c = new ArrayList<>(len);
          for (int j = 0; j < len; j++) {
            var lf = is.read();
            switch ((char) lf) {
              case '$' -> c.add(readBulkString(is));
              case ':' -> c.add(readInt(is));
              case '+' -> c.add(readLine(is));
              default -> {
                throw new IllegalStateException();
              }
            }
          }
          var sb = new StringBuilder();
          for (int j = 0; j < c.size(); j++) {
            sb.append(j + 1).append(") ");
            var o = c.get(j);
            if (o instanceof String) {
              sb.append("\"").append(o).append("\"");
            } else {
              sb.append(o);
            }

            if (j < c.size() - 1) {
              sb.append("\n");
            }
          }
          log(sb.toString());
        }
        default -> {
          throw new IllegalStateException(flag + "");
        }
      }

      if (line.trim().equals("quit")) {
        socket.close();
        break;
      }
    }
  }

  private static String readLine(InputStream is) throws IOException {
    byte[] buf = new byte[1024];

    int i = 0;
    for (; ; ) {
      var b = is.read();
      if (b == 13) {
        // check next
        if (is.read() == 10) {
          break;
        }
      } else {
        buf[i++] = (byte) b;
      }
    }

    return new String(buf, 0, i);
  }

  private static int readInt(InputStream is) throws IOException {
    int len = 0;
    for (; ; ) {
      var t = is.read() - 48; // 48 -> 0
      if (t < 0) { // cr(13)
        is.read(); //skip lf(10）
        break;
      }
      len = len * 10 + t;
    }
    return len;
  }

  private static String readBulkString(InputStream is) throws IOException {
    // 1. fetch len
    int len = 0;
    for (; ; ) {
      var t = is.read() - 48; // 48 -> 0
      if (t == -3) { // - , negative number , null bulk string.
        return null;
      }
      if (t < 0) { // cr(13)
        is.read(); //skip lf(10）
        break;
      }
      len = len * 10 + t;
    }

    // 2. read n bytes
    var v = new String(is.readNBytes(len));
    is.readNBytes(2);
    return v;
  }

  static class ReconnectThread extends Thread {

    private final String prompt;

    ReconnectThread(String prompt) {
      this.prompt = prompt;
    }

    @Override
    public void run() {
      while (!connected.get()) {
        try {
          socket = new Socket();
          socket.connect(endpoint);
          connected.set(true);
          Cli.prompt = this.prompt;
        } catch (IOException ignore) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException i) {
          }
        }
      }
    }
  }

  static class Config {

    String host = "127.0.0.1";
    int port = 6379;

    void parse(String[] args) {
      for (int i = 0; i < args.length; i++) {
        var arg = args[i];
        if (arg.startsWith("-")) {
          switch (arg) {
            case "-h" -> host = args[++i];
            case "-p" -> port = Integer.parseInt(args[++i]);
            default -> throw new IllegalArgumentException();
          }
        }
      }
    }
  }
}

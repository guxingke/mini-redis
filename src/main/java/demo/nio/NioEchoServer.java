package demo.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class NioEchoServer {

  public static void main(String[] args) throws Exception {
    var ss = ServerSocketChannel.open();
    ss.bind(new InetSocketAddress("127.0.0.1", 6379));
    ss.configureBlocking(false); // non-blocking

    Selector selector = Selector.open();
    ss.register(selector, SelectionKey.OP_ACCEPT); // 注册 accept 事件

    for (; ; ) { // loop
      if (selector.select() > 0) { // 有可就绪事件
        var readyKeys = selector.selectedKeys();
        var iterator = readyKeys.iterator();

        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          // remove key
          iterator.remove();

          // ---
          if (key.isAcceptable()) { // 新链接
            var ssc = (ServerSocketChannel) key.channel();
            var sc = ssc.accept();
            sc.configureBlocking(false);

            // 注册事件
            sc.register(selector, SelectionKey.OP_READ, new Msg());
          } else if (key.isReadable()) { // 可读
            var sc = (SocketChannel) key.channel();
            var msg = (Msg) key.attachment();

            var rb = msg.rb;
            rb.clear();
            var read = sc.read(rb);
            rb.flip();
            if (read == 0) {
              key.cancel();
              continue;
            }

            var len = rb.limit();
            var bytes = new byte[len];
            rb.get(bytes);
            msg.replies.add(bytes);
            key.interestOps(SelectionKey.OP_WRITE);
          } else if (key.isWritable()) { // 可写
            var sc = (SocketChannel) key.channel();
            var msg = (Msg) key.attachment();
            var replies = msg.replies;
            var iter = replies.iterator();
            boolean close = false;
            while (iter.hasNext()) {
              byte[] bytes = iter.next();
              iter.remove();
              if (bytes.length == 6) { // check quit
                if (bytes[0] == 113 && bytes[1] == 117 && bytes[2] == 105 && bytes[3] == 116) {
                  bytes = "good bye!\r\n".getBytes();
                  close = true;
                }
              }
              var buf = ByteBuffer.allocate(bytes.length);
              buf.put(bytes);
              buf.flip();
              sc.write(buf);

              if (close) {
                key.cancel();
                key.channel().close();
              }
            }
            if (!close) {
              // 写完了读
              key.interestOps(SelectionKey.OP_READ);
            }
          }
        }
      }
    }
  }

  static class Msg {

    ByteBuffer rb = ByteBuffer.allocate(1024);
    List<byte[]> replies = new ArrayList<>();
  }

}

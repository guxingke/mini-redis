package demo.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyEchoServer {

  public static void main(String[] args) throws Exception {
    NioEventLoopGroup el = new NioEventLoopGroup(1);
    try {
      ServerBootstrap sb = new ServerBootstrap();
      sb.group(el).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
          .handler(new ChannelInitializer<ServerSocketChannel>() {
            @Override
            protected void initChannel(ServerSocketChannel sch) throws Exception {
              sch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
            }
          }).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
              ch.pipeline().addLast(new EchoHandler());
            }
          });

      ChannelFuture f = sb.bind(6379).sync();
      f.channel().closeFuture().sync();
    } finally {
      el.shutdownGracefully();
    }
  }

  static class EchoHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(
        ChannelHandlerContext ctx,
        Object msg
    ) throws Exception {
      var buf = (ByteBuf) msg;
      if (buf.readableBytes() == 6) { // maybe quit
        buf.markReaderIndex();
        if (buf.readByte() == 113 && buf.readByte() == 117 && buf.readByte() == 105 && buf.readByte() == 116
            && buf.readByte() == 13 && buf.readByte() == 10) {

          var bytes = "good bye!\r\n".getBytes();
          var buffer = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length);
          buffer.writeBytes(bytes);
          ctx.writeAndFlush(buffer);
          ctx.close();
          return;
        }
        buf.resetReaderIndex();
      }

      ctx.writeAndFlush(msg);
    }
  }

}


package com.guxingke.redis;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class Server {

  public static RedisServer server;

  public static void main(String[] args) throws Exception {
    initServerConfig();
    initServer();

    // aof & rdb
  }

  private static void initServer() throws Exception {
    RedisServer.db = new RedisDb(new Dict());

    // ----
    var rds = new RedisHandler();
    var elg = new NioEventLoopGroup(1);
    try {
      ServerBootstrap sb = new ServerBootstrap();
      sb.group(elg).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
          .handler(new ChannelInitializer<ServerSocketChannel>() {
            @Override
            protected void initChannel(ServerSocketChannel sch) throws Exception {
              sch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
//              sch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
//                @Override
//                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//                  ctx.channel().eventLoop().scheduleAtFixedRate(stats::printStats, 1, 10, TimeUnit.SECONDS);
//                }
//              });
            }
          }).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(new IdleStateHandler(60, 60, 60));
              ch.pipeline().addLast(rds);
            }
          });

      ChannelFuture f = sb.bind(6379).sync();
      f.channel().closeFuture().sync();
    } finally {
      elg.shutdownGracefully();
    }
  }

  private static void initServerConfig() {
    server.port = 6379;
    server.bindaddr = "127.0.0.1";
  }
}

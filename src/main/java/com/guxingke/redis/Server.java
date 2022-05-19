package com.guxingke.redis;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.handler.timeout.IdleStateHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

public class Server {

  public static RedisServer server;

  public static void main(String[] args) throws Exception {
    initServerConfig();
    initServer();

    // aof & rdb
  }

  private static void initServer() throws Exception {
    RedisServer.db = new RedisDb(new Dict());

    if (RedisServer.appendonly && RedisServer.aof.exists()) {
      Aof.loadAppendOnlyFile();
    } else if (RedisServer.rdb) {
      Rdb.loadRdb(RedisServer.db.dict);
    }

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
              sch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                  ctx.channel().eventLoop().scheduleAtFixedRate(Aof::flush, 1, 1, TimeUnit.SECONDS);
                }
              });
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

  private static void initServerConfig() throws FileNotFoundException {
    server.port = 6379;
    server.bindaddr = "127.0.0.1";

    // 开启 AOF
    server.appendonly = true;
    server.aofbuf = new byte[0];
    server.aof = new File("appendonly.aof");

    // rdb
    server.rdb = true;
  }
}

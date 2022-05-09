package com.guxingke.redis;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class RedisHandler extends ChannelInboundHandlerAdapter {

  private static Logger log = LoggerFactory.getLogger(RedisHandler.class);
  private AttributeKey<RedisClient> CLI = AttributeKey.newInstance("client");

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    var cli = new RedisClient(new DefaultChannel(ctx.channel()));
    ctx.channel().attr(CLI).setIfAbsent(cli);
  }

  @Override
  public void channelRead(
      ChannelHandlerContext ctx,
      Object msg
  ) throws Exception {
    if (!(msg instanceof ByteBuf bb)) {
      return;
    }

    var cli = ctx.channel().attr(CLI).get();

    // read to query buf
    var len = bb.readableBytes();
    var buf = new byte[len];
    bb.readBytes(buf);
    cli.querybuf = Bytes.cat(cli.querybuf, buf);

    processInputBuffer(cli);
  }

  private void processInputBuffer(RedisClient c) {
    while (c.querybuf.length > 0) { // has data

      // detect req type
      if (c.reqType == 0) {
        if (c.querybuf[0] == (byte) '*') {
          c.reqType = 2; // multi bulk
        } else {
          c.reqType = 1;  // inline
        }
      }

      if (c.reqType == 1) {
        if (!processInlineBuffer(c)) {
          break;
        }
      } else {
        if (!processMultiBulkBuffer(c)) {
          break;
        }
      }

      if (c.argv.length == 0) {
        c.reset();
        return;
      }
      if (processCommand(c)) {
        c.reset();
      }
    }
  }

  @Override
  public void userEventTriggered(
      ChannelHandlerContext ctx,
      Object evt
  ) throws Exception {
    log.debug("catch event {}", evt);
    super.userEventTriggered(ctx, evt);
  }

  private boolean processCommand(RedisClient c) {
    // lookup cmd
    RedisCommand cmd = null;
    for (RedisCommand command : RedisServer.commands) {
      if (Arrays.equals(command.name().getBytes(), c.argv[0])) {
        cmd = command;
      }
    }
    if (cmd == null) {
      var name = new String(c.argv[0]);
      log.debug("not found valid command {}", name);
      c.sendError("unknown command '%s'".formatted(name));
      return true;
    }

    cmd.proc().apply(c);

    return true;
  }

  private boolean processInlineBuffer(RedisClient c) {
    int pos = Bytes.newline(c.querybuf);
    if (pos <= 0) {
      return false;
    }

    c.argv = Bytes.split(c.querybuf, pos);
    c.querybuf = Bytes.range(c.querybuf, pos + 2, -1);

    return true;
  }

  private boolean processMultiBulkBuffer(RedisClient c) {
    return true;
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(
      ChannelHandlerContext ctx,
      Throwable cause
  ) throws Exception {
    log.error("catch exception, {}", ctx, cause);
    ctx.close();
  }
}

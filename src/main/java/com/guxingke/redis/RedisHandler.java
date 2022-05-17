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

  public static void processInputBuffer(RedisClient c) {
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

  private static boolean processCommand(RedisClient c) {
    // lookup cmd
    RedisCommand cmd = null;
    for (RedisCommand command : RedisServer.commands) {
      if (Arrays.equals(command.name().getBytes(), c.argv[0])) {
        cmd = command;
        break;
      }
    }
    if (cmd == null) {
      var name = new String(c.argv[0]);
      log.debug("not found valid command {}", name);
      c.sendError("unknown command '%s'".formatted(name));
      return true;
    }

    call(c, cmd);

    return true;
  }

  private static void call(
      RedisClient c,
      RedisCommand cmd
  ) {
    var dirty = RedisServer.dirty;
    cmd.proc().apply(c);
    var changes = dirty != RedisServer.dirty;

    if (RedisServer.appendonly && changes) { // dirty changed
      Aof.feedAppendOnlyFile(cmd, 0, c.argv, c.argv.length);
    }

    RedisServer.stat_numcommands++;
  }

  private static boolean processInlineBuffer(RedisClient c) {
    int pos = Bytes.newline(c.querybuf);
    if (pos <= 0) {
      return false;
    }

    c.argv = Bytes.split(c.querybuf, pos);
    c.querybuf = Bytes.range(c.querybuf, pos + 2, -1);

    return true;
  }

  // *3\r\n$3\r\nset\r\n$3\r\nfoo\r\n$3\r\nbar\r\n*3\r\n$3\r\nset\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
  private static boolean processMultiBulkBuffer(RedisClient c) {
    if (c.multibulklen == 0) {
      int pos = Bytes.newline(c.querybuf);
      if (pos <= 0) {
        return false;
      }
      // skip *
      var len = Bytes.parseInt(Bytes.range(c.querybuf, 1, pos));
      if (len <= 0) {
        c.querybuf = Bytes.range(c.querybuf, pos + 2, -1);
        return true;
      }
      if (len > 1024 * 1024) { // 1M
        c.sendError("Protocol error: invalid multibulk length");
        return false;
      }

      c.querybuf = Bytes.range(c.querybuf, pos + 2, -1);
      c.argv = new byte[len][];
      c.multibulklen = len;
    }

    while (c.multibulklen > 0) {
      /* Read bulk length if unknown */
      if (c.bulklen == -1) {
        int pos = Bytes.newline(c.querybuf);
        if (pos <= 0) {
          /* No newline in current buffer, so wait for more data */
          break;
        }
        // check $
        if (c.querybuf[0] != (byte) '$') {
          c.sendError("Protocol error: expected '$', got '%c'".formatted(c.querybuf[pos]));
          return false;
        }

        var len = Bytes.parseInt(Bytes.range(c.querybuf, 1, pos));
        c.bulklen = len;
        c.querybuf = Bytes.range(c.querybuf, pos + 2, -1);
      }

      /* Read bulk argument */
      if (c.querybuf.length >= c.bulklen + 2) {
        c.argv[c.argv.length - c.multibulklen] = Bytes.range(c.querybuf, 0, c.bulklen);
        c.querybuf = Bytes.range(c.querybuf, c.bulklen + 2, -1);
        c.bulklen = -1;
        c.multibulklen--;
      } else {
        break;
      }
    }
    /* We're done when c->multibulk == 0 */
    if (c.multibulklen == 0) {
      return true;
    }

    return false;
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

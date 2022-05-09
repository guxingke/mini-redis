package com.guxingke.redis;

import java.nio.charset.StandardCharsets;

/**
 * 表示一个客户端，关联 channel
 */
public class RedisClient {

  public final Channel channel;
  public Dict dict;

  public byte reqType;
  public byte[] querybuf;
  public byte[][] argv;

  public RedisClient(Channel channel) {
    this.channel = channel;
    this.dict = RedisServer.db.dict;
    reset();
    // final
    this.channel.bindClient(this);
  }

  public void reset() {
    this.querybuf = new byte[0];
    this.argv = new byte[0][0];
    this.reqType = 0;
  }

  public void sendError(String msg) {
    send("-" + msg + "\r\n");
  }

  public void send(String msg) {
    channel.write((msg.getBytes(StandardCharsets.US_ASCII)));
    channel.flush();
  }

  public void sendBulkBytes(byte[] raw) {
    channel.write(("$" + (raw.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
    channel.write(raw);
    channel.write(new byte[]{13, 10});
    channel.flush();
  }
}

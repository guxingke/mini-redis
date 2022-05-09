package com.guxingke.redis;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

public class DefaultChannel implements Channel{

  private final io.netty.channel.Channel channel;
  private RedisClient client;

  public DefaultChannel(io.netty.channel.Channel channel) {
    this.channel = channel;
  }

  @Override
  public RedisClient getClient() {
    return client;
  }

  @Override
  public void bindClient(RedisClient client) {
    if (this.client == null) {
      this.client = client;
    }
  }

  @Override
  public void write(byte[] bytes) {
    ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length);
    buf.writeBytes(bytes);
    this.channel.write(buf);
  }

  @Override
  public void flush() {
    this.channel.flush();
  }
}

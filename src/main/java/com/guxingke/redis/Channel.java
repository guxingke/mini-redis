package com.guxingke.redis;

/**
 * 传输通道
 */
public interface Channel {

  /**
   * 任意 channel 必须关联一个 client
   * @return client
   */
  RedisClient getClient();


  /**
   * 绑定 client
   */
  void bindClient(RedisClient client);

  void write(byte[] msg);

  void flush();
}

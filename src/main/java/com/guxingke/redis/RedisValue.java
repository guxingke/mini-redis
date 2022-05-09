package com.guxingke.redis;

public class RedisValue {

  public final byte[] raw;
  public final int version;

  public RedisValue(
      byte[] raw,
      int version
  ) {
    this.raw = raw;
    this.version = version;
  }
}

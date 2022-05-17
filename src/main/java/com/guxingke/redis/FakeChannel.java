package com.guxingke.redis;

public class FakeChannel implements Channel{

  private RedisClient client;

  @Override
  public RedisClient getClient() {
    return client;
  }

  @Override
  public void bindClient(RedisClient client) {
    this.client = client;
  }

  @Override
  public void write(byte[] msg) {

  }

  @Override
  public void flush() {

  }
}

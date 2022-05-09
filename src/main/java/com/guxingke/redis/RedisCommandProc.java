package com.guxingke.redis;

@FunctionalInterface
public interface RedisCommandProc {

  void apply(RedisClient c);
}


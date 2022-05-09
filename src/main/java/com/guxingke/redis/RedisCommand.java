package com.guxingke.redis;

/**
 * 命令容器
 */
public record RedisCommand(String name, RedisCommandProc proc, int arity, int flags) {

  public static RedisCommand of(
      String name,
      RedisCommandProc proc,
      int arity,
      int flags
  ) {
    return new RedisCommand(name, proc, arity, flags);
  }
}

package com.guxingke.redis;

/**
 * 杂七杂八的
 */
public abstract class TypeCommon {

  public static RedisCommandProc pingCommand() {
    return c -> {
      c.send("+Pong\r\n");
    };
  }

  public static RedisCommandProc echoCommand() {
    return c -> {
      c.sendBulkBytes(c.argv[1]);
    };
  }
}

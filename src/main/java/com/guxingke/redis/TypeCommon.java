package com.guxingke.redis;

import static com.guxingke.redis.Rdb.saveRdb;

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

  public static RedisCommandProc saveCommand() {
    return c -> {
      try {
        saveRdb(c.dict);
        c.send("+OK\r\n");
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
  }

}

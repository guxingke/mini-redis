package com.guxingke.redis;

/**
 * String 相关
 */
public abstract class TypeString {

  // set foo bar
  public static RedisCommandProc setCommand() {
    return c -> {
      c.dict.put(c.argv[1], new RedisValue(c.argv[2], RedisServer.version));
      c.send("+OK\r\n");
    };
  }

  public static RedisCommandProc getCommand() {
    return c -> {
      var val = c.dict.get(c.argv[1]);
      if (val == null) {
        c.send("$-1\r\n");
        return;
      }

      c.sendBulkBytes(val.raw);
    };
  }
}

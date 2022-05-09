package com.guxingke.redis;

import static com.guxingke.redis.RedisCommand.of;

public abstract class RedisServer {

  public static RedisDb db;
  public static int port;
  public static String bindaddr;

  public static RedisCommand[] commands = new RedisCommand[]{
      of("ping", TypeCommon.pingCommand(), 1, 0),
      of("echo", TypeCommon.echoCommand(), 2, 0),
      of("get", TypeString.getCommand(), 2, 0),
      of("set", TypeString.setCommand(), 3, 0),
  };

  public static int version = 0;
}

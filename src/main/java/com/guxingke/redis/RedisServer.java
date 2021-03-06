package com.guxingke.redis;

import static com.guxingke.redis.RedisCommand.of;

import java.io.File;
import java.io.FileOutputStream;

public abstract class RedisServer {

  public static RedisDb db;
  public static int port;
  public static String bindaddr;

  public static long dirty;

  public static RedisCommand[] commands = new RedisCommand[]{
      of("ping", TypeCommon.pingCommand(), 1, 0),
      of("echo", TypeCommon.echoCommand(), 2, 0),
      of("get", TypeString.getCommand(), 2, 0),
      of("set", TypeString.setCommand(), 3, 0),
      of("del", TypeString.delCommand(), 2, 0),
      of("save", TypeCommon.saveCommand(), 1, 0),
  };

  public static int version = 0;
  public static long stat_numcommands;

  // aof
  public static boolean appendonly;
  public static byte[] aofbuf;
  public static File aof;
  public static FileOutputStream aofout;

  // rdb
  public static boolean rdb;
}

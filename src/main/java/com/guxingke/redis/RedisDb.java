package com.guxingke.redis;

public class RedisDb {

  public final Dict dict;
//  public final Dict expires;

  public RedisDb(
      Dict dict
//      Dict expires
  ) {
    this.dict = dict;
//    this.expires = expires;
  }

//  dict *dict;                 /* The keyspace for this DB */
//  dict *expires;              /* Timeout of keys with a timeout set */
//  dict *blocking_keys;        /* Keys with clients waiting for data (BLPOP) */
//  dict *io_keys;              /* Keys with clients waiting for VM I/O */
//  dict *watched_keys;         /* WATCHED keys for MULTI/EXEC CAS */
//  int id;
}

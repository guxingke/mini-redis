package com.guxingke.redis;

import java.nio.charset.StandardCharsets;

public interface Aof {

//  sds catAppendOnlyGenericCommand(sds buf, int argc, robj **argv) {
//    int j;
//    buf = sdscatprintf(buf,"*%d\r\n",argc);
//    for (j = 0; j < argc; j++) {
//      robj *o = getDecodedObject(argv[j]);
//      buf = sdscatprintf(buf,"$%lu\r\n",(unsigned long)sdslen(o->ptr));
//      buf = sdscatlen(buf,o->ptr,sdslen(o->ptr));
//      buf = sdscatlen(buf,"\r\n",2);
//      decrRefCount(o);
//    }
//    return buf;
//  }
  static byte[] catAppendOnlyGenericCommand(
      byte[] buf,
      int argc,
      byte[][] argv
  ) {
    buf = Bytes.cat(buf, "*%d\r\n".formatted(argc).getBytes(StandardCharsets.US_ASCII));
    for (int j = 0; j < argc; j++) {
      var bytes = argv[j];
      buf = Bytes.cat(buf, "$%d\r\n".formatted(bytes.length).getBytes(StandardCharsets.US_ASCII));
      buf = Bytes.cat(buf, bytes);
      buf = Bytes.cat(buf, "\r\n".getBytes(StandardCharsets.US_ASCII));
    }

    return buf;
  }

//  void feedAppendOnlyFile(struct redisCommand *cmd, int dictid, robj **argv, int argc) {
//    sds buf = sdsempty();
//    robj *tmpargv[3];
//
//    /* The DB this command was targetting is not the same as the last command
//     * we appendend. To issue a SELECT command is needed. */
//    if (dictid != server.appendseldb) { // select db , 如果只用一个库的话，aof 里就只有一次 select 0 , 岂不美哉
//      char seldb[64];
//
//      snprintf(seldb,sizeof(seldb),"%d",dictid);
//      buf = sdscatprintf(buf,"*2\r\n$6\r\nSELECT\r\n$%lu\r\n%s\r\n",
//                         (unsigned long)strlen(seldb),seldb);
//      server.appendseldb = dictid;
//    }
//
//    if (cmd->proc == expireCommand) {
//      /* Translate EXPIRE into EXPIREAT */
//      buf = catAppendOnlyExpireAtCommand(buf,argv[1],argv[2]); // 这里会有主从过期时间不一致(gap 很小）
//    } else if (cmd->proc == setexCommand) {
//      /* Translate SETEX to SET and EXPIREAT */
//      tmpargv[0] = createStringObject("SET",3);
//      tmpargv[1] = argv[1];
//      tmpargv[2] = argv[3];
//      buf = catAppendOnlyGenericCommand(buf,3,tmpargv);
//      decrRefCount(tmpargv[0]);
//      buf = catAppendOnlyExpireAtCommand(buf,argv[1],argv[2]);
//    } else {
//      buf = catAppendOnlyGenericCommand(buf,argc,argv);
//    }
//
//    /* Append to the AOF buffer. This will be flushed on disk just before
//     * of re-entering the event loop, so before the client will get a
//     * positive reply about the operation performed. */
//    server.aofbuf = sdscatlen(server.aofbuf,buf,sdslen(buf));
//
//    /* If a background append only file rewriting is in progress we want to
//     * accumulate the differences between the child DB and the current one
//     * in a buffer, so that when the child process will do its work we
//     * can append the differences to the new append only file. */
//    if (server.bgrewritechildpid != -1)
//      server.bgrewritebuf = sdscatlen(server.bgrewritebuf,buf,sdslen(buf));
//
//    sdsfree(buf);
//  }
  static void feedAppendOnlyFile(RedisCommand cmd, int dictid, byte[][] argv, int argc) {
    byte[] buf = new byte[0];
    buf = Aof.catAppendOnlyGenericCommand(buf, argc, argv);
    Bytes.cat(RedisServer.aofbuf, buf);
  }
}


package com.guxingke.redis;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
  static void feedAppendOnlyFile(
      RedisCommand cmd,
      int dictid,
      byte[][] argv,
      int argc
  ) {
    if (RedisServer.aofout == null) {
      try {
        RedisServer.aofout = new FileOutputStream(RedisServer.aof, true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
    byte[] buf = new byte[0];
    buf = Aof.catAppendOnlyGenericCommand(buf, argc, argv);
    RedisServer.aofbuf = Bytes.cat(RedisServer.aofbuf, buf);
  }

  static void flush() {
//    System.out.println("aof flush " + System.currentTimeMillis());
    if (RedisServer.aofbuf.length != 0) {
      try {
        RedisServer.aofout.write(RedisServer.aofbuf);
        RedisServer.aofout.getFD().sync();
        RedisServer.aofbuf = new byte[0];
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /* Replay the append log file. On error REDIS_OK is returned. On non fatal
   * error (the append only file is zero-length) REDIS_ERR is returned. On
   * fatal error an error message is logged and the program exists. */
//  int loadAppendOnlyFile(char *filename) {
//    struct redisClient *fakeClient;
//    FILE *fp = fopen(filename,"r");
//    struct redis_stat sb;
//    int appendonly = server.appendonly;
//    long loops = 0;
//
//    if (redis_fstat(fileno(fp),&sb) != -1 && sb.st_size == 0)
//    return REDIS_ERR;
//
//    if (fp == NULL) {
//      redisLog(REDIS_WARNING,"Fatal error: can't open the append log file for reading: %s",strerror(errno));
//      exit(1);
//    }
//
//    /* Temporarily disable AOF, to prevent EXEC from feeding a MULTI
//     * to the same file we're about to read. */
//    server.appendonly = 0;
//
//    fakeClient = createFakeClient();
//    startLoading(fp);
//
//    while(1) {
//      int argc, j;
//      unsigned long len;
//      robj **argv;
//      char buf[128];
//      sds argsds;
//      struct redisCommand *cmd;
//      int force_swapout;
//
//      /* Serve the clients from time to time */
//      if (!(loops++ % 1000)) {
//        loadingProgress(ftello(fp));
//        aeProcessEvents(server.el, AE_FILE_EVENTS|AE_DONT_WAIT);
//      }
//
//      if (fgets(buf,sizeof(buf),fp) == NULL) {
//        if (feof(fp))
//          break;
//        else
//                goto readerr;
//      }
//      if (buf[0] != '*') goto fmterr;
//      argc = atoi(buf+1);
//      argv = zmalloc(sizeof(robj*)*argc);
//      for (j = 0; j < argc; j++) {
//        if (fgets(buf,sizeof(buf),fp) == NULL) goto readerr;
//        if (buf[0] != '$') goto fmterr;
//        len = strtol(buf+1,NULL,10);
//        argsds = sdsnewlen(NULL,len);
//        if (len && fread(argsds,len,1,fp) == 0) goto fmterr;
//        argv[j] = createObject(REDIS_STRING,argsds);
//        if (fread(buf,2,1,fp) == 0) goto fmterr; /* discard CRLF */
//      }
//
//      /* Command lookup */
//      cmd = lookupCommand(argv[0]->ptr);
//      if (!cmd) {
//        redisLog(REDIS_WARNING,"Unknown command '%s' reading the append only file", argv[0]->ptr);
//        exit(1);
//      }
//      /* Run the command in the context of a fake client */
//      fakeClient->argc = argc;
//      fakeClient->argv = argv;
//      cmd->proc(fakeClient);
//
//      /* The fake client should not have a reply */
//      redisAssert(fakeClient->bufpos == 0 && listLength(fakeClient->reply) == 0);
//
//      /* Clean up, ready for the next command */
//      for (j = 0; j < argc; j++) decrRefCount(argv[j]);
//      zfree(argv);
//
//      /* Handle swapping while loading big datasets when VM is on */
//      force_swapout = 0;
//      if ((zmalloc_used_memory() - server.vm_max_memory) > 1024*1024*32)
//        force_swapout = 1;
//
//      if (server.vm_enabled && force_swapout) {
//        while (zmalloc_used_memory() > server.vm_max_memory) {
//          if (vmSwapOneObjectBlocking() == REDIS_ERR) break;
//        }
//      }
//    }
//
//    /* This point can only be reached when EOF is reached without errors.
//     * If the client is in the middle of a MULTI/EXEC, log error and quit. */
//    if (fakeClient->flags & REDIS_MULTI) goto readerr;
//
//    fclose(fp);
//    freeFakeClient(fakeClient);
//    server.appendonly = appendonly;
//    stopLoading();
//    return REDIS_OK;
//
//    readerr:
//    if (feof(fp)) {
//      redisLog(REDIS_WARNING,"Unexpected end of file reading the append only file");
//    } else {
//      redisLog(REDIS_WARNING,"Unrecoverable error reading the append only file: %s", strerror(errno));
//    }
//    exit(1);
//    fmterr:
//    redisLog(REDIS_WARNING,"Bad file format reading the append only file: make a backup of your AOF file, then use ./redis-check-aof --fix <filename>");
//    exit(1);
//  }

  static boolean loadAppendOnlyFile() {
    if (RedisServer.aof.exists()) {
      var fc = new RedisClient(new FakeChannel());

      try (var is = new BufferedInputStream(new FileInputStream(RedisServer.aof))) {
        while (is.available() > 0) {
          var bytes = is.readNBytes(1024);
          fc.querybuf = Bytes.cat(fc.querybuf, bytes);
          RedisHandler.processInputBuffer(fc);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return true;
  }
}


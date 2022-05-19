package com.guxingke.redis;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RedisRdbOutputStream extends OutputStream {

  private final FileOutputStream out;

  public RedisRdbOutputStream(FileOutputStream out) {
    this.out = out;
  }

  @Override
  public void write(int b) throws IOException {
    this.out.write(b);
  }

  public void write(byte[] b) throws IOException {
    this.out.write(b);
  }


  //  unsigned char buf[2];
//  int nwritten;
//
//    if (len < (1<<6)) {
//    /* Save a 6 bit len */
//    buf[0] = (len&0xFF)|(REDIS_RDB_6BITLEN<<6);
//    if (rdbWriteRaw(fp,buf,1) == -1) return -1;
//    nwritten = 1;
//  } else if (len < (1<<14)) {
//    /* Save a 14 bit len */
//    buf[0] = ((len>>8)&0xFF)|(REDIS_RDB_14BITLEN<<6);
//    buf[1] = len&0xFF;
//    if (rdbWriteRaw(fp,buf,2) == -1) return -1;
//    nwritten = 2;
//  } else {
//    /* Save a 32 bit len */
//    buf[0] = (REDIS_RDB_32BITLEN<<6);
//    if (rdbWriteRaw(fp,buf,1) == -1) return -1;
//    len = htonl(len);
//    if (rdbWriteRaw(fp,&len,4) == -1) return -1;
//    nwritten = 1+4;
//  }
//    return nwritten;
//
  // write len
  public void writeLen(int len) throws IOException {
    if (len < (1 << 6)) {
      // Save a 6 bit len
      this.out.write(((byte) len));
      return;
    }
    byte[] buf = new byte[2];
    if (len < (1 << 14)) {
      // Save a 14 bit len
      buf[0] = ((byte) (((len >> 8) & 0xFF) | (1 << 6)));
      buf[1] = ((byte) (len & 0xFF));
      this.out.write(buf);
      return;
    }
    // Save a 32 bit len
    buf = new byte[5];
    buf[0] = ((byte) (2 << 6));
    buf[1] = (byte) (len >>> 24);
    buf[2] = (byte) (len >>> 16);
    buf[3] = (byte) (len >>> 8);
    buf[4] = (byte) (len >>> 0);
    out.write(buf, 0, 5);
  }
}

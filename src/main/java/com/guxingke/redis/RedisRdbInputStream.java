package com.guxingke.redis;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RedisRdbInputStream extends InputStream {

  private final FileInputStream in;

  public RedisRdbInputStream(FileInputStream in) {
    this.in = in;
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  public int readLen() throws IOException {
    var t = this.read();
    var h = (((byte) t) & 0xc0) >> 6;
    if (h == 2) {
      // read int
      int ch1 = in.read();
      int ch2 = in.read();
      int ch3 = in.read();
      int ch4 = in.read();
      if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
      }
      return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    if (h == 1) {
      // read short
      int ch1 = in.read();
      int ch2 = in.read();
      return (ch1 & 0x3f) << 8 | ch2;
    }

    // read byte
    return t & 0x3f;
  }
}

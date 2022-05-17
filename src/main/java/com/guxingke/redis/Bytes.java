package com.guxingke.redis;

/**
 * byte 数组工具
 */
public abstract class Bytes {

  public static byte[] cat(
      byte[] dst,
      byte[] neo
  ) {
    var bytes = new byte[dst.length + neo.length];
    System.arraycopy(dst, 0, bytes, 0, dst.length);
    System.arraycopy(neo, 0, bytes, dst.length, neo.length);
    return bytes;
  }

  public static int newline(byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] == 13) { // CR
        if (i + 1 < bytes.length) {
          if (bytes[i + 1] == 10) { // LF
            return i;
          }
        }
      }
    }
    return -1;
  }

  /**
   * split by space(multi space)
   */
  public static byte[][] split(
      byte[] buf,
      int pos
  ) {
    int len = 0;
    for (int i = 0; i < pos; i++) {
      if (buf[i] == (byte) ' ') { // skip begin
        while (buf[i + 1] == (byte) ' ') {
          i++;
        }
        continue;
      }

      while (buf[i] != ' ') {
        if (i == pos) {
          break;
        }
        i++;
      }
      len++;
    }
    var bytes = new byte[len][];

    int idx = 0;
    int b = 0;
    for (int i = 0; i < pos; i++) {
      if (buf[i] == (byte) ' ') { // skip begin
        while (buf[i + 1] == (byte) ' ') {
          i++;
        }
        b = i + 1;
        continue;
      }

      while (buf[i] != (byte) ' ') {
        i++;
        if (i == pos) {
          break;
        }
      }

      var tmp = new byte[i - b];
      System.arraycopy(buf, b, tmp, 0, tmp.length);
      b = i + 1;
      bytes[idx++] = tmp;
    }
    return bytes;
  }

  public static byte[] range(
      byte[] buf,
      int begin,
      int end
  ) {
    if (begin >= buf.length - 1) {
      return new byte[0];
    }
    if (end <= 0) {
      end = buf.length;
    }
    var t = new byte[end - begin];
    System.arraycopy(buf, begin, t, 0, t.length);
    return t;
  }

  // parse char array to int
  public static int parseInt(byte[] bytes) {
    int len = 0;
    for (byte b : bytes) {
      var t = b - 48; // 48 -> 0
      len = len * 10 + t;
    }
    return len;
  }
}

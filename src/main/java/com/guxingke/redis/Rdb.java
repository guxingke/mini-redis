package com.guxingke.redis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public interface Rdb {

  static boolean saveRdb(Dict dict) throws Exception {
    File file = new File("tmp.rdb");
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();

    var fos = new FileOutputStream(file);
    var os = new RedisRdbOutputStream(fos);
    try (os) {
      // magic
      os.write("REDIS0001".getBytes(StandardCharsets.US_ASCII));
      os.write(254);
      os.writeLen(0);

      // dict
      for (Dict.Entry entry : dict.table) {
        if (entry == null) {
          continue;
        }
        var key = entry.key;
        var value = entry.value.raw;
        os.write(0); //type

        os.writeLen(key.length);
        os.write(key);

        os.writeLen(value.length);
        os.write(value);

        // more
        while (entry.next != null) {
          entry = entry.next;
          key = entry.key;
          value = entry.value.raw;
          os.write(0); //type

          os.writeLen(key.length);
          os.write(key);

          os.writeLen(value.length);
          os.write(value);
        }

      }

      os.write(255);
      os.flush();
    }
    file.renameTo(new File("dump.rdb"));
    return true;
  }

  static void loadRdb(Dict dict) throws Exception {
    var f = new File("dump.rdb");
    if (!f.exists()) {
      return;
    }
    var fis = new FileInputStream(f);
    var is = new RedisRdbInputStream(fis);

    try (is) {
      var rb = is.readNBytes(5);
      var vb = is.readNBytes(4);

      is.read();
      is.read();

      var type = is.read();
      while (type != 255) {
        var ekl = is.readLen();
        var ekb = is.readNBytes(ekl);
        var evl = is.readLen();
        var evb = is.readNBytes(evl);

        dict.put(ekb, new RedisValue(evb, RedisServer.version));
        type = is.read();
      }
    }

  }
}

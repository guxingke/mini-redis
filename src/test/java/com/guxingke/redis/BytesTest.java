package com.guxingke.redis;

import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

public class BytesTest {

  @Test
  public void split() {

    var bytes = "SET FO BARR\r\n".getBytes(StandardCharsets.US_ASCII);
    var pos = Bytes.newline(bytes);
    Assert.assertEquals(11, pos);

    var split = Bytes.split(bytes, pos);
    Assert.assertEquals(3, split.length);
    Assert.assertEquals(3, split[0].length);
    Assert.assertEquals(2, split[1].length);
    Assert.assertEquals(4, split[2].length);
  }

  @Test
  public void split_2() {
    var bytes = " SET  FO  BARR \r\n".getBytes(StandardCharsets.US_ASCII);
    var pos = Bytes.newline(bytes);
    Assert.assertEquals(15, pos);

    var split = Bytes.split(bytes, pos);
    Assert.assertEquals(3, split.length);
    Assert.assertEquals(3, split[0].length);
    Assert.assertEquals(2, split[1].length);
    Assert.assertEquals(4, split[2].length);
  }

  @Test
  public void range() {
    var bytes = "*2\r\n$1\r\n1".getBytes(StandardCharsets.US_ASCII);
    var pos = Bytes.newline(bytes);

    var nb = Bytes.range(bytes, pos + 2, -1);
    Assert.assertEquals(bytes.length - pos - 2, nb.length);
  }
}
package com.guxingke.redis;

public class Dict {

  public Entry[] table;
  public int size;
  public int cnt;

  public Dict() {
    this(0);
  }

  public Dict(int size) {
    this.size = size;
    table = new Entry[size];
  }

  public Dict copy() {
    Entry[] ee = new Entry[this.size];
    System.arraycopy(this.table, 0, ee, 0, this.size);

    Dict dict = new Dict();
    dict.table = ee;
    dict.size = size;
    dict.cnt = cnt;
    return dict;
  }


  public RedisValue get(byte[] key) {
    if (size == 0) {
      return null;
    }
    Entry entry = table[hash(key) & size - 1];
    if (entry == null) {
      return null;
    }

    if (entry.equals(key)) {
      return entry.value;
    }

    while (entry.next != null) {
      entry = entry.next;
      if (entry.equals(key)) {
        return entry.value;
      }
    }

    return null;
  }

  private int hash(byte[] key) {
    return hash32(key, key.length, 0x9747b28c);
  }

  public void put(
      byte[] key,
      RedisValue value
  ) {
    expand();
    put(table, key, value);
  }

  private void put(
      Entry[] table,
      byte[] key,
      RedisValue value
  ) {
    Entry entry = table[hash(key) & size - 1];
    if (entry == null) {
      cnt++;
      Entry ne = new Entry(key, value);
      table[hash(key) & size - 1] = ne;
      return;
    }

    if (entry.equals(key)) {
      entry.value = value;
      return;
    }

    while (entry.next != null) {
      entry = entry.next;
      if (entry.equals(key)) {
        entry.value = value;
        return;
      }
    }

    // new
    cnt++;
    entry.next = new Entry(key, value);
  }

  private void expand() {
    if (this.size == 0) {
      this.size = 16;
      this.table = new Entry[this.size];
      return;
    }
    if (this.cnt * 100 / this.size > 80) {
      var os = this.size;
      var ot = this.table;

      this.size = this.size * 2;
      this.table = new Entry[this.size];

      // rehash
      for (Entry e : ot) {
        if (e == null) {
          continue;
        }
        this.put(this.table, e.key, e.value);
      }
    }
  }

  public RedisValue remove(byte[] key) {
    Entry entry = table[hash(key) & size - 1];
    if (entry == null) {
      return null;
    }

    if (entry.equals(key)) {
      Entry next = entry.next;
      entry.next = null;
      table[hash(key) & size - 1] = next;
      cnt--;
      return entry.value;
    }

    while (entry.next != null) {
      Entry head = entry;
      entry = entry.next;
      if (entry.equals(key)) { // found
        head.next = entry.next;
        cnt--;
        return entry.value;
      }
    }

    return null;
  }

  static class Entry {

    public final byte[] key;

    public RedisValue value;
    public Entry next;

    Entry(
        byte[] key,
        RedisValue value
    ) {
      this.key = key;
      this.value = value;
    }

    boolean equals(byte[] o) {
      if (this.key == o) {
        return true;
      }
      if (this.key.length != o.length) {
        return false;
      }
      for (int i = 0; i < this.key.length; i++) {
        if (this.key[i] != o[i]) {
          return false;
        }
      }
      return true;
    }
  }

  // https://commons.apache.org/proper/commons-codec/jacoco/org.apache.commons.codec.digest/MurmurHash2.java.html
  // Constants for 32-bit variant
  private static final int M32 = 0x5bd1e995;
  private static final int R32 = 24;

  /**
   * Generates a 32-bit hash from byte array with the given length and seed.
   *
   * @param data The input byte array
   * @param length The length of the array
   * @param seed The initial seed value
   * @return The 32-bit hash
   */
  public static int hash32(
      final byte[] data,
      final int length,
      final int seed
  ) {
    // Initialize the hash to a random value
    int h = seed ^ length;

    // Mix 4 bytes at a time into the hash
    final int nblocks = length >> 2;

    // body
    for (int i = 0; i < nblocks; i++) {
      final int index = (i << 2);
      int k = getLittleEndianInt(data, index);
      k *= M32;
      k ^= k >>> R32;
      k *= M32;
      h *= M32;
      h ^= k;
    }

    // Handle the last few bytes of the input array
    final int index = (nblocks << 2);
    switch (length - index) {
      case 3:
        h ^= (data[index + 2] & 0xff) << 16;
      case 2:
        h ^= (data[index + 1] & 0xff) << 8;
      case 1:
        h ^= (data[index] & 0xff);
        h *= M32;
    }

    // Do a few final mixes of the hash to ensure the last few
    // bytes are well-incorporated.
    h ^= h >>> 13;
    h *= M32;
    h ^= h >>> 15;

    return h;
  }

  /**
   * Gets the little-endian int from 4 bytes starting at the specified index.
   *
   * @param data The data
   * @param index The index
   * @return The little-endian int
   */
  private static int getLittleEndianInt(
      final byte[] data,
      final int index
  ) {
    return ((data[index] & 0xff)) | ((data[index + 1] & 0xff) << 8) | ((data[index + 2] & 0xff) << 16) | (
        (data[index + 3] & 0xff) << 24);
  }
}

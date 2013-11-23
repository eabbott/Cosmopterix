package org.eabbott.volthll.api;

import com.clearspring.analytics.hash.MurmurHash;

public class HashFunction {
  public int memberHash(Object o) {
    return MurmurHash.hash(o);
  }
  public long keyHash(Object o) {
    return MurmurHash.hash64(o);
  }
}

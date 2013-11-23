package org.eabbott.volthll.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegisterSet
{
    public final static int LOG2_BITS_PER_WORD = 6;
    public final static int REGISTER_SIZE = 5;

    public final int size;

    private final int[] M;

    public RegisterSet(int[] initialValues)
    {
      this.M = initialValues;
      this.size = this.M.length;
    }

    public RegisterSet(int count)
    {
      int bits = count / LOG2_BITS_PER_WORD;

      if (bits == 0)
      {
        this.M = new int[1];
      }
      else if (bits % Integer.SIZE == 0)
      {
        this.M = new int[bits];
      }
      else
      {
        this.M = new int[bits + 1];
      }
      this.size = this.M.length;
    }

    public void set(int position, int value)
    {
        int bucketPos = position / LOG2_BITS_PER_WORD;
        int shift = REGISTER_SIZE * (position - (bucketPos * LOG2_BITS_PER_WORD));
        this.M[bucketPos] = (this.M[bucketPos] & ~(0x1f << shift)) | (value << shift);
    }

    public int get(int position)
    {
        int bucketPos = position / LOG2_BITS_PER_WORD;
        int shift = REGISTER_SIZE * (position - (bucketPos * LOG2_BITS_PER_WORD));
        return (this.M[bucketPos] & (0x1f << shift)) >>> shift;
    }
    
    public boolean updateIfGreater(int position, int value)
    {
        int bucket = position / LOG2_BITS_PER_WORD;
        int shift  = REGISTER_SIZE * (position - (bucket * LOG2_BITS_PER_WORD));
        int mask = 0x1f << shift;

        // Use long to avoid sign issues with the left-most shift
        long curVal = this.M[bucket] & mask;
        long newVal = value << shift;
        if (curVal < newVal) {
            this.M[bucket] = (int)((this.M[bucket] & ~mask) | newVal);
            return true;
        } else {
            return false;
        }
    }

    public void merge(RegisterSet that)
    {
        for (int bucket = 0; bucket < M.length; bucket++)
        {
            int word = 0;
            for (int j = 0; j < LOG2_BITS_PER_WORD; j++)
            {
                int mask = 0x1f << (REGISTER_SIZE * j);

                int thisVal = (this.M[bucket] & mask);
                int thatVal = (that.M[bucket] & mask);
                word |= (thisVal < thatVal) ? thatVal : thisVal;
            }
            this.M[bucket] = word;
        }
    }
    
    public int[] data()
    {
      return M;
    }

    public static int[] byteToInt(byte[] bytes)
    {
      int bitSize = bytes.length / 4;
      int[] bits = new int[bitSize];

      try {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        for (int i = 0; i < bitSize; i++)
        {
            bits[i] = dis.readInt();
        }
      }
      catch (IOException ioe) {
        // not sure how this could happen
      }
      return bits;
    }
    
    public static byte[] intToByte(int[] ints)
    {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (int x : ints) { dos.writeInt(x); }

        return baos.toByteArray();
      }
      catch (IOException ioe) {
        // not sure how this could happen
      }
      return null;
    }
}

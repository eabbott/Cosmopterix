package org.eabbott.volthll.api;


/**
 * This impl is based off o Clearspring's version and altered to
 * utilize VoltDB as the backend for the registerSet data.
 * <p/>
 * Java implementation of HyperLogLog (HLL) algorithm from this paper:
 * <p/>
 * http://algo.inria.fr/flajolet/Publications/FlFuGaMe07.pdf
 * <p/>
 * HLL is an improved version of LogLog that is capable of estimating
 * the cardinality of a set with accuracy = 1.04/sqrt(m) where
 * m = 2^b.  So we can control accuracy vs space usage by increasing
 * or decreasing b.
 * <p/>
 * The main benefit of using HLL over LL is that it only requires 64%
 * of the space that LL does to get the same accuracy.
 * <p/>
 * This implementation implements a single counter.  If a large (millions)
 * number of counters are required you may want to refer to:
 * <p/>
 * http://dsiutils.dsi.unimi.it/
 * <p/>
 * It has a more complex implementation of HLL that supports multiple counters
 * in a single object, drastically reducing the java overhead from creating
 * a large number of objects.
 * <p/>
 * This implementation leveraged a javascript implementation that Yammer has
 * been working on:
 * <p/>
 * https://github.com/yammer/probablyjs
 * <p>
 * Note that this implementation does not include the long range correction function
 * defined in the original paper.  Empirical evidence shows that the correction
 * function causes more harm than good.
 * </p>
 *
 * <p>
 * Users have different motivations to use different types of hashing functions.
 * Rather than try to keep up with all available hash functions and to remove
 * the concern of causing future binary incompatibilities this class allows clients
 * to offer the value in hashed int or long form.  This way clients are free
 * to change their hash function on their own time line.  We recommend using Google's
 * Guava Murmur3_128 implementation as it provides good performance and speed when
 * high precision is required.  In our tests the 32bit MurmurHash function included
 * in this project is faster and produces better results than the 32 bit murmur3
 * implementation google provides.
 * </p>
 */
public class HyperLogLog
{
    private final int log2m;
    private final int count;
    private final double alphaMM;
    private final HashFunction hash;
    private final VoltBackend volt;


    /**
     * Create a new HyperLogLog instance.  The log2m parameter defines the accuracy of
     * the counter.  The larger the log2m the better the accuracy.
     * <p/>
     * accuracy = 1.04/sqrt(2^log2m)
     *
     * @param log2m - the number of bits to use as the basis for the HLL instance
     * @param keyHash - hash function for the key
     * @param memberHash - hash function for the members
     */
    public HyperLogLog(int log2m, HashFunction hash, VoltBackend volt)
    {
        this.log2m = log2m;
        this.hash = hash;
        this.volt = volt;
        this.count = calculateCountFromLog2m(log2m);

        // See the paper.
        switch (log2m)
        {
            case 4:
                alphaMM = 0.673 * count * count;
                break;
            case 5:
                alphaMM = 0.697 * count * count;
                break;
            case 6:
                alphaMM = 0.709 * count * count;
                break;
            default:
                alphaMM = (0.7213 / (1 + 1.079 / count)) * count * count;
        }
    }

    public HashFunction getHashFunction() { return hash; }

    private static int calculateCountFromLog2m(int log2m) {
        return (int) Math.pow(2, log2m);
    }
      
    public void addMembers(final Object key, final Object[] members) throws Exception {
      volt.merge(hash.keyHash(key), generateRegisterSet(members).data());
    }

    public void setMembers(final Object key, final Object[] members) throws Exception {
      volt.set(hash.keyHash(key), generateRegisterSet(members).data());
    }

    public long estimatedCardinality(final Object key) throws Exception {
      int[] registers = volt.get(hash.keyHash(key));
      return registers == null ? 0 : cardinality(new RegisterSet(registers));
    }

    public long estimatedCardinalityFromSecondary(final Object key) throws Exception {
      int[] registers = volt.getFromSecondary(hash.keyHash(key));
      return registers == null ? 0 : cardinality(new RegisterSet(registers));
    }

    // Should only be for testing..
    public void showCardinalities(final long key) throws Exception {
      long primary = cardinality(new RegisterSet(volt.get(key)));
      long secondary = cardinality(new RegisterSet(volt.getFromSecondary(key)));
      System.out.println("key["+ key +"]: "+ primary +", "+ secondary);
    }

    public int getCount() {
      return this.count;
    }

    public static int registerBytesNeededForLog2m(int log2m) {
      RegisterSet registerSet = new RegisterSet(calculateCountFromLog2m(log2m));
      return registerSet.size * 4;
    }

    private RegisterSet generateRegisterSet(final Object[] members) {
      RegisterSet registerSet = new RegisterSet(this.count);
      for (Object member: members) {
        int hashedValue = hash.memberHash(member);
        // j becomes the binary address determined by the first b log2m of x
        // j will be between 0 and 2^log2m
        final int j = hashedValue >>> (Integer.SIZE - log2m);
        final int r = Integer.numberOfLeadingZeros((hashedValue << this.log2m) | (1 << (this.log2m - 1)) + 1) + 1;
        registerSet.updateIfGreater(j, r);
      }
      return registerSet;
    }

    private long cardinality(RegisterSet registerSet)
    {
        double registerSum = 0;
        double zeros = 0.0;
        
        for (int j = 0; j < this.count; j++)
        {
            int val = registerSet.get(j);
            registerSum += 1.0 / (1<<val);
            if (val == 0) {
                zeros++;
            }
        }

        double estimate = alphaMM * (1 / registerSum);

        if (estimate <= (5.0 / 2.0) * this.count)
        {
            // Small Range Estimate
            return Math.round(this.count * Math.log(this.count / zeros));
        }
        else
        {
            return Math.round(estimate);
        }
    }
}
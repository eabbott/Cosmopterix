package org.eabbott.volthll.example;

import java.util.Random;

import org.eabbott.volthll.api.HashFunction;
import org.eabbott.volthll.api.HyperLogLog;
import org.eabbott.volthll.api.VoltHll;


public class Run1 {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    Run1 run1 = new Run1();
    VoltHll volt = new VoltHll("localhost");
    run1.printInformation(volt);
    run1.go(volt);
  }

  public void printInformation(VoltHll volt) throws Exception {
    System.out.println("Current attached voltdb supports max log2m size of "+ volt.findMaxAllowedLog2m());
    volt.printSizesAndAccuracy();
  }

  public void go(VoltHll volt) throws Exception {
    long time = System.currentTimeMillis();
    String key = "this_one";
    HyperLogLog hll = new HyperLogLog(7, new HashFunction(), volt);
    hll.setMembers(key,  new Long[] { Long.valueOf(3L) });
    System.out.println("stuff = "+ hll.estimatedCardinality(key));
    //Random random = new Random(44339L);
    Random random = new Random();
    for (int i=0; i < 60000; i++) {
      Long[] l_arr = new Long[5];
      l_arr[0] = Math.abs(random.nextLong());
      l_arr[1] = Math.abs(random.nextLong());
      l_arr[2] = Math.abs(random.nextLong());
      l_arr[3] = Math.abs(random.nextLong());
      l_arr[4] = Math.abs(random.nextLong());
      hll.addMembers(key,  l_arr);
      if ((i % 1000) == 0 && i != 0) {
        long card = hll.estimatedCardinality(key);
        long percent = ((i*5) - card) * 100 / (i*5);
        System.out.println("added user["+ (i*5) +"] = "+ l_arr[0] +", c="+ hll.estimatedCardinality(key) +", perc=%"+ percent);
      }
    }
    System.out.println("and nothing happened in "+ (System.currentTimeMillis() - time) +"ms");
  }
}

package org.eabbott.volthll.api;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.NoConnectionsException;
//import org.voltdb.client.NullCallback;
import org.voltdb.client.ProcCallException;
import org.voltdb.exceptions.ConstraintFailureException;

public class VoltHll
{

    // Client connection to the underlying VoltDB cluster
    private final Client client;
    //private final String servers;

    // For internal use: NullCallback for "noreply" operations
    //private static final NullCallback nullCallback = new NullCallback();

    /**
     * Creates a new VoltHll instance with a given VoltDB client.
     * @param servers The comma separated list of VoltDB servers in
     * hostname[:port] format that the instance will use.
     */
    public VoltHll(String servers) throws Exception {
        //this.servers = servers;
        client = connect(servers);
    }

    /**
     * Connect to a set of servers in parallel. Each will retry until
     * connection. This call will block until all have connected.
     *
     * @param servers A comma separated list of servers using the hostname:port
     * syntax (where :port is optional).
     * @throws InterruptedException if anything bad happens with the threads.
     */
    Client connect(final String servers) throws InterruptedException {
        ClientConfig clientConfig = new ClientConfig();
        final Client client = ClientFactory.createClient(clientConfig);
        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                public void run() {
                    connectToOneServerWithRetry(client, server);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
        return client;
    }

    /**
     * Connect to a single server with retry. Limited exponential backoff.
     * No timeout. This will run until the process is killed if it's not
     * able to connect.
     *
     * @param server hostname:port or just hostname (hostname can be ip).
     */
    void connectToOneServerWithRetry(Client client, String server) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            }
            catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try { Thread.sleep(sleep); } catch (Exception interruted) {}
                if (sleep < 8000) sleep += sleep;
            }
        }
    }



    /**
     * Closes the VoltHll connection.
     */
    public void close()
    {
        try {
            this.client.drain();
            this.client.close();
        } catch (NoConnectionsException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int[] get(long keyHash) throws Exception {
      VoltTable[] result = this.client.callProcedure("HLL.select", keyHash).getResults();
      return (result.length == 1 && result[0].advanceRow()) ? RegisterSet.byteToInt(result[0].getVarbinary(1)) : null;
    }

    public void delete(long keyHash) throws Exception {
      this.client.callProcedure("HLL.delete", keyHash);
    }

    public void set(long keyHash, int[] registerData) throws Exception {
      this.client.callProcedure("HllSet", keyHash, RegisterSet.intToByte(registerData));
    }

    public void merge(long keyHash, int[] registerData) throws Exception {
      this.client.callProcedure("HllMerge", keyHash, RegisterSet.intToByte(registerData));
    }

    /**
     * Interrogate the attached voltdb to determine the highest log2m supported.
     * @return the largest log2m value supported by the attached voltdb instance
     * @throws Exception Will throw any given voltdb connection exception that occurs.
     */
    public int findMaxAllowedLog2m() throws Exception {
      long hashKey = 1;
      // Pass 1: Find register length from a returned value.
      VoltTable[] result = this.client.callProcedure("HLL.select", hashKey).getResults();
      if (result.length == 1 && result[0].advanceRow()) {
        int registerByteLength = result[0].getVarbinary(1).length;
        
        for (int i=16; i > 0; i--) {
          if (HyperLogLog.registerBytesNeededForLog2m(i) <= registerByteLength) {
            return i;
          }
        }
        return -1;
      }
      // Pass 2: Insert increasingly larger register sizes until rejected.
      for (int i=1; i <= 16; i++) {
        try {
          HyperLogLog hll = new HyperLogLog(i, new HashFunction(), this);
          RegisterSet rs = new RegisterSet(hll.getCount());
          set(hashKey, new int[rs.size]);
        }
        catch (ProcCallException pce) {
          delete(hashKey);
          return i-1;
        }
      }
      delete(hashKey);
      return -1;
    }

    /**
     * This method prints to stdout the sizing and accuracy details for log2m values 1-16.
     */
    public void printSizesAndAccuracy() {
      for (int i=1; i <= 16; i++) {
        double accuracy = 1.04 / (Math.sqrt(Math.pow(2, i))) * 100;
        int bytesNeeded = HyperLogLog.registerBytesNeededForLog2m(i);
        int numBuckets = (int)Math.pow(2, i);
        System.out.println(String.format("log2m = %2d, storage = %5db, Hll 2^%02d = %5d buckets, accuracy = %02.2f%%", i, bytesNeeded, i, numBuckets, accuracy));
      }
    }

    /**
     * Returns underlying performance statistics
     * @returns Full map of performance counters for the underlying VoltDB connection
     */
    public ClientStatsContext getStatistics()
    {
      return this.client.createStatsContext();
    }

    /**
     * Saves performance statistics to a file
     * @param stats The stats instance used to generate the statistics data
     * @param file The path to the file where statistics will be saved
     */
    public void saveStatistics(ClientStats stats, String file) throws IOException
    {
      client.writeSummaryCSV(stats, file);
    }

}

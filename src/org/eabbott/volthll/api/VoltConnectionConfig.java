package org.eabbott.volthll.api;

import java.util.concurrent.CountDownLatch;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.NoConnectionsException;

public class VoltConnectionConfig {
  private boolean primary;
  private String servers;
  //private int port;
  public Client client;
  
  public boolean isConnectionValid() { return true; }
  public boolean isPrimary() { return primary; }
  public void setPrimary(boolean flag) { this.primary = flag; }

  public VoltConnectionConfig(String servers) throws Exception {
      this.servers = servers;
      this.primary = false;
      client = connect(this.servers);
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
   * Closes the Volt connection.
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
}

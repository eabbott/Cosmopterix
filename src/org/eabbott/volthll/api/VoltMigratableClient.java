package org.eabbott.volthll.api;

import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

/**
 * Features
 * - zk for configuration
 * @author eabbott@hubspot.com
 *
 */
public class VoltMigratableClient {
  private VoltConnectionConfig primary = null;
  private VoltConnectionConfig backup = null;

  public synchronized void setPrimary(VoltConnectionConfig config) {
    if (config != null && config.isConnectionValid()) {
      primary = config;
    }
  }

  public synchronized void setBackup(VoltConnectionConfig config) throws Exception {
    if (config != null && !config.isConnectionValid()) {
      throw new Exception("Invalid config passed to backup");
    }
    backup = config;
  }

  // runs on both connections
  public void write(String procedure, Object... params) throws Exception
  {
    Client primary = null;
    Client backup = null;
    synchronized (this) {
      primary = this.primary.client;
      if (this.backup != null) {
        backup = this.backup.client;
      }
    }
 
    primary.callProcedure(procedure, params);
    if (backup != null) {
      backup.callProcedure(procedure, params);
    }
  }
  
  // runs on just primary
  public ClientResponse read(String procedure, Object... params) throws Exception
  {
    Client primary = null;
    synchronized (this) {
      primary = this.primary.client;
    }
    return primary.callProcedure(procedure, params);
  }


}

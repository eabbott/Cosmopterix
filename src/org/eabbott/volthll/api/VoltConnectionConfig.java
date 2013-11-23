package org.eabbott.volthll.api;

import org.voltdb.client.Client;

public class VoltConnectionConfig {
  private boolean enabled;
  private boolean primary;
  private String server;
  private int port;
  public Client client;
  
  public boolean isConnectionValid() { return true; }
  
}

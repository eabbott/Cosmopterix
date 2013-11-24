package org.eabbott.volthll.api;

import org.voltdb.client.ClientResponse;

/**
 * This client was broken out to support two simultaneous backends for use
 * during instance migration. The expected usage is an external resource
 * such as zookeeper would trigger the connection configuration to be updated on the fly.
 */
public class VoltMigratableClient {
  public final static String LEFT = "left";
  public final static String RIGHT = "right";
  private VoltConnectionConfig left = null;
  private VoltConnectionConfig right = null;

  public synchronized void setLeft(VoltConnectionConfig config) throws Exception {
    if (config != null && !config.isConnectionValid()) {
      throw new Exception("Invalid config passed to left");
    }
    left = config;
  }

  public synchronized void setRight(VoltConnectionConfig config) throws Exception {
    if (config != null && !config.isConnectionValid()) {
      throw new Exception("Invalid config passed to right");
    }
    right = config;
  }

  public synchronized void setPrimary(String side) throws Exception {
    if (LEFT.equals(side)) {
      if (this.left == null) {
        throw new Exception("Cannot set primary to unset left connection");
      } else if (!this.left.isConnectionValid()) {
        throw new Exception("Cannot set primary to invalid left connection");
      }
      this.left.setPrimary(true);
      if (this.right != null) { this.right.setPrimary(false); }
    } else if (RIGHT.equals(side)) {
      if (this.right == null) {
        throw new Exception("Cannot set primary to unset right connection");
      } else if (!this.right.isConnectionValid()) {
        throw new Exception("Cannot set primary to invalid right connection");
      }
      this.right.setPrimary(true);
      if (this.left != null) { this.left.setPrimary(false); }
    }
  }

  public VoltConnectionConfig getPrimary() {
    return left != null && left.isPrimary() ? left : right;
  }

  public VoltConnectionConfig getSecondary() {
    return left != null && left.isPrimary() ? right : left;
  }

  // runs on both connections
  public void write(String procedure, Object... params) throws Exception
  {
    VoltConnectionConfig primary = null;
    VoltConnectionConfig secondary = null;
    synchronized (this) {
      primary = getPrimary();
      secondary = getSecondary();
    }
 
    if (primary != null) {
      primary.client.callProcedure(procedure, params);
    }
    if (secondary != null) {
      secondary.client.callProcedure(procedure, params);
    }
  }
  
  // runs on just primary
  public ClientResponse read(String procedure, Object... params) throws Exception
  {
    VoltConnectionConfig primary = null;
    synchronized (this) {
      primary = getPrimary();
    }
    return primary.client.callProcedure(procedure, params);
  }

  // To be used during migrations, write only to the secondary client
  public void writeSecondary(String procedure, Object... params) throws Exception
  {
    VoltConnectionConfig secondary = null;
    synchronized (this) {
      secondary = getSecondary();
    }
 
    if (secondary != null) {
      secondary.client.callProcedure(procedure, params);
    }
  }

  // To be used for testing and/or as a read slave?
  public ClientResponse readSecondary(String procedure, Object... params) throws Exception
  {
    VoltConnectionConfig secondary = null;
    synchronized (this) {
      secondary = getSecondary();
    }

    if (secondary != null) {
      return secondary.client.callProcedure(procedure, params);
    }
    return null;
  }

  public void close() throws Exception {
    VoltConnectionConfig primary = null;
    VoltConnectionConfig secondary = null;
    synchronized (this) {
      primary = getPrimary();
      secondary = getSecondary();
    }
 
    if (primary != null) {
      primary.client.close();
    }
    if (secondary != null) {
      secondary.client.close();
    }
  }
}

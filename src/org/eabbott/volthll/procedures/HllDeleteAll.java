package org.eabbott.volthll.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HllDeleteAll extends VoltProcedure
{
  private final SQLStmt DELETE_ALL = new SQLStmt("delete from hll");

  public VoltTable[] run() throws VoltAbortException
  {
    voltQueueSQL(DELETE_ALL);
    return voltExecuteSQL();
  }
}

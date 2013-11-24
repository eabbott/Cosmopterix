package org.eabbott.volthll.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HllGetAll extends VoltProcedure
{
  private final SQLStmt SELECT_ALL = new SQLStmt("select hash, registers from hll");

  public VoltTable[] run() throws VoltAbortException
  {
    voltQueueSQL(SELECT_ALL);
    return voltExecuteSQL();
  }
}

package org.eabbott.volthll.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class HllSet extends VoltProcedure
{
  private final SQLStmt SELECT_REGISTERS = new SQLStmt("select registers from hll where hash=?");
  private final SQLStmt INSERT = new SQLStmt("insert into hll (hash, registers) values (?, ?)");
  private final SQLStmt UPDATE_REGISTERS = new SQLStmt("update hll set registers=? where hash=?");

  public long run(long key, byte[] registers) throws VoltAbortException
  {
    voltQueueSQL(SELECT_REGISTERS, key);
    if (voltExecuteSQL()[0].getRowCount() == 0) {
      voltQueueSQL(INSERT, key, registers);
      voltExecuteSQL(true);
    } else {
      voltQueueSQL(UPDATE_REGISTERS, registers, key);
      voltExecuteSQL(true);
    }
    return 1L;
  }
}
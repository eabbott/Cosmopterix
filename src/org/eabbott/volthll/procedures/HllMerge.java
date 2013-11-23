package org.eabbott.volthll.procedures;

import org.eabbott.volthll.api.RegisterSet;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HllMerge extends VoltProcedure
{
  private final SQLStmt SELECT_REGISTERS = new SQLStmt("select registers from hll where hash=?");
  private final SQLStmt INSERT = new SQLStmt("insert into hll (hash, registers) values (?, ?)");
  private final SQLStmt UPDATE_REGISTERS = new SQLStmt("update hll set registers=? where hash=?");

  public long run(long key, byte[] registers) throws VoltAbortException
  {
    voltQueueSQL(SELECT_REGISTERS, key);
    VoltTable results = voltExecuteSQL()[0];
    if (results.getRowCount() == 0) {
      voltQueueSQL(INSERT, key, registers);
      voltExecuteSQL(true);
    } else {
      results.advanceRow();
      byte[] merged = merge(results.getVarbinary(0), registers);
      voltQueueSQL(UPDATE_REGISTERS, merged, key);
      voltExecuteSQL(true);
    }
    return 1L;
  }
  
  private byte[] merge(byte[] oldData, byte[] newData) {
    RegisterSet oldRegister = new RegisterSet(RegisterSet.byteToInt(oldData));
    RegisterSet newRegister = new RegisterSet(RegisterSet.byteToInt(newData));
    
    oldRegister.merge(newRegister);
    return RegisterSet.intToByte(oldRegister.data());
  }
}

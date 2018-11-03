package Server.LockManager;

import java.rmi.RemoteException;
import java.util.Vector;
import java.util.Hashtable;
import Server.Interface.IResourceManager;

public class TransactionManager {
    private Hashtable<Integer, Vector<IResourceManager>> xid_rm;
    private static int num_transaction = 0;

    public TransactionManager()
    {
        xid_rm = new Hashtable<>();
    }

    public int start()
    {
        num_transaction ++;
        xid_rm.put(num_transaction, new Vector<IResourceManager>());
        return num_transaction;
    }

    public boolean commit(int xid)
    {
        try {
            for (IResourceManager rm : xid_rm.get(xid)) {
                rm.commit(xid);
            }
            xid_rm.remove(xid);
            return true;
        } catch(RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean abort(int xid)
    {
        try {
            for (IResourceManager rm : xid_rm.get(xid)) {
                rm.abort(xid);
            }
            xid_rm.remove(xid);
            return true;
        } catch(RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean tranactionExist(int xid)
    {
        if (xid_rm.containsKey(xid))
            return true;
        else
            return false;
    }
}

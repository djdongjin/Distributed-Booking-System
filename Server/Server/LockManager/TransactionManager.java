package Server.LockManager;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;
import Server.Interface.IResourceManager;

public class TransactionManager {
    private Hashtable<Integer, Vector<IResourceManager>> xid_rm;
    private Hashtable<Integer, Long> xid_time;
    private static int num_transaction = 0;
    private static long MAX_EXIST_TIME = 10000;


    public TransactionManager()
    {
        xid_rm = new Hashtable<>();
        xid_time = new Hashtable<Integer, Long>();
    }

    public int start()
    {
        num_transaction ++;
        xid_rm.put(num_transaction, new Vector<IResourceManager>());
        xid_time.put(num_transaction, System.currentTimeMillis());
        return num_transaction;
    }

    public boolean commit(int xid)
    {
        try {
            for (IResourceManager rm : xid_rm.get(xid)) {
                rm.commit(xid);
            }
            xid_rm.remove(xid);
            xid_time.remove(xid);
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
            xid_time.remove(xid);
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

    public Set<Integer> activeTransaction()
    {
        return xid_time.keySet();
    }

    public boolean checkTime(int xid)
    {
        if (System.currentTimeMillis() - xid_time.get(xid) > MAX_EXIST_TIME)
            return false;
        else
            return true;
    }

    public void resetTime(int xid)
    {
        xid_time.put(xid, System.currentTimeMillis());
    }
}

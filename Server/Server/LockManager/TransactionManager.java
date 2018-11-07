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
    private static long MAX_EXIST_TIME = 100000;


    public TransactionManager()
    {
        xid_rm = new Hashtable<>();
        xid_time = new Hashtable<Integer, Long>();
    }

    public int start()
    {
        num_transaction ++;
        synchronized (xid_rm) {
            xid_rm.put(num_transaction, new Vector<IResourceManager>());
        }
        synchronized (xid_time) {
            xid_time.put(num_transaction, System.currentTimeMillis());
        }
        return num_transaction;
    }

    public void addRM(int xid, IResourceManager rm)
    {
        Vector<IResourceManager> tmp = xid_rm.get(xid);
        if (!tmp.contains(rm))
        {
            tmp.add(rm);
            xid_rm.put(xid, tmp);
        }
//        try {
//            for (IResourceManager xxx : tmp)
//                System.out.print(xxx.getName());
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    public boolean commit(int xid)
    {
        try {
            for (IResourceManager rm : xid_rm.get(xid)) {
                rm.commit(xid);
            }
            synchronized (xid_rm) {
                xid_rm.remove(xid);
            }
            synchronized (xid_rm) {
                xid_time.remove(xid);
            }
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
            synchronized (xid_rm) {
                xid_time.remove(xid);
            }
            synchronized (xid_rm) {
                xid_rm.remove(xid);
            }
            return true;
        } catch(RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean transactionExist(int xid)
    {
        if (xid_rm.containsKey(xid))
            return true;
        else
            return false;
    }

    public Set<Integer> activeTransaction()
    {
//        for (Integer s: xid_time.keySet())
//        {
//            System.out.print(s);
//        }
//        System.out.println();
        return xid_time.keySet();
    }

    public boolean checkTime(int xid)
    {
//        System.out.println(xid_time.get(xid));
        if (System.currentTimeMillis() - xid_time.get(xid) > MAX_EXIST_TIME)
            return false;
        else
            return true;
    }

    public void resetTime(int xid)
    {
        synchronized (xid_time) {
            xid_time.put(xid, System.currentTimeMillis());
        }
    }
}

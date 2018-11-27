package Server.LockManager;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;

import Server.Common.LogItem;
import Server.Interface.IResourceManager;

public class TransactionManager {
    private Hashtable<Integer, Vector<IResourceManager>> xid_rm;
    private Hashtable<Integer, Long> xid_time;
    private static int num_transaction = 0;
    private static long MAX_EXIST_TIME = 100000;
    private static long MAX_VOTING_TIME = 80000;
    private String mid_name;


    public TransactionManager(String name)
    {
        xid_rm = new Hashtable<>();
        xid_time = new Hashtable<Integer, Long>();
        mid_name = name;
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
    }

    public boolean twoPC(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        HashMap<IResourceManager, Integer> vote = new HashMap<>();
        int num_rm = xid_rm.get(xid).size();

        try {
            // write start-2PC record in log
            writeLog(new LogItem(xid, "start-2PC"));
            // send VOTE-REQ request
            // TODO: may timeout when waiting for all votes
            long begin_time = System.currentTimeMillis();
            Thread vote_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (IResourceManager rm : vote.keySet()) {
                        try {
                            int res = rm.prepare(xid) ? 1: 0;
                            vote.put(rm, res);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            vote_thread.start();

            while (true) {
                if (System.currentTimeMillis() - begin_time > MAX_VOTING_TIME)
                {
                    // timeout
                    vote_thread.interrupt();
                    writeLog(new LogItem(xid, "ABORT"));
                    for (IResourceManager rm: vote.keySet())
                        if (vote.get(rm) == 1)
                            rm.abort(xid);
                    return false;
                }
                // all rm votes
                if (vote.size() == num_rm) {
                    int vote_res = 0;
                    for (Integer i: vote.values())
                        vote_res += i;
                    if (vote_res == num_rm) {
                        // all vote YES
                        writeLog(new LogItem(xid, "COMMIT"));
                        commit(xid);
                        return true;
                    } else {
                        // some votes NO
                        writeLog(new LogItem(xid, "ABORT"));
                        for (IResourceManager rm: vote.keySet())
                            if (vote.get(rm) == 1)
                                rm.abort(xid);
                        return false;
                    }
                }
            }
        } catch(RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
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

    public boolean abort(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
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

    public Hashtable<Integer, Vector<IResourceManager>> getRMTable()
    {
        return xid_rm;
    }

    public void setRMTable(Hashtable<Integer, Vector<IResourceManager>> xidrm)
    {
        xid_rm = xidrm;
    }

    public Hashtable<Integer, Long> getTimeTable()
    {
        return xid_time;
    }

    public void setTimeTable(Hashtable<Integer, Long> xidtime)
    {
        xid_time = xidtime;
    }

    public void writeLog(LogItem lg)
    {
        System.out.println(">>>LOG: <xid,info>:" + lg.xid + ", " + lg.info);
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(mid_name + ".log"));
            out.writeObject(lg);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

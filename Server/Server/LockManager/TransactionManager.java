package Server.LockManager;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

import Server.Common.LogItem;
import Server.Interface.IResourceManager;

public class TransactionManager {
    private Hashtable<Integer, Vector<String>> xid_rm;
    private Hashtable<Integer, Long> xid_time;
    private static int num_transaction = 0;
    private static long MAX_EXIST_TIME = 100000;
    private static long MAX_VOTING_TIME = 80000;
    private String mid_name;

    private Hashtable<String, IResourceManager> name_RM = new Hashtable<> ();

    // TODO: Crash mode 8 in Middleware recovery
    public ArrayList<Boolean> crash_middle = new ArrayList<>(9);

    public TransactionManager(String name) {
        xid_rm = new Hashtable<>();
        xid_time = new Hashtable<Integer, Long>();
        mid_name = name;
        for (int i = 0; i < crash_middle.size(); i++)
            crash_middle.set(i, false);
    }

    public int start() {
        num_transaction++;
        writeLog(new LogItem(num_transaction, "INIT"));
        synchronized (xid_rm) {
            xid_rm.put(num_transaction, new Vector<String>());
        }
        synchronized (xid_time) {
            xid_time.put(num_transaction, System.currentTimeMillis());
        }
        return num_transaction;
    }

    public void addRM(int xid, IResourceManager rm) throws RemoteException {
        String nm = rm.getName();
        updateRM(nm, rm);
        Vector<String> tmp = xid_rm.get(xid);
        if (!tmp.contains(nm)) {
            tmp.add(nm);
            xid_rm.put(xid, tmp);
        }
    }

    public void updateRM(String name, IResourceManager rm) {
        name_RM.put(name, rm);
    }

    public boolean twoPC(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        HashMap<IResourceManager, Integer> vote = new HashMap<>();
        int num_rm = xid_rm.get(xid).size();

        try {
            // write start-2PC record in log
            writeLog(new LogItem(xid, "start-2PC"));
            // send VOTE-REQ request
            // TODO: may timeout when waiting for all votes
            long begin_time = System.currentTimeMillis();

            // Crash mode 1
            if (crash_middle.get(1)) {
                System.out.println("crash mode 1: crash after logging start-2PC and before sending any vote request");
                System.exit(1);
            }

            Thread vote_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (IResourceManager rm : vote.keySet()) {
                        Thread rm_prepare = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    int res = rm.prepare(xid) ? 1 : 0;
                                    vote.put(rm, res);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        rm_prepare.start();
                    }

                    // Crash mode 2
                    if (crash_middle.get(2) && vote.size() == 0) {
                        System.out.println(
                                "crash mode 2: crash after sending all vote requests and before receiving any replies");
                        System.exit(1);
                    }
                }
            });
            vote_thread.start();

            while (true) {
                if (System.currentTimeMillis() - begin_time > MAX_VOTING_TIME) {
                    // timeout
                    vote_thread.interrupt();
                    writeLog(new LogItem(xid, "ABORT"));
                    for (IResourceManager rm : vote.keySet())
                        if (vote.get(rm) == 1) {
                            rm.abort(xid);
                        }
                    synchronized (xid_rm) {
                        xid_time.remove(xid);
                    }
                    synchronized (xid_rm) {
                        xid_rm.remove(xid);
                    }
                    return false;
                }

                // Crash mode 3
                if (crash_middle.get(3) && vote.size() >= 1) {
                    System.out.println("crash mode 3: crash after receiving some replies but not all");
                    System.exit(1);
                }

                // all rm votes
                if (vote.size() == num_rm) {

                    // Crash mode 4
                    if (crash_middle.get(4)) {
                        System.out.println("crash mode 4: crash after receiving all replies but before deciding");
                        System.exit(1);
                    }

                    int vote_res = 0;
                    for (Integer i : vote.values())
                        vote_res += i;
                    if (vote_res == num_rm) {
                        // all vote YES
                        writeLog(new LogItem(xid, "COMMIT"));

                        // Crash mode 5
                        if (crash_middle.get(5)) {
                            System.out.println("crash mode 5: crash after deciding but before sending decision");
                            System.exit(1);
                        }

                        commit(xid);
                        return true;
                    } else {
                        // some votes NO
                        writeLog(new LogItem(xid, "ABORT"));

                        // Crash mode 5
                        if (crash_middle.get(5)) {
                            System.out.println("crash mode 5: crash after deciding but before sending decision");
                            System.exit(1);
                        }

                        for (IResourceManager rm : vote.keySet())
                            if (vote.get(rm) == 1) {
                                rm.abort(xid);
                                // Crash mode 6
                                if (crash_middle.get(6)) {
                                    System.out.println("crash mode 6: crash after sending some but not all decisions");
                                    System.exit(1);
                                }
                            }

                        // Crash mode 7
                        if (crash_middle.get(7)) {
                            System.out.println("crash mode 7: crash after sending all decisions");
                            System.exit(1);
                        }

                        return false;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        try {
            for (String rm_name : xid_rm.get(xid)) {
                name_RM.get(rm_name).commit(xid);

                // Crash mode 6
                if (crash_middle.get(6)) {
                    System.out.println("crash mode 6: crash after sending some but not all decisions");
                    System.exit(1);
                }
            }

            // Crash mode 7
            if (crash_middle.get(7)) {
                System.out.println("crash mode 7: crash after sending all decisions");
                System.exit(1);
            }

            synchronized (xid_rm) {
                xid_rm.remove(xid);
            }
            synchronized (xid_rm) {
                xid_time.remove(xid);
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean abort(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        try {
            for (String rm_name : xid_rm.get(xid)) {
                name_RM.get(rm_name).abort(xid);
            }
            synchronized (xid_rm) {
                xid_time.remove(xid);
            }
            synchronized (xid_rm) {
                xid_rm.remove(xid);
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        writeLog(new LogItem(xid, "ABORT"));
        return true;
    }

    public boolean transactionExist(int xid) {
        if (xid_rm.containsKey(xid))
            return true;
        else
            return false;
    }

    public Set<Integer> activeTransaction() {
        return xid_time.keySet();
    }

    public boolean checkTime(int xid) {
        // System.out.println(xid_time.get(xid));
        if (System.currentTimeMillis() - xid_time.get(xid) > MAX_EXIST_TIME)
            return false;
        else
            return true;
    }

    public void resetTime(int xid) {
        synchronized (xid_time) {
            xid_time.put(xid, System.currentTimeMillis());
        }
    }

    public Hashtable<Integer, Vector<String>> getRMTable() {
        return xid_rm;
    }

    public void setRMTable(Hashtable<Integer, Vector<String>> xidrm) {
        xid_rm = xidrm;
    }

//    public Hashtable<Integer, Long> getTimeTable() {
//        return xid_time;
//    }
//
//    public void setTimeTable(Hashtable<Integer, Long> xidtime) {
//        xid_time = xidtime;
//    }

    public void writeLog(LogItem lg) {
        System.out.println(">>>LOG: <xid,info>:" + lg.xid + ", " + lg.info);
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(mid_name + ".log", true));
            out.writeObject(lg);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetCrashes() {
        for (int i = 0; i < crash_middle.size(); i++)
            crash_middle.set(i, false);
    }

    public void crashMiddleware(int mode) {
        crash_middle.set(mode, true);
    }
}

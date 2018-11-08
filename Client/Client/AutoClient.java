package Client;

import Server.Interface.*;
import Server.LockManager.InvalidTransactionException;
import Server.LockManager.TransactionAbortedException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.NotBoundException;

import java.util.*;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;

public class AutoClient extends Client {

    public enum City {
        YUL, YYZ, YOW, YVR, YQB, JFK, EWR, LGA, BOS, PHL, IAD, ATL, MIA, IAH, MDW, ORD, DTW, SEA, SFO, LAX, PEK, PVG,
        SHA, CAN, HKG;
    }

    IResourceManager m_resourceManager = null;
    Vector<Integer> xids = null;

    private static int CustomerNum = 10;
    private static int CityNum = City.values().length;
    private static int iterationNum = 100;
    private long startTime, endTime;
    private Vector<Long> responseTime = new Vector<Long>();
    private Vector<Float> initTime = new Vector<Float>();
    private Vector<Float> queryTime = new Vector<Float>();
    private Vector<Float> reserveTime = new Vector<Float>();
    private Vector<Float> commitTime = new Vector<Float>();
    private Vector<Long> txTime = new Vector<Long>();
    private Random rand = new Random();

    private static boolean MultiMachine = false;
    private static boolean MultiRM = false;
    private static long TxInterval = 0;
    private static int ClientNum = 1;

    private static String s_serverHost = "localhost";
    private static int s_serverPort = 1099;
    private static String s_serverName = "RMIMiddleware";

    // TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
    private static String s_rmiPrefix = "group14";

    public AutoClient() {
        super();
        xids = new Vector<Integer>();
    }

    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            s_serverName = args[1];
        }
        if (args.length > 2) {
            if (args[2].toLowerCase().equals("y")) {
                MultiMachine = true;
            }
        }
        if (args.length > 3) {
            if (!MultiMachine) {
                if (args[3].toLowerCase().equals("y")) {
                    MultiRM = true;
                }
            } else {
                TxInterval = Long.parseLong(args[3]);
                if (args.length > 4) {
                    ClientNum = Integer.parseInt(args[4]);
                }
            }
        }
        if (args.length > 5) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
                    + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            AutoClient atc = new AutoClient();
            atc.connectServer();
            atc.start(MultiMachine, MultiRM, TxInterval, ClientNum);
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void connectServer() {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix
                            + name + "]");
                    break;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/"
                                + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start(boolean multiMachine, boolean multiRM, long txInterval, int clientNum) {
        // Prepare for reading commands
        System.out.println();
        System.out.println("press any key to start...");

        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            stdin.readLine();
        } catch (IOException io) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m" + io.getLocalizedMessage());
            io.printStackTrace();
            System.exit(1);
        }

        try {
            if (!multiMachine) {

                if (!preparation()) {
                    System.out.println("preparation failed, exit");
                    return;
                }

                System.out.println("begin to test...");
                for (int i = 0; i < iterationNum; i++) {
                    startTime = System.currentTimeMillis();
                    responseTime = itinerary(multiRM, 10000 + i % CustomerNum, 100 + i % CityNum,
                            City.values()[i % CityNum].name());
                    txTime.add(System.currentTimeMillis() - startTime);
                    if (responseTime.size() == 1) {
                        System.out.println("itinerary failed");
                        return;
                    } else {
                        initTime.add((float) responseTime.get(0));
                        queryTime.add((float) ((responseTime.get(1) + responseTime.get(2) + responseTime.get(4)
                                + responseTime.get(6)) / 4));
                        reserveTime
                                .add((float) ((responseTime.get(3) + responseTime.get(5) + responseTime.get(7)) / 3));
                        commitTime.add((float) responseTime.get(8));
                    }
                }

                System.out.println("itinerary finished");

                float avgInitTime = 0, avgQueryTime = 0, avgReserveTime = 0, avgCommitTime = 0, avgTxTime = 0;
                for (int i = 0; i < iterationNum; i++) {
                    avgInitTime += initTime.get(i);
                    avgQueryTime += queryTime.get(i);
                    avgReserveTime += reserveTime.get(i);
                    avgCommitTime += commitTime.get(i);
                    avgTxTime += txTime.get(i);
                }
                avgInitTime /= iterationNum;
                avgQueryTime /= iterationNum;
                avgReserveTime /= iterationNum;
                avgCommitTime /= iterationNum;
                avgTxTime /= iterationNum;

                System.out.println("start time for every itinerary are: ");
                for (float time : initTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("query time for every itinerary are: ");
                for (float time : queryTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("reserve time for every itinerary are: ");
                for (float time : reserveTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("commit time for every itinerary are: ");
                for (float time : commitTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("transaction time for every itinerary are: ");
                for (long time : txTime) {
                    System.out.print(Long.toString(time) + " ");
                }
                System.out.println();

                System.out.println("average start time: " + Float.toString(avgInitTime));
                System.out.println("average query time: " + Float.toString(avgQueryTime));
                System.out.println("average reserve time: " + Float.toString(avgReserveTime));
                System.out.println("average commit time: " + Float.toString(avgCommitTime));
                System.out.println("average transaction time: " + Float.toString(avgTxTime));

            } else {
                if (clientNum == 1) {
                    startTime = System.currentTimeMillis();
                    if (!preparation()) {
                        System.out.println("preparation failed, exit");
                        return;
                    }

                    endTime = System.currentTimeMillis();
                    if (endTime - startTime < 5000) {
                        Thread.sleep(5000 + startTime - endTime);
                    }
                } else {
                    Thread.sleep(5000);
                }

                System.out.println("begin to test...");
                for (int i = 0; i < iterationNum; i++) {
                    startTime = System.currentTimeMillis();
                    responseTime = itinerary(true, 10000 + (i * clientNum) % CustomerNum,
                            100 + (i * clientNum) % CityNum, City.values()[(i * clientNum) % CityNum].name());
                    txTime.add(System.currentTimeMillis() - startTime);
                    if (reserveTime.size() == 1) {
                        System.out.println("itinerary failed");
                        return;
                    } else {
                        initTime.add((float) responseTime.get(0));
                        queryTime.add((float) ((responseTime.get(1) + responseTime.get(2) + responseTime.get(4)
                                + responseTime.get(6)) / 4));
                        reserveTime
                                .add((float) ((responseTime.get(3) + responseTime.get(5) + responseTime.get(7)) / 3));
                        commitTime.add((float) responseTime.get(8));
                    }

                    endTime = System.currentTimeMillis();
                    if (endTime - startTime < txInterval) {
                        long noise = (long) (rand.nextInt((int) (txInterval / 10) + 1) - (txInterval / 20));
                        Thread.sleep(txInterval + startTime - endTime + noise);
                    }
                }

                System.out.println("itinerary finished");

                float avgInitTime = 0, avgQueryTime = 0, avgReserveTime = 0, avgCommitTime = 0, avgTxTime = 0;
                for (int i = 0; i < iterationNum; i++) {
                    avgInitTime += initTime.get(i);
                    avgQueryTime += queryTime.get(i);
                    avgReserveTime += reserveTime.get(i);
                    avgCommitTime += commitTime.get(i);
                    avgTxTime += txTime.get(i);
                }
                avgInitTime /= iterationNum;
                avgQueryTime /= iterationNum;
                avgReserveTime /= iterationNum;
                avgCommitTime /= iterationNum;
                avgTxTime /= iterationNum;

                System.out.println("start time for every itinerary are: ");
                for (float time : initTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("query time for every itinerary are: ");
                for (float time : queryTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("reserve time for every itinerary are: ");
                for (float time : reserveTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("commit time for every itinerary are: ");
                for (float time : commitTime) {
                    System.out.print(Float.toString(time) + " ");
                }
                System.out.println();
                System.out.println("transaction time for every itinerary are: ");
                for (long time : txTime) {
                    System.out.print(Long.toString(time) + " ");
                }
                System.out.println();

                System.out.println("average start time: " + Float.toString(avgInitTime));
                System.out.println("average query time: " + Float.toString(avgQueryTime));
                System.out.println("average reserve time: " + Float.toString(avgReserveTime));
                System.out.println("average commit time: " + Float.toString(avgCommitTime));
                System.out.println("average transaction time: " + Float.toString(avgTxTime));
            }

        } catch (TransactionAbortedException e) {
            int id = e.getXId();
            if (xids.contains(id))
                xids.removeElement(id);
            System.out.println("Transaction [" + id + "] is aborted.");
        } catch (InvalidTransactionException e) {
            int id = e.getXId();
            if (xids.contains(id))
                xids.removeElement(id);
            System.out.println("Transaction [" + id + "] doesn't exist.");
        } catch (RemoteException e) {
            System.out.println("Remote exception");
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Sleep interrupted");
            System.exit(1);
        }

    }

    public boolean preparation() throws RemoteException, TransactionAbortedException, InvalidTransactionException {

        int xid = m_resourceManager.start();

        for (int i = 0; i < CustomerNum; i++) {
            if (m_resourceManager.newCustomer(xid, 10000 + i)) {
                System.out.println("Add customer ID: " + Integer.toString(10000 + i));
            } else {
                System.out.println("Customer could not be added");
                m_resourceManager.abort(xid);
                return false;
            }
        }

        for (int i = 0; i < CityNum; i++) {
            if (m_resourceManager.addFlight(xid, 100 + i, 200, 500 + i * 10)
                    && m_resourceManager.addCars(xid, City.values()[i].name(), 50, 300 + i * 10)
                    && m_resourceManager.addRooms(xid, City.values()[i].name(), 100, 200 + i * 10)) {
                continue;
            } else {
                System.out.println("preperation failed. Flights, cars or rooms cannot be added");
                m_resourceManager.abort(xid);
                return false;
            }
        }
        m_resourceManager.commit(xid);

        System.out.println("preparation finished");
        return true;
    }

    public Vector<Long> itinerary(boolean multiRM, int customerID, int flightNum, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Vector<Long> responseTime = new Vector<Long>();
        long startTime;

        startTime = System.currentTimeMillis();
        int xid = m_resourceManager.start();
        responseTime.add(System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        m_resourceManager.queryCustomerInfo(xid, customerID);
        responseTime.add(System.currentTimeMillis() - startTime);
        if (!multiRM) {
            for (int i = 0; i < 3; i++) {
                startTime = System.currentTimeMillis();
                int seatNum = m_resourceManager.queryFlight(xid, flightNum);
                responseTime.add(System.currentTimeMillis() - startTime);
                if (seatNum > 0) {
                    startTime = System.currentTimeMillis();
                    boolean reserveRes = m_resourceManager.reserveFlight(xid, customerID, flightNum);
                    responseTime.add(System.currentTimeMillis() - startTime);
                    if (!reserveRes) {
                        m_resourceManager.abort(xid);
                        System.out.println("failed to reserve flight " + Integer.toString(flightNum)
                                + " in transaction " + Integer.toString(xid));
                        return new Vector<Long>(-1);
                    }
                } else {
                    m_resourceManager.abort(xid);
                    System.out.println("failed to reserve flight " + Integer.toString(flightNum) + " in transaction "
                            + Integer.toString(xid) + ", not enough seats");
                    return new Vector<Long>(-1);
                }
            }
        } else {
            startTime = System.currentTimeMillis();
            int seatNum = m_resourceManager.queryFlight(xid, flightNum);
            responseTime.add(System.currentTimeMillis() - startTime);
            if (seatNum > 0) {
                startTime = System.currentTimeMillis();
                boolean reserveRes = m_resourceManager.reserveFlight(xid, customerID, flightNum);
                responseTime.add(System.currentTimeMillis() - startTime);
                if (!reserveRes) {
                    m_resourceManager.abort(xid);
                    System.out.println("failed to reserve flight " + Integer.toString(flightNum) + " in transaction "
                            + Integer.toString(xid));
                    return new Vector<Long>(-1);
                }
            } else {
                m_resourceManager.abort(xid);
                System.out.println("failed to reserve flight " + Integer.toString(flightNum) + " in transaction "
                        + Integer.toString(xid) + ", not enough seats");
                return new Vector<Long>(-1);
            }

            startTime = System.currentTimeMillis();
            int carNum = m_resourceManager.queryCars(xid, location);
            responseTime.add(System.currentTimeMillis() - startTime);
            if (carNum > 0) {
                startTime = System.currentTimeMillis();
                boolean reserveRes = m_resourceManager.reserveCar(xid, customerID, location);
                responseTime.add(System.currentTimeMillis() - startTime);
                if (!reserveRes) {
                    m_resourceManager.abort(xid);
                    System.out.println(
                            "failed to reserve car in " + location + " in transaction " + Integer.toString(xid));
                    return new Vector<Long>(-1);
                }
            } else {
                m_resourceManager.abort(xid);
                System.out.println("failed to reserve car in " + location + " in transaction " + Integer.toString(xid)
                        + ", not enough cars");
                return new Vector<Long>(-1);
            }

            startTime = System.currentTimeMillis();
            int RoomNum = m_resourceManager.queryRooms(xid, location);
            responseTime.add(System.currentTimeMillis() - startTime);
            if (RoomNum > 0) {
                startTime = System.currentTimeMillis();
                boolean reserveRes = m_resourceManager.reserveRoom(xid, customerID, location);
                responseTime.add(System.currentTimeMillis() - startTime);
                if (!reserveRes) {
                    m_resourceManager.abort(xid);
                    System.out.println(
                            "failed to reserve room in " + location + " in transaction " + Integer.toString(xid));
                    return new Vector<Long>(-1);
                }
            } else {
                m_resourceManager.abort(xid);
                System.out.println("failed to reserve room in " + location + " in transaction " + Integer.toString(xid)
                        + ", not enough seats");
                return new Vector<Long>(-1);
            }
        }

        startTime = System.currentTimeMillis();
        m_resourceManager.commit(xid);
        responseTime.add(System.currentTimeMillis() - startTime);
        return responseTime;
    }

}

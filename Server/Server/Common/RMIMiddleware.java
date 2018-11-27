package Server.Common;

import Server.Interface.*;
import Server.LockManager.*;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

public class RMIMiddleware extends ResourceManager {

    private static String s_serverName = "RMIMiddleware";
    private static String s_rmiPrefix = "group14";

    private static String s_flightHost = "localhost";
    private static int s_flightPort = 1099;
    private static String s_flightName = "Flight";

    private static String s_carHost = "localhost";
    private static int s_carPort = 1099;
    private static String s_carName = "Car";

    private static String s_roomHost = "localhost";
    private static int s_roomPort = 1099;
    private static String s_roomName = "Room";

    protected LockManager lm = new LockManager();
    protected TransactionManager tm = null;

    private IResourceManager flightRM = null;
    private IResourceManager carRM = null;
    private IResourceManager roomRM = null;

    public static void main(String[] args)
    {
        // RMIMiddleware server name
        // Given all 6 server arguments or all use default values.
        if (args.length == 7 || args.length == 1 || args.length == 0)
        {
            if (args.length > 0){
                s_serverName = args[0];
            }
            if (args.length == 7)
            {
                s_flightHost = args[1];
                s_flightName = args[2];
                s_carHost = args[3];
                s_carName = args[4];
                s_roomHost = args[5];
                s_roomName = args[6];
            }
        }
        else
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // Crate a new Server entry and get 3 rmiRegistry.
        try {
            RMIMiddleware middle = new RMIMiddleware("RMIMiddleware");
            try {
                middle.connectServer("flight", s_flightHost, s_flightPort, s_flightName);
                middle.connectServer("car", s_carHost, s_carPort, s_carName);
                middle.connectServer("room", s_roomHost, s_roomPort, s_roomName);
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }

            // Dynamically generate the stub (client proxy)
            IResourceManager middlewareServer = (IResourceManager) UnicastRemoteObject.exportObject(middle, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(1099);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + s_serverName, middlewareServer);

            // time-to-live mechanism
            Thread time_to_live = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        middle.timeoutDetect();
                    }
                }
            });
            time_to_live.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' middleware server unbound");
                    } catch (Exception e) {
                        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public RMIMiddleware(String name)
    {
        super(name);
        tm = new TransactionManager(name);
    }

    public void connectServer(String tp, String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true)
            {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    if (tp.equals("flight")) {
                        flightRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else if (tp.equals("car")) {
                        carRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else if (tp.equals("room")) {
                        roomRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else {
                        System.err.println((char)27 + "[Please specify four ResourceManagers, or use default values.");
                        System.exit(1);
                    }
                    System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first)
                    {
                        System.out.println("Waiting for '"+name+"' server ["+ server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Reserve an item
    protected boolean reserveItem(int xid, int customerID, String key, String location, IResourceManager rm) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        lockSomething(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);

        Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        int price = rm.modify(xid, key, -1);
        if (price == -1)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        }
        else if (price == 0)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") item doesn't exist");
            return false;
        }
        else
        {
            customer.reserve(key, location, price);
            writeData(xid, customer.getKey(), customer);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    public int newCustomer(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        Trace.info("RM::newCustomer(" + xid + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));

        lockSomething(xid, Customer.getKey(cid), TransactionLockObject.LockType.LOCK_WRITE);

        Customer customer = new Customer(cid);
        writeData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);

        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            customer = new Customer(customerID);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        }
        else
        {
            Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);

        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        }
        else
        {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            // Lock all reservedKey first
            for (String reservedKey: reservations.keySet())
            {
                lockSomething(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
            }

            for (String reservedKey : reservations.keySet())
            {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() +  " times");
                IResourceManager rm = givenKey(reservedKey);
                int price = rm.modify(xid, reserveditem.getKey(), reserveditem.getCount());
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey());
            }

            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    public String queryCustomerInfo(int xid, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ);

        Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return "";
        }
        else
        {
            Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
            System.out.println(customer.getBill());
            return customer.getBill();
        }
    }

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        return flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int xid, String location, int count, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        return carRM.addCars(xid, location, count, price);
    }

    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        return roomRM.addRooms(xid, location, count, price);
    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
        return flightRM.deleteFlight(xid, flightNum);
    }

    public boolean deleteCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        return carRM.deleteCars(xid, location);
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        return roomRM.deleteRooms(xid, location);
    }

    public int queryFlight(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_READ);
        return flightRM.queryFlight(xid, flightNum);
    }

    public int queryCars(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        return carRM.queryCars(xid, location);
    }

    public int queryRooms(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        return roomRM.queryRooms(xid, location);
    }

    public int queryFlightPrice(int xid, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_READ);
        return flightRM.queryFlightPrice(xid, flightNum);
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        return carRM.queryCarsPrice(xid, location);
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
        return roomRM.queryRoomsPrice(xid, location);
    }

    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), flightRM);
    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        return reserveItem(xid, customerID, Car.getKey(location), location, carRM);
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        return reserveItem(xid, customerID, Room.getKey(location), location, roomRM);
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNum, String location, boolean car, boolean room) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        lockSomething(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
        Vector<Integer> flightNum_int = new Vector<>();
        for (String num: flightNum)
        {
            int num_int = Integer.parseInt(num);
            flightNum_int.add(num_int);
            lockSomething(xid, Flight.getKey(num_int), TransactionLockObject.LockType.LOCK_WRITE);
        }
        if (car)
            lockSomething(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        if (room)
            lockSomething(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);

        boolean yn_car = !car || carRM.queryCars(xid, location) > 0;
        boolean yn_room = !room || roomRM.queryRooms(xid, location) > 0;

        boolean yn_flight = true;
        for (Integer num_int: flightNum_int)
        {
            yn_flight = flightRM.queryFlight(xid, num_int) > 0;
            if (!yn_flight)
                break;
        }
        if (yn_car && yn_room && yn_flight)
        {
            boolean ret1 = true;
            if (car)
                ret1 = reserveCar(xid, customerID, location);
            boolean ret2 = true;
            if (room)
                ret2 = reserveRoom(xid, customerID, location);
            boolean ret3 = true;
            for (Integer i: flightNum_int)
            {
                ret3 = reserveFlight(xid, customerID, i);
                if (!ret3)
                    break;
            }
            return ret1 && ret2 && ret3;
        }
        return false;
    }

    public String getName() throws RemoteException
    {
        return m_name;
    }

    private void lockSomething(int xid, String key, TransactionLockObject.LockType lc) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        if (!tm.transactionExist(xid))
        {
            throw new InvalidTransactionException(xid, "transaction not exist.");
        }
        tm.resetTime(xid);
        IResourceManager rm = givenKey(key);
        if (rm != null) {
            tm.addRM(xid, rm);
        }
        try {
            boolean yn = lm.Lock(xid, key, lc);
            if (!yn)
            {
                abort(xid);
                throw new TransactionAbortedException(xid, "lock is not granted.");
            }
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, "deadlock.");
        }
    }

    public void timeoutDetect()
    {
        try {
            for (Integer xid : tm.activeTransaction()) {
                if (!tm.checkTime(xid)) {
                    abort(xid);
                    System.out.println("Transaction [" + xid + "] has been aborted caused by timeout!");
                    break;
                }
            }
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int start()
    {
        return tm.start();
    }

    public boolean commit(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        if (!tm.transactionExist(xid))
        {
            throw new InvalidTransactionException(xid, "transaction not exist.");
        }
        lm.UnlockAll(xid);
        synchronized (local_copy) {
            RMHashMap data = local_copy.get(xid);
            if (data != null) {
                for (String key : data.keySet()) {
                    if (data.get(key) == null) {
                        synchronized (m_data) {
                            m_data.remove(key);
                        }
                    } else {
                        synchronized (m_data) {
                            m_data.put(key, data.get(key));
                        }
                    }
                }
                local_copy.remove(xid);
            }
        }
        return tm.commit(xid);
    }

    public boolean abort(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        if (!tm.transactionExist(xid))
        {
            throw new InvalidTransactionException(xid, "transaction not exist.");
        }
        lm.UnlockAll(xid);
        synchronized (local_copy) {
            local_copy.remove(xid);
        }
        // TODO: Write ABORT record in log
        // writeLog();
        return tm.abort(xid);
    }

    public void shutdown()
    {
        try {
            flightRM.shutdown();
        } catch (RemoteException e) {
            System.out.println("Flight RM shut down successfully!");
        }
        try {
            roomRM.shutdown();
        } catch (RemoteException e) {
            System.out.println("Room RM shut down successfully!");
        }
        try {
            carRM.shutdown();
        } catch (RemoteException e) {
            System.out.println("Car RM shut down successfully!");
        }
        System.out.println("Middleware shut down successfully!");
        System.exit(1);
    }

    public boolean twoPC(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        if (!tm.transactionExist(xid))
        {
            throw new InvalidTransactionException(xid, "transaction not exist.");
        }
        // forward 2-pc to transactionManager
        boolean yn = tm.twoPC(xid);
        // all vote YES, then commit locally
        if (yn)
        {
            lm.UnlockAll(xid);
            synchronized (local_copy) {
                RMHashMap data = local_copy.get(xid);
                if (data != null) {
                    for (String key : data.keySet()) {
                        if (data.get(key) == null) {
                            synchronized (m_data) {
                                m_data.remove(key);
                            }
                        } else {
                            synchronized (m_data) {
                                m_data.put(key, data.get(key));
                            }
                        }
                    }
                    local_copy.remove(xid);
                }
            }
            return true;
        } else {
        // someone votes NO, then abort locally
            lm.UnlockAll(xid);
            synchronized (local_copy) {
                local_copy.remove(xid);
            }
            return false;
        }
    }

    public boolean commitShadowing(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        String committed = "./" + m_name + ".A";
        String working = "./" + m_name + ".B";
        String master = "./" + m_name + ".master";
        if (master_rec.get(0) == 1) {
            String tmp = committed;
            committed = working;
            working = tmp;
        }
        try {
            // ObjectOutputStream committed_file = new ObjectOutputStream(new FileOutputStream(committed));
            ObjectOutputStream working_file = new ObjectOutputStream(new FileOutputStream(working));
            ObjectOutputStream master_file = new ObjectOutputStream(new FileOutputStream(master));
            working_file.writeObject(m_data);
            working_file.writeObject(lm.getTables());
            working_file.writeObject(tm.getRMTable());
            working_file.writeObject(tm.getTimeTable());
            working_file.close();
            master_rec.set(0, 1 - master_rec.get(0));
            master_rec.set(1, xid);
            master_file.writeObject(master_rec);
            master_file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean abortShadowing(int xid) throws RemoteException, TransactionAbortedException, InvalidTransactionException
    {
        String committed = "./" + m_name + ".A";
        String working = "./" + m_name + ".B";
        String master = "./" + m_name + ".master";
        if (master_rec.get(0) == 1) {
            String tmp = committed;
            committed = working;
            working = tmp;
        }
        try {
            ObjectInputStream committed_file = new ObjectInputStream(new FileInputStream(committed));
            m_data = (RMHashMap) committed_file.readObject();
            lm.setTables((Vector<TPHashTable>) committed_file.readObject());
            tm.setRMTable((Hashtable<Integer, Vector<IResourceManager>>) committed_file.readObject());
            tm.setTimeTable((Hashtable<Integer, Long>) committed_file.readObject());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private IResourceManager givenKey(String key)
    {
        IResourceManager rm = null;
        switch (key.substring(0, 3))
        {
            case "fli":
            {
                rm = flightRM;
                break;
            }
            case "car":
            {
                rm = carRM;
                break;
            }
            case "roo":
            {
                rm = roomRM;
                break;
            }
        }
        return rm;
    }
}
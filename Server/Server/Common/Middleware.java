package Server.Common;

import Server.Interface.*;
import Client.Client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.io.*;

public class Middleware implements IResourceManager {

    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group14";
    // protected RMHashMap m_data = new RMHashMap();

    private static String s_flightHost = "localhost";
    private static int s_flightPort = 1099;
    private static String s_flightName = "Flight";

    private static String s_carHost = "localhost";
    private static int s_carPort = 1099;
    private static String s_carName = "Car";

    private static String s_roomHost = "localhost";
    private static int s_roomPort = 1099;
    private static String s_roomName = "Room";

    private static String s_customerHost = "localhost";
    private static int s_customerPort = 1099;
    private static String s_customerName = "Customer";

    private IResourceManager flightRM = null;
    private IResourceManager carRM = null;
    private IResourceManager roomRM = null;
    private IResourceManager customerRM = null;
    private String m_name;

    public static void main(String[] args)
    {
        // Middleware server name
        // Given all 8 arguments or all use default values.
        if (args.length == 9 || args.length == 1 || args.length == 0)
        {
            if (args.length > 0){
                s_serverName = args[0];
            }
            if (args.length == 9)
            {
                s_flightHost = args[1];
                s_flightName = args[2];
                s_carHost = args[3];
                s_carName = args[4];
                s_roomHost = args[5];
                s_roomName = args[6];
                s_customerHost = args[7];
                s_customerName = args[8];
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

        // Crate a new Server entry and get 4 rmiRegistry.
        try {
            Middleware middle = new Middleware("Middleware");
            try {
                middle.connectServer("flight", s_flightHost, s_flightPort, s_flightName);
                middle.connectServer("car", s_carHost, s_carPort, s_carName);
                middle.connectServer("room", s_roomHost, s_roomPort, s_roomName);
                middle.connectServer("customer", s_customerHost, s_customerPort, s_customerName);
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
        // Create and install a security manager
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public Middleware(String p_name)
    {
        s_serverName = p_name;
    }

    public void connectServer(String tp, String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true)
            {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    if (tp == "flight") {
                        flightRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else if (tp == "car") {
                        carRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else if (tp == "room") {
                        roomRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    } else if (tp == "customer") {
                        customerRM = (IResourceManager) registry.lookup(s_rmiPrefix + name);
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

    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException
    {
        return flightRM.addFlight(xid, flightNum, flightSeats, flightPrice);
    }

    public boolean addCars(int xid, String location, int count, int price) throws RemoteException
    {
        return carRM.addCars(xid, location, count, price);
    }

    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException
    {
        return roomRM.addRooms(xid, location, count, price);
    }

    public boolean deleteFlight(int xid, int flightNum) throws RemoteException
    {
        return flightRM.deleteFlight(xid, flightNum);
    }

    public boolean deleteCars(int xid, String location) throws RemoteException
    {
        return carRM.deleteCars(xid, location);
    }

    public boolean deleteRooms(int xid, String location) throws RemoteException
    {
        return roomRM.deleteRooms(xid, location);
    }

    public int queryFlight(int xid, int flightNum) throws RemoteException
    {
        return flightRM.queryFlight(xid, flightNum);
    }

    public int queryCars(int xid, String location) throws RemoteException
    {
        return carRM.queryCars(xid, location);
    }

    public int queryRooms(int xid, String location) throws RemoteException
    {
        return roomRM.queryRooms(xid, location);
    }

    public int queryFlightPrice(int xid, int flightNum) throws RemoteException
    {
        return flightRM.queryFlightPrice(xid, flightNum);
    }

    public int queryCarsPrice(int xid, String location) throws RemoteException
    {
        return carRM.queryCarsPrice(xid, location);
    }

    public int queryRoomsPrice(int xid, String location) throws RemoteException
    {
        return roomRM.queryRoomsPrice(xid, location);
    }

    public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException
    {
        return flightRM.reserveFlight(xid, customerID, flightNum);
    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
    {
        return carRM.reserveCar(xid, customerID, location);
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
    {
        return roomRM.reserveRoom(xid, customerID, location);
    }

    /**
     * Customer-related functions
     */
    public String queryCustomerInfo(int xid, int customerID) throws RemoteException
    {
        return customerRM.queryCustomerInfo(xid, customerID);
    }

    public int newCustomer(int xid) throws RemoteException
    {
        return customerRM.newCustomer(xid);
    }

    public boolean newCustomer(int xid, int customerID) throws RemoteException
    {
        return customerRM.newCustomer(xid, customerID);
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException
    {
        return customerRM.deleteCustomer(xid, customerID);
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNum, String location, boolean car, boolean room) throws RemoteException
    {
        boolean yn_car = !car || carRM.queryCars(xid, location) > 0;
        boolean yn_room = !room || roomRM.queryRooms(xid, location) > 0;

        boolean yn_flight = true;
        Vector<Integer> flightNum_int = new Vector<>();
        for (String num: flightNum)
        {
            int num_int = Client.toInt(num);
            flightNum_int.add(num_int);
            yn_flight = flightRM.queryFlight(xid, num_int) > 0;
            if (!yn_flight)
                break;
        }
        if (yn_car && yn_room && yn_flight)
        {
            boolean ret1 = carRM.reserveCar(xid, customerID, location);
            boolean ret2 = roomRM.reserveRoom(xid, customerID, location);
            boolean ret3 = true;
            for (Integer i: flightNum_int)
            {
                ret3 = flightRM.reserveFlight(xid, customerID, i);
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
}
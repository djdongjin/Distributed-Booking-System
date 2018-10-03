package Server.Common;

import Server.Interface.*;

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
    protected boolean reserveItem(int xid, int customerID, String key, String location, IResourceManager rm) throws RemoteException
    {
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

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException
    {
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
            IResourceManager[] v = {flightRM, carRM, roomRM};
            for (String reservedKey : reservations.keySet())
            {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() +  " times");
                for (IResourceManager rm : v) {
                    int price = rm.modify(xid, reserveditem.getKey(), reserveditem.getCount());
                    if (price <= 0) continue;
                    Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey());
                    break;
                }
            }

            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
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
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), flightRM);
    }

    public boolean reserveCar(int xid, int customerID, String location) throws RemoteException
    {
        return reserveItem(xid, customerID, Car.getKey(location), location, carRM);
    }

    public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException
    {
        return reserveItem(xid, customerID, Room.getKey(location), location, roomRM);
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNum, String location, boolean car, boolean room) throws RemoteException
    {
        boolean yn_car = !car || carRM.queryCars(xid, location) > 0;
        boolean yn_room = !room || roomRM.queryRooms(xid, location) > 0;

        boolean yn_flight = true;
        Vector<Integer> flightNum_int = new Vector<>();
        for (String num: flightNum)
        {
            int num_int = Integer.parseInt(num);
            flightNum_int.add(num_int);
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
}
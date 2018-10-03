package Server.TCP;

import Client.Command;
import Server.Common.*;
import Server.Interface.IResourceManager;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Vector;

public class TCPMiddleware extends ResourceManager {

    private static String s_serverName = "Server";
    private static int s_serverPort = 9999;

    private static String flightHost = "localhost";
    private static int flightPort = 9998;

    private static String carHost = "localhost";
    private static int carPort = 9997;

    private static String roomHost = "localhost";
    private static int roomPort = 9996;

    private int listenPort;

    TCPMiddleware(String name, int port)
    {
        super(name);
        this.listenPort = port;
    }

    public static void main(String[] args)
    {
        if (args.length > 0)
            s_serverName = args[0];
        if (args.length > 1)
            s_serverPort = Integer.parseInt(args[1]);
        if (args.length > 2)
            flightHost = args[2];
        if (args.length > 3)
            flightPort = Integer.parseInt(args[3]);
        if (args.length > 4)
            carHost = args[4];
        if (args.length > 5)
            carPort = Integer.parseInt(args[5]);
        if (args.length > 6)
            roomHost = args[6];
        if (args.length > 7)
            roomPort = Integer.parseInt(args[7]);
        if (args.length > 8) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
                    + "[0mUsage: java client.TCPClient [serverHost, serverPort]");
            System.exit(1);
        }

        TCPMiddleware tcp_middle = new TCPMiddleware(s_serverName, s_serverPort);
        tcp_middle.acceptConnection();
    }

    protected boolean reserveItem(int xid, int customerID, String key, String location, String serverHost, int serverPort, Vector<String> arg)
    {
        Trace.info("TCP::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("TCP::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        int price = Integer.parseInt(forward_server(flightHost, flightPort, arg));
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

    public boolean reserveFlight(int xid, int customerID, int flightNum,  Vector<String> arg) throws RemoteException
    {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), flightHost, flightPort, arg);
    }

    public boolean reserveCar(int xid, int customerID, String location, Vector<String> arg) throws RemoteException
    {
        return reserveItem(xid, customerID, Car.getKey(location), location, carHost, carPort, arg);
    }

    public boolean reserveRoom(int xid, int customerID, String location, Vector<String> arg) throws RemoteException
    {
        return reserveItem(xid, customerID, Room.getKey(location), location, roomHost, roomPort, arg);
    }

    public boolean deleteCustomer(int xid, int customerID) throws RemoteException {
        Vector<String> msg = new Vector<String>();
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer
            // reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
                        + " " + reserveditem.getCount() + " times");

                String ret = "false";
                String reservedItemKey = reserveditem.getKey();
                switch (reservedKey.substring(0, 3)) {
                    case "fli": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedItemKey);
                        msg.add(Integer.toString(reserveditem.getCount()));
                        ret = forward_server(flightHost, flightPort, msg);
                        break;
                    }
                    case "car": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedItemKey);
                        msg.add(Integer.toString(reserveditem.getCount()));
                        ret = forward_server(carHost, carPort, msg);
                        break;
                    }
                    case "roo": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedItemKey);
                        msg.add(Integer.toString(reserveditem.getCount()));
                        ret = forward_server(roomHost, roomPort, msg);
                        break;
                    }
                }
                msg.clear();
                if (ret.equals("false"))
                    return false;
            }

            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    public void acceptConnection()
    {
        try
        {
            ServerSocket server = new ServerSocket(listenPort);
            Socket client_request = null;
            while (true)
            {
                client_request = server.accept();
                (new Thread(new MiddlewareHandler(client_request))).start();
            }
        }
        catch (BindException e) {
            // TODO
        }
        catch (IOException e) {
            // TODO
        }
    }

    public boolean bundle(int xid, int customerID, Vector<String> flightNum, String location, boolean car, boolean room)
            throws RemoteException {

        String traceInfo = "RM::bundle(" + xid + ", customer=" + customerID;
        Vector<String> msg = new Vector<String>();
        Vector<String> flightKey = new Vector<String>();
        String carKey = "", roomKey = "";
        for (String num : flightNum) {
            flightKey.add(Flight.getKey(Integer.parseInt(num)));
            traceInfo += ", " + flightKey;
        }
        if (car) {
            carKey = Car.getKey(location);
            traceInfo += ", " + carKey;
        }
        if (room) {
            roomKey = Room.getKey(location);
            traceInfo += ", " + roomKey;
        }
        traceInfo += ", " + location + ") ";
        Trace.info(traceInfo + " called");

        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn(traceInfo + " failed--customer doesn't exist");
            return false;
        }

        if (car) {
            msg.add("QueryCars");
            msg.add(Integer.toString(xid));
            msg.add(carKey);
            int carNum = Integer.parseInt(send_to_car(msg));
            if (carNum <= 0) {
                return false;
            }
            msg.clear();
        }
        if (room) {
            msg.add("QueryRooms");
            msg.add(Integer.toString(xid));
            msg.add(roomKey);
            int roomNum = Integer.parseInt(send_to_room(msg));
            if (roomNum <= 0) {
                return false;
            }
            msg.clear();
        }
        for (String num : flightNum) {
            msg.add("Queryflight");
            msg.add(Integer.toString(xid));
            msg.add(num);
            int seatNum = Integer.parseInt(send_to_flight(msg));
            if (seatNum <= 0) {
                return false;
            }
            msg.clear();
        }

        if (car) {
            msg.add("ReserveCar");
            msg.add(Integer.toString(customerID));
            msg.add(location);
            String carReserveRes = send_to_car(msg);
            if (carReserveRes.equals("false")) {
                return false;
            }
            msg.clear();
        }

        if (room) {
            msg.add("ReserveRoom");
            msg.add(Integer.toString(customerID));
            msg.add(location);
            String roomReserveRes = send_to_room(msg);
            if (roomReserveRes.equals("false")) {
                return false;
            }
            msg.clear();
        }

        for (String num : flightNum) {
            msg.add("ReserveFlight");
            msg.add(Integer.toString(customerID));
            msg.add(num);
            String flightReserveRes = send_to_flight(msg);
            if (flightReserveRes.equals("false")) {
                return false;
            }
            msg.clear();
        }

        return true;
    }

    public String forward_server(String serverHost, int serverPort, Vector<String> arguments)
    {
        BufferedReader reader = null;
        ObjectOutputStream writer = null;
        String ret = null;
        try {
            Socket client = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            writer = new ObjectOutputStream(client.getOutputStream());
        } catch (UnknownHostException e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[UnknownHost exception");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[IO exception");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            writer.writeObject(arguments);
            writer.flush();
            ret = reader.readLine();
        } catch (IOException e) {
            System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[0mIO exception");
            e.printStackTrace();
        }

        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[IO exception");
            e.printStackTrace();
            System.exit(1);
        }

        return ret;
    }

    public String send_to_flight(Vector<String> arg) { return forward_server(flightHost, flightPort, arg);}

    public String send_to_car(Vector<String> arg) { return forward_server(carHost, carPort, arg);}

    public String send_to_room(Vector<String> arg) { return forward_server(roomHost, roomPort, arg);}

    public String execute(Command cmd, Vector<String> arguments) throws NumberFormatException
    {
        try {
            String ret = "false";
            switch (cmd) {
                case AddFlight:
                case DeleteFlight:
                case QueryFlight:
                case QueryFlightPrice: {

                    ret = forward_server(flightHost, flightPort, arguments);
                    break;
                }
                case AddCars:
                case DeleteCars:
                case QueryCars:
                case QueryCarsPrice: {

                    ret = forward_server(carHost, carPort, arguments);
                    break;
                }
                case AddRooms:
                case DeleteRooms:
                case QueryRooms:
                case QueryRoomsPrice: {

                    ret = forward_server(roomHost, roomPort, arguments);
                    break;
                }
                case AddCustomer: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customer = newCustomer(id);

                    ret = Integer.toString(customer);
                    break;
                }
                case AddCustomerID: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));

                    boolean yn = newCustomer(id, customerID);
                    ret = Boolean.toString(yn);
                    break;
                }
                case DeleteCustomer: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));

                    boolean yn = deleteCustomer(id, customerID);
                    ret = Boolean.toString(yn);
                    break;
                }

                case QueryCustomer: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));

                    ret = queryCustomerInfo(id, customerID);
                    break;
                }

                case ReserveFlight: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));
                    int flightNum = Integer.parseInt(arguments.elementAt(3));

                    ret = Boolean.toString(reserveFlight(id, customerID, flightNum, arguments));
                    System.out.println("Flight Reserved");
                    break;
                }
                case ReserveCar: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);

                    ret = Boolean.toString(reserveCar(id, customerID, location, arguments));
                    System.out.println("Car Reserved");
                    break;
                }
                case ReserveRoom: {

                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);

                    ret = Boolean.toString(reserveRoom(id, customerID, location, arguments));
                    System.out.println("Room Reserved");
                    break;
                }
                case Bundle: {
                    int id = Integer.parseInt(arguments.elementAt(1));
                    int customerID = Integer.parseInt(arguments.elementAt(2));
                    Vector<String> flightNumbers = new Vector<String>();
                    for (int i = 0; i < arguments.size() - 6; ++i) {
                        flightNumbers.addElement(arguments.elementAt(3 + i));
                    }
                    String location = arguments.elementAt(arguments.size() - 3);
                    boolean car = Boolean.valueOf(arguments.elementAt(arguments.size() - 2));
                    boolean room = Boolean.valueOf(arguments.elementAt(arguments.size() - 1));

                    ret = Boolean.toString(bundle(id, customerID, flightNumbers, location, car, room));
                }
            }
            return ret;
        } catch (RemoteException e) {

        }
        return "false";
    }

    public class MiddlewareHandler implements Runnable {

        private Socket client_request = null;

        MiddlewareHandler(Socket sock)
        {
            super();
            this.client_request = sock;
        }

        public void run()
        {
            BufferedWriter writer = null;
            ObjectInputStream reader = null;
            try
            {
                writer = new BufferedWriter(new OutputStreamWriter(client_request.getOutputStream()));
                reader = new ObjectInputStream(client_request.getInputStream());

                Vector<String> arguments = (Vector<String>)reader.readObject();

                Command cmd = Command.fromString((String)arguments.elementAt(0));
                String ret = execute(cmd, arguments);

                // TODO: return value from RM to Middle to client
                writer.write(ret);
                writer.flush();
            }
            catch (IOException e) {
                // TODO
            }
            catch (ClassNotFoundException e) {
                // TODO
            }
        }

    }

}

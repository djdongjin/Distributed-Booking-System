package Server.TCP;

import Client.Command;
import Server.Common.*;

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
            System.err.println((char) 27 + "[31;1mTCPMiddleware exception: " + (char) 27
                    + "[0mUsage: java server.TCP.TCPMiddleware [serverHost, serverPort]");
            System.exit(1);
        }
        try {
            TCPMiddleware tcp_middle = new TCPMiddleware(s_serverName, s_serverPort);
            while (true) {
                tcp_middle.acceptConnection();
                System.out.println("Middleware::Connection interrupted, restart listening...");
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mTCPMiddleware exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
    // TODO check
    public void acceptConnection()
    {
        try
        {
            ServerSocket server = new ServerSocket(listenPort);
            System.out.println("Middleware::Begin to listen at: " + listenPort);
            Socket client_request = null;
            while (true)
            {
                client_request = server.accept();
                System.out.println("Client connected.");
                (new Thread(new MiddlewareHandler(client_request))).start();
            }
        }
        catch (BindException e) {
            System.err.println((char) 27 + "[31;1mTCPMiddleware exception: " + (char) 27 + "[0mUnbind exception");
            e.printStackTrace();
            System.exit(1);
        }
        catch (IOException e) {
            System.err.println((char) 27 + "[31;1mTCPMiddleware exception: " + (char) 27 + "[0mIO exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected boolean reserveItem(int xid, int customerID, String key, String location, String serverHost, int serverPort, Vector<String> arg)
    {
        Trace.info("Middleware::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
        if (customer == null)
        {
            Trace.warn("Middleware::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        int price = Integer.parseInt(forward_server(serverHost, serverPort, arg));
        if (price == -1)
        {
            Trace.warn("Middleware::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return false;
        }
        else if (price == 0)
        {
            Trace.warn("Middleware::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        }
        else
        {
            customer.reserve(key, location, price);
            writeData(xid, customer.getKey(), customer);

            Trace.info("Middleware::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
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
        Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("Middleware::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer
            // reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("Middleware::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
                        + " " + reserveditem.getCount() + " times");

                String ret = "false";
                switch (reservedKey.substring(0, 3)) {
                    case "fli": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedKey);
                        msg.add(Integer.toString(reserveditem.getCount()));
                        ret = forward_server(flightHost, flightPort, msg);
                        break;
                    }
                    case "car": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedKey);
                        msg.add(Integer.toString(reserveditem.getCount()));
                        ret = forward_server(carHost, carPort, msg);
                        break;
                    }
                    case "roo": {
                        msg.add("DeleteReservation");
                        msg.add(Integer.toString(xid));
                        msg.add(Integer.toString(customerID));
                        msg.add(reservedKey);
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

    public boolean bundle(int xid, int customerID, Vector<String> flightNum, String location, boolean car, boolean room)
            throws RemoteException {

        String traceInfo = "RM::bundle(" + xid + ", customer=" + customerID;
        Vector<String> msg = new Vector<String>();

        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn(traceInfo + " failed--customer doesn't exist");
            return false;
        }

        if (car) {
            msg.add("QueryCars");
            msg.add(Integer.toString(xid));
            msg.add(location);
            int carNum = Integer.parseInt(send_to_car(msg));
            if (carNum <= 0) {
                return false;
            }
            msg.clear();
        }
        if (room) {
            msg.add("QueryRooms");
            msg.add(Integer.toString(xid));
            msg.add(location);
            int roomNum = Integer.parseInt(send_to_room(msg));
            if (roomNum <= 0) {
                return false;
            }
            msg.clear();
        }
        for (String num : flightNum) {
            msg.add("QueryFlight");
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
            msg.add(Integer.toString(xid));
            msg.add(Integer.toString(customerID));
            msg.add(location);
            if (reserveCar(xid,customerID,location,msg) == false)
                return false;
            /*String carReserveRes = send_to_car(msg);
            if (carReserveRes.equals("false")) {
                return false;
            }*/
            msg.clear();
        }

        if (room) {
            msg.add("ReserveRoom");
            msg.add(Integer.toString(xid));
            msg.add(Integer.toString(customerID));
            msg.add(location);
            if (reserveRoom(xid,customerID,location,msg) == false)
                return false;
            /*String roomReserveRes = send_to_room(msg);
            if (roomReserveRes.equals("false")) {
                return false;
            }*/
            msg.clear();
        }

        for (String num : flightNum) {
            msg.add("ReserveFlight");
            msg.add(Integer.toString(xid));
            msg.add(Integer.toString(customerID));
            msg.add(num);
            if (reserveFlight(xid,customerID, Integer.parseInt(num),msg) == false)
                return false;
            /*String flightReserveRes = send_to_flight(msg);
            if (flightReserveRes.equals("false")) {
                return false;
            }*/
            msg.clear();
        }
        return true;
    }

    public String analytics(int xid, String item, int threshold, Vector<String> arguments) {
        Trace.info("Middleware::analytics(" + xid + ", " + item + ", " + threshold + ") called");

        String ret = "false";
        switch (item) {
            case "flight": {
                ret = send_to_flight(arguments);
                break;
            }
            case "car": {
                ret = send_to_car(arguments);
                break;
            }
            case "room": {
                ret = send_to_room(arguments);
                break;
            }
            default: {
                Trace.warn("Middleware::analytics(" + xid + ", " + item + ", " + threshold + ") failed--item doesn't exist");
                break;
            }
        }
        return ret;
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
                while (true) {
                    Vector<String> arguments = (Vector<String>) reader.readObject();

                    Command cmd = Command.fromString((String) arguments.elementAt(0));
                    if (cmd == Command.Quit)
                        break;
                    String ret = execute(cmd, arguments);

                    writer.write(ret + "\n");
                    writer.flush();
                    System.out.println("Middleware execute successfully, return value:" + ret);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                // TODO
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println("Middleware execute failed, please retry.");
            }
        }

        public String execute(Command cmd, Vector<String> arguments) throws NumberFormatException
        {
            try {
                String ret = "false";
                switch (cmd) {
                    case AddFlight:
                    case DeleteFlight:
                    case QueryFlight:
                    case QueryFlightPrice: {

                        ret = send_to_flight(arguments);
                        break;
                    }
                    case AddCars:
                    case DeleteCars:
                    case QueryCars:
                    case QueryCarsPrice: {

                        ret = send_to_car(arguments);
                        break;
                    }
                    case AddRooms:
                    case DeleteRooms:
                    case QueryRooms:
                    case QueryRoomsPrice: {

                        ret = send_to_room(arguments);
                        break;
                    }
                    // checked
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
                        String [] info = ret.split("\n");
                        ret = String.join(";", info);
                        break;
                    }

                    case ReserveFlight: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        int flightNum = Integer.parseInt(arguments.elementAt(3));

                        ret = Boolean.toString(reserveFlight(id, customerID, flightNum, arguments));
                        break;
                    }
                    case ReserveCar: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveCar(id, customerID, location, arguments));
                        break;
                    }
                    case ReserveRoom: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveRoom(id, customerID, location, arguments));
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
                        break;
                    }
                    case Analytics: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        String item = arguments.elementAt(2);
                        int threshold = Integer.parseInt(arguments.elementAt(3));

                        ret = analytics(id, item, threshold, arguments);
                        break;
                    }
                }
                return ret;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return "false";
        }

    }

    public class TCPSendMsg {
        public Socket rmClientSocket = null;
        private BufferedReader reader = null;
        private ObjectOutputStream writer = null;
        public String serverHost = "localhost";
        public int serverPort = 9999;

        TCPSendMsg(String address, int port) {
            serverHost = address;
            serverPort = port;
        }

        public String send_msg(Vector<String> arguments) {
            String ret = null;

            connectServer();
            try {
                writer.writeObject(arguments);
                writer.flush();
                while((ret = reader.readLine()) != null)
                {
                    disconnect();
                    return ret;
                }
            } catch (IOException e) {
                System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[0mIO exception");
                e.printStackTrace();
            }
            return null;
        }

        // connect to the RM server
        private void connectServer() {
            try {
                rmClientSocket = new Socket(serverHost, serverPort);
                reader = new BufferedReader(new InputStreamReader(rmClientSocket.getInputStream()));
                writer = new ObjectOutputStream(rmClientSocket.getOutputStream());
            } catch (UnknownHostException e) {
                System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[UnknownHost exception");
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[IO exception");
                e.printStackTrace();
                System.exit(1);
            }
        }

        // disconnect to the RM server
        private void disconnect() {
            try {
                writer.close();
                reader.close();
                rmClientSocket.close();
            } catch (IOException e) {
                System.err.println((char) 27 + "[31;1mIO exception: " + (char) 27 + "[IO exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private String send_to_flight(Vector<String> arguments) {
        TCPSendMsg sendMsg = new TCPSendMsg(flightHost, flightPort);
        return sendMsg.send_msg(arguments);
    }

    private String send_to_car(Vector<String> arguments) {
        TCPSendMsg sendMsg = new TCPSendMsg(carHost, carPort);
        return sendMsg.send_msg(arguments);
    }

    private String send_to_room(Vector<String> arguments) {
        TCPSendMsg sendMsg = new TCPSendMsg(roomHost, roomPort);
        return sendMsg.send_msg(arguments);
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
            while ( (ret = reader.readLine()) != null )
                break;
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

}

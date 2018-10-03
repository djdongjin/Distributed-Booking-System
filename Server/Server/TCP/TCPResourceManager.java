package Server.TCP;

import Server.Common.*;
import Server.Interface.*;
import Client.Command;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Vector;

public class TCPResourceManager extends ResourceManager {

    private static String s_serverName = "Server";
    private static int s_serverPort = 9999;

    private int listenPort;

    TCPResourceManager(String name, int port)
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
        {
            System.err.println((char) 27 + "[31;1mTCPResourceManager exception: " + (char) 27
                    + "[0mUsage: java server.TCP.TCPResourceManager [serverHost, serverPort]");
            System.exit(1);
        }
        try {
            TCPResourceManager tcp_ResourceManager = new TCPResourceManager(s_serverName, s_serverPort);
            while (true)
                tcp_ResourceManager.acceptConnection();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mTCPMiddleware exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
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
                (new Thread(new RequestHandler(client_request))).start();
            }
        }
        catch (BindException e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.err.println((char)27 + "[31;1mIO exception: " + (char)27 + "[0mUncaught IOStream");
            e.printStackTrace();
        }
    }

    // Delete the reservation when deleting a customer
    public boolean deleteReservation(int xid, int customerID, String reservedKey, int reservedCount)
            throws RemoteException {
        ReservableItem item = (ReservableItem) readData(xid, reservedKey);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reservedKey
                + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount()
                + " times");
        item.setReserved(item.getReserved() - reservedCount);
        item.setCount(item.getCount() + reservedCount);
        writeData(xid, item.getKey(), item);

        return true;
    }


    public class RequestHandler implements Runnable {

        private Socket client_request = null;

        RequestHandler(Socket sock)
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
                System.err.println((char)27 + "[31;1mIO exception: " + (char)27 + "[0mUncaught IOStream");
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                System.err.println((char)27 + "[31;1mClassNotFound exception: " + (char)27 + "[0mUncaught ClassNotFound exception");
                e.printStackTrace();
            }
        }

        public String execute(Command cmd, Vector<String> arguments) throws NumberFormatException
        {
            try {
                String ret = "false";
                switch (cmd) {
                    case AddFlight: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        int flightNum = Integer.parseInt(arguments.elementAt(2));
                        int flightSeats = Integer.parseInt(arguments.elementAt(3));
                        int flightPrice = Integer.parseInt(arguments.elementAt(4));

                        boolean yn = addFlight(id, flightNum, flightSeats, flightPrice);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case AddCars: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);
                        int numCars = Integer.parseInt(arguments.elementAt(3));
                        int price = Integer.parseInt(arguments.elementAt(4));

                        boolean yn = addCars(id, location, numCars, price);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case AddRooms: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);
                        int numRooms = Integer.parseInt(arguments.elementAt(3));
                        int price = Integer.parseInt(arguments.elementAt(4));

                        if (addRooms(id, location, numRooms, price)) {
                            System.out.println("Rooms added");
                        } else {
                            System.out.println("Rooms could not be added");
                        }
                        break;
                    }
                    case DeleteFlight: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        int flightNum = Integer.parseInt(arguments.elementAt(2));

                        boolean yn = deleteFlight(id, flightNum);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteCars: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        boolean yn = deleteCars(id, location);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteRooms: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        boolean yn = deleteRooms(id, location);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case QueryFlight: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int flightNum = Integer.parseInt(arguments.elementAt(2));

                        int seats = queryFlight(id, flightNum);
                        ret = Integer.toString(seats);
                        break;
                    }
                    case QueryCars: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int numCars = queryCars(id, location);
                        ret = Integer.toString(numCars);
                        break;
                    }
                    case QueryRooms: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int numRoom = queryRooms(id, location);
                        ret = Integer.toString(numRoom);
                        break;
                    }
                    case QueryFlightPrice: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int flightNum = Integer.parseInt(arguments.elementAt(2));

                        int price = queryFlightPrice(id, flightNum);
                        ret = Integer.toString(price);
                        break;
                    }
                    case QueryCarsPrice: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int price = queryCarsPrice(id, location);
                        ret = Integer.toString(price);
                        break;
                    }
                    case QueryRoomsPrice: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int price = queryRoomsPrice(id, location);
                        ret = Integer.toString(price);
                        break;
                    }
                    case ReserveFlight: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        int flightNum = Integer.parseInt(arguments.elementAt(3));

                        ret = Boolean.toString(reserveFlight(id, customerID, flightNum));
                        System.out.println("Flight Reserved");
                        break;
                    }
                    case ReserveCar: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveCar(id, customerID, location));
                        break;
                    }
                    case ReserveRoom: {

                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveRoom(id, customerID, location));
                        System.out.println("Room Reserved");
                        break;
                    }
                    case DeleteReservation: {
                        int id = Integer.parseInt(arguments.elementAt(1));
                        int customerID = Integer.parseInt(arguments.elementAt(2));
                        String reservedKey = arguments.elementAt(3);
                        int reservedCount = Integer.parseInt(arguments.elementAt(4));

                        boolean yn = deleteReservation(id, customerID, reservedKey, reservedCount);
                        ret = Boolean.toString(yn);
                        break;
                    }
                }
                return ret;
            } catch (RemoteException e) {

            }
            return "false";
        }
    }

}

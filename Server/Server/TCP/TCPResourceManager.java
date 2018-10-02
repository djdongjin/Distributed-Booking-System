package Server.TCP;

import Server.Common.*;
import Server.Interface.*;
import Client.Client;
import Client.Command;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.Vector;

import static Client.Client.toInt;

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

        TCPResourceManager tcp_ResourceManager = new TCPResourceManager(s_serverName, s_serverPort);
        tcp_ResourceManager.acceptConnection();
    }

    public void acceptConnection()
    {
        try
        {
            ServerSocket server = new ServerSocket(listenPort, 10);
            Socket client_request = null;
            while (true)
            {
                client_request = server.accept();
                (new Thread(new RequestHandler(client_request))).start();
            }
        }
        catch (BindException e) {
            // TODO
        }
        catch (IOException e) {
            // TODO
        }
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
            try
            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client_request.getOutputStream()));
                ObjectInputStream reader = new ObjectInputStream(client_request.getInputStream());

                Vector<String> arguments = (Vector<String>)reader.readObject();

                Command cmd = Command.fromString((String)arguments.elementAt(0));
                String ret = execute(cmd, arguments);

                // TODO: return value from RM to Middle to client
                //
            }
            catch (IOException e) {
                // TODO
            }
            catch (ClassNotFoundException e) {
                // TODO
            }
        }

        public String execute(Command cmd, Vector<String> arguments) throws NumberFormatException
        {
            try {
                String ret = "false";
                switch (cmd) {
                    case AddFlight: {
                        int id = toInt(arguments.elementAt(1));
                        int flightNum = toInt(arguments.elementAt(2));
                        int flightSeats = toInt(arguments.elementAt(3));
                        int flightPrice = toInt(arguments.elementAt(4));

                        boolean yn = addFlight(id, flightNum, flightSeats, flightPrice);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case AddCars: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);
                        int numCars = toInt(arguments.elementAt(3));
                        int price = toInt(arguments.elementAt(4));

                        boolean yn = addCars(id, location, numCars, price);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case AddRooms: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);
                        int numRooms = toInt(arguments.elementAt(3));
                        int price = toInt(arguments.elementAt(4));

                        if (addRooms(id, location, numRooms, price)) {
                            System.out.println("Rooms added");
                        } else {
                            System.out.println("Rooms could not be added");
                        }
                        break;
                    }
                    case AddCustomer: {

                        int id = toInt(arguments.elementAt(1));
                        int customer = newCustomer(id);

                        ret = Integer.toString(customer);
                        break;
                    }
                    case AddCustomerID: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));

                        boolean yn = newCustomer(id, customerID);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteFlight: {

                        int id = toInt(arguments.elementAt(1));
                        int flightNum = toInt(arguments.elementAt(2));

                        boolean yn = deleteFlight(id, flightNum);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteCars: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        boolean yn = deleteCars(id, location);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteRooms: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        boolean yn = deleteRooms(id, location);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case DeleteCustomer: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));

                        boolean yn = deleteCustomer(id, customerID);
                        ret = Boolean.toString(yn);
                        break;
                    }
                    case QueryFlight: {

                        int id = toInt(arguments.elementAt(1));
                        int flightNum = toInt(arguments.elementAt(2));

                        int seats = queryFlight(id, flightNum);
                        ret = Integer.toString(seats);
                        break;
                    }
                    case QueryCars: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int numCars = queryCars(id, location);
                        ret = Integer.toString(numCars);
                        break;
                    }
                    case QueryRooms: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int numRoom = queryRooms(id, location);
                        ret = Integer.toString(numRoom);
                        break;
                    }
                    case QueryCustomer: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));

                        ret = queryCustomerInfo(id, customerID);
                        break;
                    }
                    case QueryFlightPrice: {

                        int id = toInt(arguments.elementAt(1));
                        int flightNum = toInt(arguments.elementAt(2));

                        int price = queryFlightPrice(id, flightNum);
                        ret = Integer.toString(price);
                        break;
                    }
                    case QueryCarsPrice: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int price = queryCarsPrice(id, location);
                        ret = Integer.toString(price);
                        break;
                    }
                    case QueryRoomsPrice: {

                        int id = toInt(arguments.elementAt(1));
                        String location = arguments.elementAt(2);

                        int price = queryRoomsPrice(id, location);
                        ret = Integer.toString(price);
                        break;
                    }
                    case ReserveFlight: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));
                        int flightNum = toInt(arguments.elementAt(3));

                        ret = Boolean.toString(reserveFlight(id, customerID, flightNum));
                        System.out.println("Flight Reserved");
                        break;
                    }
                    case ReserveCar: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveCar(id, customerID, location));
                        break;
                    }
                    case ReserveRoom: {

                        int id = toInt(arguments.elementAt(1));
                        int customerID = toInt(arguments.elementAt(2));
                        String location = arguments.elementAt(3);

                        ret = Boolean.toString(reserveRoom(id, customerID, location));
                        System.out.println("Room Reserved");
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

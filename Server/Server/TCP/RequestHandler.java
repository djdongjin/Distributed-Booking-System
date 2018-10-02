package Server.TCP;

import Client.Command;
import Server.Interface.IResourceManager;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Vector;

import static Client.Client.toInt;

public class RequestHandler implements Runnable {

    private Socket client_request = null;
    private IResourceManager rm;

    RequestHandler(IResourceManager rm, Socket sock)
    {
        super();
        this.client_request = sock;
        this.rm = rm;
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

                    boolean yn = rm.addFlight(id, flightNum, flightSeats, flightPrice);
                    ret = Boolean.toString(yn);
                    break;
                }
                case AddCars: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    int numCars = toInt(arguments.elementAt(3));
                    int price = toInt(arguments.elementAt(4));

                    boolean yn = rm.addCars(id, location, numCars, price);
                    ret = Boolean.toString(yn);
                    break;
                }
                case AddRooms: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);
                    int numRooms = toInt(arguments.elementAt(3));
                    int price = toInt(arguments.elementAt(4));

                    if (rm.addRooms(id, location, numRooms, price)) {
                        System.out.println("Rooms added");
                    } else {
                        System.out.println("Rooms could not be added");
                    }
                    break;
                }
                case AddCustomer: {

                    int id = toInt(arguments.elementAt(1));
                    int customer = rm.newCustomer(id);

                    ret = Integer.toString(customer);
                    break;
                }
                case AddCustomerID: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));

                    boolean yn = rm.newCustomer(id, customerID);
                    ret = Boolean.toString(yn);
                    break;
                }
                case DeleteFlight: {

                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));

                    boolean yn = rm.deleteFlight(id, flightNum);
                    ret = Boolean.toString(yn);
                    break;
                }
                case DeleteCars: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    boolean yn = rm.deleteCars(id, location);
                    ret = Boolean.toString(yn);
                    break;
                }
                case DeleteRooms: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    boolean yn = rm.deleteRooms(id, location);
                    ret = Boolean.toString(yn);
                    break;
                }
                case DeleteCustomer: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));

                    boolean yn = rm.deleteCustomer(id, customerID);
                    ret = Boolean.toString(yn);
                    break;
                }
                case QueryFlight: {

                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));

                    int seats = rm.queryFlight(id, flightNum);
                    ret = Integer.toString(seats);
                    break;
                }
                case QueryCars: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    int numCars = rm.queryCars(id, location);
                    ret = Integer.toString(numCars);
                    break;
                }
                case QueryRooms: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    int numRoom = rm.queryRooms(id, location);
                    ret = Integer.toString(numRoom);
                    break;
                }
                case QueryCustomer: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));

                    ret = rm.queryCustomerInfo(id, customerID);
                    break;
                }
                case QueryFlightPrice: {

                    int id = toInt(arguments.elementAt(1));
                    int flightNum = toInt(arguments.elementAt(2));

                    int price = rm.queryFlightPrice(id, flightNum);
                    ret = Integer.toString(price);
                    break;
                }
                case QueryCarsPrice: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    int price = rm.queryCarsPrice(id, location);
                    ret = Integer.toString(price);
                    break;
                }
                case QueryRoomsPrice: {

                    int id = toInt(arguments.elementAt(1));
                    String location = arguments.elementAt(2);

                    int price = rm.queryRoomsPrice(id, location);
                    ret = Integer.toString(price);
                    break;
                }
                case ReserveFlight: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    int flightNum = toInt(arguments.elementAt(3));

                    ret = Boolean.toString(rm.reserveFlight(id, customerID, flightNum));
                    System.out.println("Flight Reserved");
                    break;
                }
                case ReserveCar: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);

                    ret = Boolean.toString(rm.reserveCar(id, customerID, location));
                    break;
                }
                case ReserveRoom: {

                    int id = toInt(arguments.elementAt(1));
                    int customerID = toInt(arguments.elementAt(2));
                    String location = arguments.elementAt(3);

                    ret = Boolean.toString(rm.reserveRoom(id, customerID, location));
                    System.out.println("Room Reserved");
                    break;
                }
            }
            return ret;
        }
        catch (RemoteException e) {
            // TODO
            // How handle RemoteException, a RMI exception, from IResourceManager.
        }
        return "false";
    }
}
